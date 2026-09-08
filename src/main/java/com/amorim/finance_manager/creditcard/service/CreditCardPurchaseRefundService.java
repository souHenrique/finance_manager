package com.amorim.finance_manager.creditcard.service;

import com.amorim.finance_manager.creditcard.dto.CreditCardRefundRequest;
import com.amorim.finance_manager.creditcard.dto.CreditCardRefundResponse;
import com.amorim.finance_manager.creditcard.entity.*;
import com.amorim.finance_manager.creditcard.repository.CreditCardCreditRepository;
import com.amorim.finance_manager.creditcard.repository.CreditCardRefundItemRepository;
import com.amorim.finance_manager.creditcard.repository.CreditCardRefundRepository;
import com.amorim.finance_manager.creditcard.repository.CreditCardRepository;
import com.amorim.finance_manager.invoice.entity.Invoice;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.shared.exception.*;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.transaction.mapper.TransactionMapper;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.user.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CreditCardPurchaseRefundService {

    private final CreditCardRepository creditCardRepository;
    private final TransactionRepository transactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final CurrentUserService currentUserService;

    private final CreditCardRefundRepository refundRepository;
    private final CreditCardRefundItemRepository refundItemRepository;
    private final CreditCardCreditRepository creditRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public CreditCardRefundResponse refundPurchase(
            UUID creditCardId,
            UUID transactionId,
            CreditCardRefundRequest request
    ) {
        if (request == null
                || request.reason() == null
                || request.reason().isBlank()
                || request.reason().length() > 500) {

            throw new InvalidCreditCardRefundException("Informe um motivo de estorno com até 500 caracteres");
        }

        UUID userId = currentUserService.getCurrentUserId();

        CreditCard card = creditCardRepository
                .findByIdAndUserId(creditCardId, userId)
                .orElseThrow(CreditCardNotFoundException::new);

        entityManager.lock(card, LockModeType.OPTIMISTIC);

        Transaction selectedTransaction = transactionRepository
                .findByIdAndUserId(transactionId, userId)
                .orElseThrow(TransactionNotFoundException::new);

        validateOwnership(selectedTransaction, userId, creditCardId);

        List<Transaction> installments =
                loadInstallments(selectedTransaction, userId);

        validateInstallments(
                selectedTransaction,
                installments,
                userId,
                creditCardId
        );

        List<UUID> transactionIds = installments.stream()
                .map(Transaction::getId)
                .toList();

        if (refundItemRepository
                .existsByOriginalTransactionIdIn(transactionIds)) {
            throw new CreditCardPurchaseAlreadyRefundedException();
        }

        for (Transaction installment : installments) {
            entityManager.lock(installment, LockModeType.OPTIMISTIC);
        }

        Map<UUID, BigDecimal> amountsByInvoice =
                calculateAmountsByInvoice(installments);

        Map<UUID, Invoice> invoices = loadAndValidateInvoices(
                amountsByInvoice,
                creditCardId,
                userId
        );

        BigDecimal unpaidAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;

        for (Map.Entry<UUID, BigDecimal> entry
                : amountsByInvoice.entrySet()) {

            Invoice invoice = invoices.get(entry.getKey());

            if (invoice.getStatus() == InvoiceStatus.PAID) {
                paidAmount = paidAmount.add(entry.getValue());
            } else {
                unpaidAmount = unpaidAmount.add(entry.getValue());
            }
        }

        BigDecimal newAvailableLimit = null;

        if (unpaidAmount.signum() > 0) {
            newAvailableLimit = calculateAvailableLimit(
                    card,
                    unpaidAmount
            );
        }

        CreditCardRefund refund = new CreditCardRefund();

        refund.setUserId(userId);
        refund.setCreditCardId(creditCardId);
        refund.setSelectedTransactionId(selectedTransaction.getId());
        refund.setInstallmentGroupId(
                selectedTransaction.getInstallmentGroupId()
        );
        refund.setReason(request.reason().strip());
        refund.setTotalAmount(unpaidAmount.add(paidAmount));
        refund.setLimitRestoredAmount(unpaidAmount);
        refund.setPaidCompensationAmount(paidAmount);

        refundRepository.saveAndFlush(refund);

        List<CreditCardRefundResponse.Item> responseItems =
                new ArrayList<>();

        List<Transaction> cancelledInstallments = new ArrayList<>();

        for (Transaction installment : installments) {
            Invoice invoice = invoices.get(installment.getInvoiceId());

            boolean paid = invoice.getStatus() == InvoiceStatus.PAID;

            CreditCardRefundItem item = new CreditCardRefundItem();

            item.setRefundId(refund.getId());
            item.setOriginalTransactionId(installment.getId());
            item.setOriginalInvoiceId(invoice.getId());
            item.setOriginalInvoiceStatus(invoice.getStatus());
            item.setAmount(installment.getAmount());
            item.setTreatment(
                    paid
                            ? CreditCardRefundTreatment.FUTURE_INVOICE_CREDIT
                            : CreditCardRefundTreatment.UNPAID_CANCELLATION
            );

            refundItemRepository.saveAndFlush(item);

            UUID creditId = null;

            if (paid) {
                CreditCardCredit credit = createCredit(refund, item);
                creditId = credit.getId();
            } else {
                cancelledInstallments.add(installment);
            }

            responseItems.add(
                    new CreditCardRefundResponse.Item(
                            item.getId(),
                            item.getOriginalTransactionId(),
                            item.getOriginalInvoiceId(),
                            item.getOriginalInvoiceStatus(),
                            item.getAmount(),
                            item.getTreatment(),
                            creditId
                    )
            );
        }

        if (unpaidAmount.signum() > 0) {
            card.setAvailableLimit(newAvailableLimit);
            creditCardRepository.saveAndFlush(card);

            List<Invoice> changedInvoices = new ArrayList<>();

            for (Map.Entry<UUID, BigDecimal> entry
                    : amountsByInvoice.entrySet()) {

                Invoice invoice = invoices.get(entry.getKey());

                if (invoice.getStatus() == InvoiceStatus.PAID) {
                    continue;
                }

                invoice.setTotalAmount(
                        invoice.getTotalAmount().subtract(entry.getValue())
                );

                changedInvoices.add(invoice);
            }

            invoiceRepository.saveAllAndFlush(changedInvoices);

            for (Transaction installment : cancelledInstallments) {
                installment.setStatus(TransactionStatus.CANCELLED);
            }

            transactionRepository.saveAllAndFlush(cancelledInstallments);
        }

        return new CreditCardRefundResponse(
                refund.getId(),
                refund.getCreditCardId(),
                refund.getSelectedTransactionId(),
                refund.getInstallmentGroupId(),
                refund.getReason(),
                refund.getTotalAmount(),
                refund.getLimitRestoredAmount(),
                refund.getPaidCompensationAmount(),
                refund.getCreatedAt(),
                List.copyOf(responseItems)
        );
    }

    private List<Transaction> loadInstallments(Transaction selectedTransaction, UUID userId) {
        UUID groupId = selectedTransaction.getInstallmentGroupId();

        if (groupId == null) {
            return List.of(selectedTransaction);
        }

        return transactionRepository
                .findAllByInstallmentGroupIdAndCreditCardIdAndUserIdOrderByInstallmentNumberAsc(
                        groupId,
                        selectedTransaction.getCreditCardId(),
                        userId
                );
    }

    private void validateOwnership(Transaction transaction, UUID userId, UUID creditCardId) {
        if (!Objects.equals(transaction.getUserId(), userId)
                || !Objects.equals(
                transaction.getCreditCardId(),
                creditCardId
        )) {
            throw new TransactionNotFoundException();
        }
    }

    private void validateInstallments(
            Transaction selectedTransaction,
            List<Transaction> installments,
            UUID userId,
            UUID creditCardId
    ) {
        if (installments.isEmpty()) {
            throw new InvalidCreditCardRefundException("Nenhuma parcela foi encontrada para a compra");
        }

        boolean containsSelectedTransaction = installments.stream()
                .anyMatch(installment -> Objects.equals(
                        installment.getId(),
                        selectedTransaction.getId()
                ));

        if (!containsSelectedTransaction) {
            throw new InvalidCreditCardRefundException("A transação não foi encontrada no grupo da compra");
        }

        UUID groupId = selectedTransaction.getInstallmentGroupId();

        if (groupId != null
                && !Objects.equals(
                selectedTransaction.getInstallmentCount(),
                installments.size()
        )) {
            throw new InvalidCreditCardRefundException("O grupo de parcelas está incompleto ou inconsistente");
        }

        for (int index = 0; index < installments.size(); index++) {
            Transaction installment = installments.get(index);

            validateOwnership(installment, userId, creditCardId);
            validatePurchase(installment);

            if (!Objects.equals(installment.getInstallmentGroupId(), groupId)) {
                throw new InvalidCreditCardRefundException("As parcelas não pertencem ao mesmo grupo");
            }

            if (groupId != null) {
                validateInstallmentNumber(
                        installment,
                        index + 1,
                        installments.size()
                );
            } else {
                validateSinglePurchaseMetadata(installment);
            }
        }
    }

    private void validatePurchase(Transaction transaction) {
        if (transaction.getType()
                != TransactionType.CREDIT_CARD_PURCHASE
                || transaction.getPaymentMethod()
                != PaymentMethod.CREDIT_CARD) {
            throw new InvalidCreditCardRefundException("A transação não representa uma compra no cartão");
        }

        if (transaction.getStatus() == TransactionStatus.CANCELLED) {
            throw new TransactionAlreadyCancelledException();
        }

        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new InvalidCreditCardRefundException("A compra precisa estar concluída para ser estornada");
        }

        if (transaction.getAmount() == null
                || transaction.getAmount().signum() <= 0) {
            throw new InvalidCreditCardRefundException("A compra possui valor inválido");
        }

        if (transaction.getInvoiceId() == null) {
            throw new InvalidCreditCardRefundException("A compra não possui uma fatura vinculada");
        }

        if (transaction.getEffectiveDate() != null
                || transaction.getSourceAccountId() != null
                || transaction.getDestinationAccountId() != null) {
            throw new InvalidCreditCardRefundException("A compra possui dados de movimentação de conta incompatíveis");
        }
    }

    private void validateInstallmentNumber(Transaction installment, int expectedNumber, int expectedCount) {
        if (!Objects.equals(
                installment.getInstallmentNumber(),
                expectedNumber
        ) || !Objects.equals(
                installment.getInstallmentCount(),
                expectedCount
        )) {
            throw new InvalidCreditCardRefundException("A numeração das parcelas está inconsistente");
        }
    }

    private void validateSinglePurchaseMetadata(Transaction transaction) {
        Integer count = transaction.getInstallmentCount();
        Integer number = transaction.getInstallmentNumber();

        if ((count != null && count != 1)
                || (number != null && number != 1)) {
            throw new InvalidCreditCardRefundException("Uma compra parcelada precisa possuir um grupo");
        }
    }

    private Map<UUID, BigDecimal> calculateAmountsByInvoice(List<Transaction> installments) {
        Map<UUID, BigDecimal> amounts = new LinkedHashMap<>();

        for (Transaction installment : installments) {
            amounts.merge(
                    installment.getInvoiceId(),
                    installment.getAmount(),
                    BigDecimal::add
            );
        }

        return amounts;
    }

    private Map<UUID, Invoice> loadAndValidateInvoices(
            Map<UUID, BigDecimal> amountsByInvoice,
            UUID creditCardId,
            UUID userId
    ) {
        Map<UUID, Invoice> invoices = new LinkedHashMap<>();

        for (Map.Entry<UUID, BigDecimal> entry
                : amountsByInvoice.entrySet()) {

            Invoice invoice = invoiceRepository
                    .findOwnedById(entry.getKey(), userId)
                    .orElseThrow(InvoiceNotFoundException::new);

            if (!Objects.equals(
                    invoice.getCreditCardId(),
                    creditCardId
            )) {
                throw new InvoiceNotFoundException();
            }

            entityManager.lock(invoice, LockModeType.OPTIMISTIC);

            InvoiceStatus status = invoice.getStatus();

            if (status != InvoiceStatus.OPEN
                    && status != InvoiceStatus.CLOSED
                    && status != InvoiceStatus.PAID) {

                throw new InvalidCreditCardRefundException("O estado da fatura não permite este estorno");
            }

            if (status == InvoiceStatus.PAID) {
                if (invoice.getPaidAt() == null) {
                    throw new InvalidCreditCardRefundException("A fatura paga não possui data de pagamento");
                }
            } else if (invoice.getPaidAt() != null) {
                throw new InvalidCreditCardRefundException("A fatura não paga possui data de pagamento");
            }

            if (invoice.getTotalAmount() == null
                    || invoice.getTotalAmount()
                    .compareTo(entry.getValue()) < 0) {

                throw new InvalidCreditCardRefundException("O valor da compra é incompatível com o total da fatura");
            }

            invoices.put(invoice.getId(), invoice);
        }

        return invoices;
    }

    private BigDecimal calculateAvailableLimit(CreditCard card, BigDecimal refundedAmount) {
        if (card.getAvailableLimit() == null
                || card.getCreditLimit() == null
                || card.getAvailableLimit().signum() < 0
                || card.getAvailableLimit()
                .compareTo(card.getCreditLimit()) > 0) {
            throw new InvalidCreditCardRefundException("O cartão possui limite inconsistente");
        }

        BigDecimal newAvailableLimit = card.getAvailableLimit()
                .add(refundedAmount);

        if (newAvailableLimit.compareTo(card.getCreditLimit()) > 0) {
            throw new InvalidCreditCardRefundException("O estorno ultrapassaria o limite total do cartão");
        }

        return newAvailableLimit;
    }

    private CreditCardCredit createCredit(CreditCardRefund refund, CreditCardRefundItem item) {
        CreditCardCredit credit = new CreditCardCredit();

        credit.setUserId(refund.getUserId());
        credit.setCreditCardId(refund.getCreditCardId());
        credit.setRefundItemId(item.getId());
        credit.setOriginalAmount(item.getAmount());
        credit.setRemainingAmount(item.getAmount());

        return creditRepository.saveAndFlush(credit);
    }
}
