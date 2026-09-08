package com.amorim.finance_manager.budget.repository;

import com.amorim.finance_manager.budget.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    List<Budget> findAllByUserIdOrderByYearDescMonthDescCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndCategoryIdAndMonthAndYear(UUID userId, UUID categoryId, Integer month, Integer year);

    boolean existsByUserIdAndCategoryIdAndMonthAndYearAndIdNot(
            UUID userId,
            UUID categoryId,
            Integer month,
            Integer year,
            UUID id
    );
}
