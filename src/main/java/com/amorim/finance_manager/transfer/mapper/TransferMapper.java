package com.amorim.finance_manager.transfer.mapper;

import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transfer.dto.CreateTransferRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransferMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)

    @Mapping(target = "competenceDate", source = "date")
    @Mapping(target = "effectiveDate", source = "date")

    @Mapping(target = "type", constant = "TRANSFER")
    @Mapping(target = "status", constant = "COMPLETED")
    @Mapping(target = "paymentMethod", constant = "TRANSFER")

    @Mapping(target = "dueDate", ignore = true)
    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "creditCardId", ignore = true)
    @Mapping(target = "invoiceId", ignore = true)
    @Mapping(target = "installmentGroupId", ignore = true)
    @Mapping(target = "installmentNumber", ignore = true)
    @Mapping(target = "installmentCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Transaction toEntity(CreateTransferRequest request);
}
