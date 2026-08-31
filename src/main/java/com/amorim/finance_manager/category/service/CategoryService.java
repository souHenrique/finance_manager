package com.amorim.finance_manager.category.service;

import com.amorim.finance_manager.category.dto.CategoryResponse;
import com.amorim.finance_manager.category.dto.CreateCategoryRequest;
import com.amorim.finance_manager.category.dto.UpdateCategoryRequest;
import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.mapper.CategoryMapper;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.shared.exception.CategoryNotFoundException;
import com.amorim.finance_manager.shared.exception.IncompatibleCategoryTypeException;
import com.amorim.finance_manager.shared.exception.InvalidCategoryUpdateException;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final CurrentUserService currentUserService;

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        UUID userId = currentUserService.getCurrentUserId();

        validateParent(
                request.parentCategoryId(),
                request.type(),
                userId
        );

        Category category = categoryMapper.toEntity(request);
        category.setUserId(userId);
        category.setStatus(CategoryStatus.ACTIVE);

        Category saved =  categoryRepository.saveAndFlush(category);

        return categoryMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        UUID userId = currentUserService.getCurrentUserId();

        return categoryRepository
                .findAllByUserIdOrderByNameAsc(userId)
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID categoryId) {
        UUID userId = currentUserService.getCurrentUserId();

        return categoryMapper.toResponse(findOwnedCategory(categoryId, userId));
    }

    @Transactional
    public CategoryResponse update(UUID categoryId, UpdateCategoryRequest request) {
        validadeUpdate(request);

        UUID userId = currentUserService.getCurrentUserId();
        Category category = findOwnedCategory(categoryId, userId);

        categoryMapper.updateEntity(request, category);

        return categoryMapper.toResponse(categoryRepository.saveAndFlush(category));
    }

    private Category findOwnedCategory(UUID categoryId, UUID userId) {
        return categoryRepository
                .findByIdAndUserId(categoryId, userId)
                .orElseThrow(CategoryNotFoundException::new);
    }

    private void validateParent(UUID parentCategoryId, CategoryType childType, UUID userId) {
        if (parentCategoryId == null) {
            return;
        }

        Category parent = categoryRepository
                .findByIdAndUserId(parentCategoryId, userId)
                .orElseThrow(CategoryNotFoundException::new);

        if (parent.getType() != childType) {
            throw new IncompatibleCategoryTypeException();
        }
    }

    private void validadeUpdate(UpdateCategoryRequest request) {
        if (request.name() == null && request.status() == null) {
            throw new InvalidCategoryUpdateException("Informe ao menos um campo para atualização");
        }

        if (request.name() != null && request.name().isBlank()) {
            throw new InvalidCategoryUpdateException("Nome não pode ser vazio");
        }
    }
}
