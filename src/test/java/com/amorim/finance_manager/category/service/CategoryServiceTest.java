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
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID INCOME_CATEGORY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID EXPENSE_CATEGORY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID PARENT_CATEGORY_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final UUID SUBCATEGORY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldCreateIncomeCategory() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Salário",
                CategoryType.INCOME,
                null
        );

        Category category = createCategory(
                INCOME_CATEGORY_ID,
                "Salário",
                CategoryType.INCOME,
                null,
                null
        );

        CategoryResponse response = createResponse(
                INCOME_CATEGORY_ID,
                "Salário",
                CategoryType.INCOME,
                null,
                CategoryStatus.ACTIVE
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(categoryMapper.toEntity(request))
                .thenReturn(category);

        when(categoryRepository.saveAndFlush(category))
                .thenReturn(category);

        when(categoryMapper.toResponse(category))
                .thenReturn(response);

        CategoryResponse result = categoryService.create(request);

        assertThat(category.getUserId())
                .isEqualTo(USER_ID);

        assertThat(category.getType())
                .isEqualTo(CategoryType.INCOME);

        assertThat(category.getStatus())
                .isEqualTo(CategoryStatus.ACTIVE);

        assertThat(category.getParentCategoryId())
                .isNull();

        assertThat(result)
                .isEqualTo(response);

        verify(categoryRepository)
                .saveAndFlush(category);

        verify(categoryRepository, never())
                .findByIdAndUserId(any(UUID.class), any(UUID.class));
    }

    @Test
    void shouldCreateExpenseCategory() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Moradia",
                CategoryType.EXPENSE,
                null
        );

        Category category = createCategory(
                EXPENSE_CATEGORY_ID,
                "Moradia",
                CategoryType.EXPENSE,
                null,
                null
        );

        CategoryResponse response = createResponse(
                EXPENSE_CATEGORY_ID,
                "Moradia",
                CategoryType.EXPENSE,
                null,
                CategoryStatus.ACTIVE
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(categoryMapper.toEntity(request))
                .thenReturn(category);

        when(categoryRepository.saveAndFlush(category))
                .thenReturn(category);

        when(categoryMapper.toResponse(category))
                .thenReturn(response);

        CategoryResponse result = categoryService.create(request);

        assertThat(category.getUserId())
                .isEqualTo(USER_ID);

        assertThat(category.getType())
                .isEqualTo(CategoryType.EXPENSE);

        assertThat(category.getStatus())
                .isEqualTo(CategoryStatus.ACTIVE);

        assertThat(category.getParentCategoryId())
                .isNull();

        assertThat(result)
                .isEqualTo(response);

        verify(categoryRepository)
                .saveAndFlush(category);
    }

    @Test
    void shouldCreateSubcategoryWithCompatibleParent() {
        Category parent = createCategory(
                PARENT_CATEGORY_ID,
                "Moradia",
                CategoryType.EXPENSE,
                null,
                CategoryStatus.ACTIVE
        );
        parent.setUserId(USER_ID);

        CreateCategoryRequest request = new CreateCategoryRequest(
                "Aluguel",
                CategoryType.EXPENSE,
                PARENT_CATEGORY_ID
        );

        Category subcategory = createCategory(
                SUBCATEGORY_ID,
                "Aluguel",
                CategoryType.EXPENSE,
                PARENT_CATEGORY_ID,
                null
        );

        CategoryResponse response = createResponse(
                SUBCATEGORY_ID,
                "Aluguel",
                CategoryType.EXPENSE,
                PARENT_CATEGORY_ID,
                CategoryStatus.ACTIVE
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(categoryRepository.findByIdAndUserId(
                PARENT_CATEGORY_ID,
                USER_ID
        )).thenReturn(Optional.of(parent));

        when(categoryMapper.toEntity(request))
                .thenReturn(subcategory);

        when(categoryRepository.saveAndFlush(subcategory))
                .thenReturn(subcategory);

        when(categoryMapper.toResponse(subcategory))
                .thenReturn(response);

        CategoryResponse result = categoryService.create(request);

        assertThat(subcategory.getUserId())
                .isEqualTo(USER_ID);

        assertThat(subcategory.getParentCategoryId())
                .isEqualTo(PARENT_CATEGORY_ID);

        assertThat(subcategory.getType())
                .isEqualTo(parent.getType());

        assertThat(subcategory.getStatus())
                .isEqualTo(CategoryStatus.ACTIVE);

        assertThat(result)
                .isEqualTo(response);

        verify(categoryRepository)
                .findByIdAndUserId(PARENT_CATEGORY_ID, USER_ID);

        verify(categoryRepository)
                .saveAndFlush(subcategory);

        verify(categoryRepository, never())
                .findById(any(UUID.class));
    }

    @Test
    void shouldRejectSubcategoryWhenParentDoesNotExist() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Aluguel",
                CategoryType.EXPENSE,
                PARENT_CATEGORY_ID
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(categoryRepository.findByIdAndUserId(
                PARENT_CATEGORY_ID,
                USER_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Categoria não encontrada");

        verify(categoryRepository)
                .findByIdAndUserId(PARENT_CATEGORY_ID, USER_ID);

        verifyNoInteractions(categoryMapper);

        verify(categoryRepository, never())
                .saveAndFlush(any(Category.class));
    }

    @Test
    void shouldRejectSubcategoryOwnedByAnotherUser() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Aluguel",
                CategoryType.EXPENSE,
                PARENT_CATEGORY_ID
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        /*
         * A consulta escopada ao USER_ID não encontra uma categoria
         * pertencente a outro usuário.
         */
        when(categoryRepository.findByIdAndUserId(
                PARENT_CATEGORY_ID,
                USER_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Categoria não encontrada");

        verify(categoryRepository)
                .findByIdAndUserId(PARENT_CATEGORY_ID, USER_ID);

        verify(categoryRepository, never())
                .findById(any(UUID.class));

        verify(categoryRepository, never())
                .saveAndFlush(any(Category.class));

        verifyNoInteractions(categoryMapper);
    }

    @Test
    void shouldRejectSubcategoryWithIncompatibleType() {
        Category incomeParent = createCategory(
                PARENT_CATEGORY_ID,
                "Receitas",
                CategoryType.INCOME,
                null,
                CategoryStatus.ACTIVE
        );
        incomeParent.setUserId(USER_ID);

        CreateCategoryRequest request = new CreateCategoryRequest(
                "Aluguel",
                CategoryType.EXPENSE,
                PARENT_CATEGORY_ID
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(categoryRepository.findByIdAndUserId(
                PARENT_CATEGORY_ID,
                USER_ID
        )).thenReturn(Optional.of(incomeParent));

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(IncompatibleCategoryTypeException.class)
                .hasMessage("Tipo de categoria incompatível");

        verify(categoryRepository)
                .findByIdAndUserId(PARENT_CATEGORY_ID, USER_ID);

        verify(categoryRepository, never())
                .saveAndFlush(any(Category.class));

        verifyNoInteractions(categoryMapper);
    }

    @Test
    void shouldInactivateCategoryWithoutDeleting() {
        Category category = createCategory(
                EXPENSE_CATEGORY_ID,
                "Moradia",
                CategoryType.EXPENSE,
                null,
                CategoryStatus.ACTIVE
        );
        category.setUserId(USER_ID);

        UpdateCategoryRequest request = new UpdateCategoryRequest(
                null,
                CategoryStatus.INACTIVE
        );

        CategoryResponse response = createResponse(
                EXPENSE_CATEGORY_ID,
                "Moradia",
                CategoryType.EXPENSE,
                null,
                CategoryStatus.INACTIVE
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(categoryRepository.findByIdAndUserId(
                EXPENSE_CATEGORY_ID,
                USER_ID
        )).thenReturn(Optional.of(category));

        doAnswer(invocation -> {
            category.setStatus(request.status());
            return null;
        }).when(categoryMapper).updateEntity(request, category);

        when(categoryRepository.saveAndFlush(category))
                .thenReturn(category);

        when(categoryMapper.toResponse(category))
                .thenReturn(response);

        CategoryResponse result = categoryService.update(
                EXPENSE_CATEGORY_ID,
                request
        );

        assertThat(category.getStatus())
                .isEqualTo(CategoryStatus.INACTIVE);

        assertThat(result)
                .isEqualTo(response);

        verify(categoryRepository)
                .findByIdAndUserId(EXPENSE_CATEGORY_ID, USER_ID);

        verify(categoryMapper)
                .updateEntity(request, category);

        verify(categoryRepository)
                .saveAndFlush(category);

        verify(categoryRepository, never())
                .delete(any(Category.class));

        verify(categoryRepository, never())
                .deleteById(any(UUID.class));
    }

    private Category createCategory(
            UUID id,
            String name,
            CategoryType type,
            UUID parentCategoryId,
            CategoryStatus status
    ) {
        Category category = new Category();

        category.setId(id);
        category.setName(name);
        category.setType(type);
        category.setParentCategoryId(parentCategoryId);
        category.setStatus(status);

        return category;
    }

    private CategoryResponse createResponse(
            UUID id,
            String name,
            CategoryType type,
            UUID parentCategoryId,
            CategoryStatus status
    ) {
        return new CategoryResponse(
                id,
                name,
                type,
                parentCategoryId,
                status,
                null,
                null
        );
    }
}
