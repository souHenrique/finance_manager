package com.amorim.finance_manager.creditcard.service;

import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.creditcard.dto.CreateCreditCardRequest;
import com.amorim.finance_manager.creditcard.dto.CreditCardResponse;
import com.amorim.finance_manager.creditcard.dto.UpdateCreditCardRequest;
import com.amorim.finance_manager.creditcard.entity.CreditCard;
import com.amorim.finance_manager.creditcard.entity.CreditCardStatus;
import com.amorim.finance_manager.creditcard.mapper.CreditCardMapper;
import com.amorim.finance_manager.creditcard.repository.CreditCardRepository;
import com.amorim.finance_manager.shared.exception.AccountNotFoundException;
import com.amorim.finance_manager.shared.exception.CreditCardNotFoundException;
import com.amorim.finance_manager.shared.exception.CreditLimitConflictException;
import com.amorim.finance_manager.shared.exception.InvalidCreditCardUpdateException;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditCardService {

    private final AccountRepository accountRepository;
    private final CreditCardRepository creditCardRepository;
    private final CreditCardMapper creditCardMapper;
    private final CurrentUserService currentUserService;

    @Transactional
    public CreditCardResponse create(CreateCreditCardRequest request) {
        UUID userId = currentUserService.getCurrentUserId();

        validateOwnedDefaultAccount(
                request.defaultAccountId(),
                userId
        );

        CreditCard card = creditCardMapper.toEntity(request);

        card.setUserId(userId);
        card.setAvailableLimit(request.creditLimit());
        card.setStatus(CreditCardStatus.ACTIVE);

        CreditCard saved = creditCardRepository.saveAndFlush(card);

        log.info(
                "event=credit_card.created creditCardId={} userId={}",
                saved.getId(),
                saved.getUserId()
        );

        return creditCardMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CreditCardResponse> findAll() {
        UUID userId = currentUserService.getCurrentUserId();

        return creditCardRepository
                .findAllByUserIdOrderByNameAsc(userId)
                .stream()
                .map(creditCardMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CreditCardResponse findById(UUID creditCardId) {
        UUID userId = currentUserService.getCurrentUserId();

        CreditCard card = findOwnedCreditCard(creditCardId, userId);

        return creditCardMapper.toResponse(card);
    }

    @Transactional
    public CreditCardResponse update(UUID creditCardId, UpdateCreditCardRequest request) {
        validateUpdate(request);

        UUID userId = currentUserService.getCurrentUserId();

        CreditCard card = findOwnedCreditCard(creditCardId, userId);

        if (request.defaultAccountId() != null) {
            validateOwnedDefaultAccount(request.defaultAccountId(), userId);
        }

        CreditCardStatus previousStatus = card.getStatus();

        updateCreditLimit(card, request.creditLimit());

        creditCardMapper.updateEntity(request, card);

        CreditCard updated = creditCardRepository.saveAndFlush(card);

        log.info(
                """
                event=credit_card.updated \
                creditCardId={} userId={} \
                previousStatus={} currentStatus={}
                """,
                updated.getId(),
                updated.getUserId(),
                previousStatus,
                updated.getStatus()
        );

        return creditCardMapper.toResponse(updated);
    }

    private void validateOwnedDefaultAccount(UUID accountId, UUID userId) {
        accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);
    }

    private CreditCard findOwnedCreditCard(UUID creditCardId, UUID userId) {
        return creditCardRepository
                .findByIdAndUserId(creditCardId, userId)
                .orElseThrow(CreditCardNotFoundException::new);
    }

    private void validateUpdate(UpdateCreditCardRequest request) {
        boolean noFieldWasProvided =
                request.name() == null
                        && request.creditLimit() == null
                        && request.closingDay() == null
                        && request.dueDay() == null
                        && request.defaultAccountId() == null
                        && request.status() == null;

        if (noFieldWasProvided) {
            throw new InvalidCreditCardUpdateException("Informe ao menos um campo para atualização");
        }

        if (request.name() != null && request.name().isBlank()) {
            throw new InvalidCreditCardUpdateException("Nome do cartão não pode ser vazio");
        }
    }

    private void updateCreditLimit(CreditCard card, BigDecimal newCreditLimit) {
        if (newCreditLimit == null) {
            return;
        }

        BigDecimal committedLimit = card.getCreditLimit().subtract(card.getAvailableLimit());

        if (newCreditLimit.compareTo(committedLimit) < 0) {
            throw new CreditLimitConflictException();
        }

        card.setCreditLimit(newCreditLimit);
        card.setAvailableLimit(newCreditLimit.subtract(committedLimit));
    }
}
