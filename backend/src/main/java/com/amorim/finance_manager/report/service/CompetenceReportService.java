package com.amorim.finance_manager.report.service;

import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.report.dto.CompetenceReportResponse;
import com.amorim.finance_manager.report.projection.CompetenceAggregate;
import com.amorim.finance_manager.report.repository.CompetenceReportRepository;
import com.amorim.finance_manager.shared.exception.InvalidReportPeriodException;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class CompetenceReportService {

    private static final Set<TransactionType> INCLUDED_TYPES = Set.of(
            TransactionType.INCOME,
            TransactionType.EXPENSE,
            TransactionType.CREDIT_CARD_PURCHASE
    );

    private final CompetenceReportRepository reportRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;
    private final CompetenceReportCalculator calculator;

    public CompetenceReportResponse generate(LocalDate startDate, LocalDate endDate) {
        validatePeriod(startDate, endDate);

        UUID userId = currentUserService.getCurrentUserId();

        List<CompetenceAggregate> rows = reportRepository.aggregate(
                userId,
                startDate,
                endDate,
                TransactionStatus.COMPLETED,
                INCLUDED_TYPES
        );

        Map<UUID, String> categoryNames = loadCategoryNames(userId, rows);

        return calculator.calculate(
                startDate,
                endDate,
                rows,
                categoryNames
        );
    }

    private Map<UUID, String> loadCategoryNames(UUID userId, List<CompetenceAggregate> rows) {
        Set<UUID> categoryIds = rows.stream()
                .map(CompetenceAggregate::categoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (categoryIds.isEmpty()) {
            return Map.of();
        }

        return categoryRepository
                .findAllByUserIdAndIdIn(userId, categoryIds)
                .stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        Category::getName
                ));
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidReportPeriodException("As datas inicial e final são obrigatórias");
        }

        if (startDate.isAfter(endDate)) {
            throw new InvalidReportPeriodException("A data inicial não pode ser posterior à data final");
        }
    }
}
