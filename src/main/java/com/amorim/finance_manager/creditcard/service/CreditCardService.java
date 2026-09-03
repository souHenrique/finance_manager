package com.amorim.finance_manager.creditcard.service;

import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.creditcard.dto.CreateCreditCardRequest;
import com.amorim.finance_manager.creditcard.entity.CreditCard;
import com.amorim.finance_manager.creditcard.entity.CreditCardStatus;
import com.amorim.finance_manager.creditcard.mapper.CreditCardMapper;
import com.amorim.finance_manager.creditcard.repository.CreditCardRepository;
import com.amorim.finance_manager.shared.exception.AccountNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class CreditCardService {

    private final AccountRepository accountRepository;
    private final CreditCardRepository creditCardRepository;
    private final CreditCardMapper creditCardMapper;

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

        return creditCardMapper.toResponse(saved);
    }

    private void validateOwnedDefaultAccount(UUID accountId, UUID userId) {
        accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);
    }
}
