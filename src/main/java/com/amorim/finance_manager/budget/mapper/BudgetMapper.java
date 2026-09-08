package com.amorim.finance_manager.budget.mapper;

import com.amorim.finance_manager.budget.dto.BudgetResponse;
import com.amorim.finance_manager.budget.dto.CreateBudgetRequest;
import com.amorim.finance_manager.budget.dto.UpdateBudgetRequest;
import com.amorim.finance_manager.budget.entity.Budget;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BudgetMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Budget toEntity(CreateBudgetRequest request);

    BudgetResponse toResponse(Budget budget);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateBudgetRequest request, @MappingTarget Budget budget);
}
