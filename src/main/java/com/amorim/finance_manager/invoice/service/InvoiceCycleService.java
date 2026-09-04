package com.amorim.finance_manager.invoice.service;

import com.amorim.finance_manager.creditcard.entity.CreditCard;
import com.amorim.finance_manager.creditcard.repository.CreditCardRepository;
import com.amorim.finance_manager.invoice.entity.Invoice;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.shared.exception.CreditCardNotFoundException;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor

public class InvoiceCycleService {

    private final InvoiceRepository invoiceRepository;
    private final CreditCardRepository creditCardRepository;
    private final CurrentUserService currentUserService;

    public InvoiceCycle calculate(
            LocalDate purchaseDate,
            int closingDay,
            int dueDay
    ) {
        Objects.requireNonNull(
                purchaseDate,
                "Data da compra é obrigatória"
        );

        validateDay(closingDay);
        validateDay(dueDay);

        YearMonth purchaseMonth = YearMonth.from(purchaseDate);

        LocalDate closingDateInPurchaseMonth =
                validDate(purchaseMonth, closingDay);

        YearMonth referenceMonth =
                purchaseDate.isAfter(closingDateInPurchaseMonth)
                        ? purchaseMonth.plusMonths(1)
                        : purchaseMonth;

        LocalDate closingDate =
                validDate(referenceMonth, closingDay);

        LocalDate dueDate =
                validDate(referenceMonth, dueDay);

        if (!dueDate.isAfter(closingDate)) {
            dueDate = validDate(
                    referenceMonth.plusMonths(1),
                    dueDay
            );
        }

        return new InvoiceCycle(
                referenceMonth.getMonthValue(),
                referenceMonth.getYear(),
                closingDate,
                dueDate
        );
    }

    @Transactional
    public Invoice findOrCreate(UUID creditCardId, LocalDate purchaseDate) {
        UUID userId = currentUserService.getCurrentUserId();

        CreditCard card = creditCardRepository
                .findByIdAndUserId(creditCardId, userId)
                .orElseThrow(CreditCardNotFoundException::new);

        InvoiceCycle cycle = calculate(
                purchaseDate,
                card.getClosingDay(),
                card.getDueDay()
        );

        return invoiceRepository
                .findByCreditCardIdAndReferenceMonthAndReferenceYear(
                        creditCardId,
                        cycle.referenceMonth(),
                        cycle.referenceYear()
                )
                .orElseGet(() -> {
                    Invoice invoice = new Invoice();

                    invoice.setCreditCardId(creditCardId);
                    invoice.setReferenceMonth(cycle.referenceMonth());
                    invoice.setReferenceYear(cycle.referenceYear());
                    invoice.setClosingDate(cycle.closingDate());
                    invoice.setDueDate(cycle.dueDate());
                    invoice.setTotalAmount(
                            BigDecimal.ZERO.setScale(2)
                    );
                    invoice.setStatus(InvoiceStatus.OPEN);
                    invoice.setPaidAt(null);

                    return invoiceRepository.saveAndFlush(invoice);
                });
    }

    @Transactional
    public Invoice findOrCreate(CreditCard card, InvoiceCycle cycle) {
        return invoiceRepository
                .findByCreditCardIdAndReferenceMonthAndReferenceYear(
                        card.getId(),
                        cycle.referenceMonth(),
                        cycle.referenceYear()
                )
                .orElseGet(() -> {
                    Invoice invoice = new Invoice();

                    invoice.setCreditCardId(card.getId());
                    invoice.setReferenceMonth(cycle.referenceMonth());
                    invoice.setReferenceYear(cycle.referenceYear());
                    invoice.setClosingDate(cycle.closingDate());
                    invoice.setDueDate(cycle.dueDate());
                    invoice.setTotalAmount(BigDecimal.ZERO.setScale(2));
                    invoice.setStatus(InvoiceStatus.OPEN);
                    invoice.setPaidAt(null);

                    return invoiceRepository.saveAndFlush(invoice);
                });
    }

    private LocalDate validDate(YearMonth month, int configuredDay) {
        int validDay = Math.min(
                configuredDay,
                month.lengthOfMonth()
        );

        return month.atDay(validDay);
    }

    private void validateDay(int day) {
        if (day < 1 || day > 31) {
            throw new IllegalArgumentException(
                    "Dia deve estar entre 1 e 31"
            );
        }
    }
}
