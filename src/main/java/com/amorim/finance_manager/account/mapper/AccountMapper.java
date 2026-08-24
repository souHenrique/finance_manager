package com.amorim.finance_manager.account.mapper;

import com.amorim.finance_manager.account.dto.AccountResponse;
import com.amorim.finance_manager.account.dto.CreateAccountRequest;
import com.amorim.finance_manager.account.dto.UpdateAccountRequest;
import com.amorim.finance_manager.account.entity.Account;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Account toEntity(CreateAccountRequest request);

    AccountResponse toResponse(Account account);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "initialBalance", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateAccountRequest request, @MappingTarget Account account);
}
