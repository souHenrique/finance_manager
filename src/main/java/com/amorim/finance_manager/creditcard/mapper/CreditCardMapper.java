package com.amorim.finance_manager.creditcard.mapper;

import com.amorim.finance_manager.creditcard.dto.CreateCreditCardRequest;
import com.amorim.finance_manager.creditcard.dto.UpdateCreditCardRequest;
import com.amorim.finance_manager.creditcard.entity.CreditCard;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CreditCardMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "availableLimit", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "version", ignore = true)
    CreditCard toEntity(CreateCreditCardRequest request);

    CreditCardResponse toResponse(CreditCard creditCard);

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "creditLimit", ignore = true)
    @Mapping(target = "availableLimit", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(
            UpdateCreditCardRequest request,
            @MappingTarget CreditCard creditCard
    );
}
