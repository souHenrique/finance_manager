package com.amorim.finance_manager.category.mapper;

import com.amorim.finance_manager.category.dto.CategoryResponse;
import com.amorim.finance_manager.category.dto.CreateCategoryRequest;
import com.amorim.finance_manager.category.dto.UpdateCategoryRequest;
import com.amorim.finance_manager.category.entity.Category;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CreateCategoryRequest request);

    CategoryResponse toResponse(Category category);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "parentCategoryId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateCategoryRequest request, @MappingTarget Category category);
}
