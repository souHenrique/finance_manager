package com.amorim.finance_manager.transaction.mapper;

import com.amorim.finance_manager.transaction.dto.CreateTransactionRequest;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transaction.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Transaction toEntity(CreateTransactionRequest request);

    TransactionResponse toResponse(Transaction transaction);
}
