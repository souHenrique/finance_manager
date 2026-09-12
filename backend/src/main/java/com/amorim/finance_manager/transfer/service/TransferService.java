package com.amorim.finance_manager.transfer.service;

import com.amorim.finance_manager.account.service.AccountBalanceService;
import com.amorim.finance_manager.shared.exception.InvalidTransferException;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.mapper.TransactionMapper;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.transfer.dto.CreateTransferRequest;
import com.amorim.finance_manager.transfer.mapper.TransferMapper;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final AccountBalanceService accountBalanceService;
    private final TransactionRepository transactionRepository;
    private final TransferMapper transferMapper;
    private final TransactionMapper transactionMapper;
    private final CurrentUserService currentUserService;

    @Transactional
    public TransactionResponse transfer(CreateTransferRequest request) {
        validateRequest(request);

        UUID userId = currentUserService.getCurrentUserId();

        accountBalanceService.debit(userId, request.sourceAccountId(), request.amount());

        accountBalanceService.credit(userId, request.destinationAccountId(), request.amount());

        Transaction transaction = transferMapper.toEntity(request);

        transaction.setUserId(userId);

        Transaction saved = transactionRepository.saveAndFlush(transaction);

        log.info(
                "event=transfer.completed transactionId={} userId={}",
                saved.getId(),
                saved.getUserId()
        );

        return transactionMapper.toResponse(saved);
    }

    private void validateRequest(CreateTransferRequest request) {
        if (Objects.equals(request.sourceAccountId(), request.destinationAccountId())) {
            throw new InvalidTransferException("As contas de origem e destino devem ser diferentes");
        }

        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new InvalidTransferException("O valor da transferência deve ser maior que zero");
        }
    }
}
