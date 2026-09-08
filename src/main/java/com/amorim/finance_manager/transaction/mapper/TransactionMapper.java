package com.amorim.finance_manager.transaction.mapper;

import com.amorim.finance_manager.transaction.dto.CreateTransactionRequest;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transaction.dto.UpdateTransactionRequest;
import com.amorim.finance_manager.transaction.entity.Transaction;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Transaction toEntity(CreateTransactionRequest request);

    TransactionResponse toResponse(Transaction transaction);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "dueDate", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "creditCardId", ignore = true)
    @Mapping(target = "invoiceId", ignore = true)
    @Mapping(target = "installmentGroupId", ignore = true)
    @Mapping(target = "installmentNumber", ignore = true)
    @Mapping(target = "installmentCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(
            UpdateTransactionRequest request,
            @MappingTarget Transaction transaction
    );
}
