package com.amorim.finance_manager.report.service;

import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.report.dto.*;
import com.amorim.finance_manager.report.projection.CashFlowAggregate;
import com.amorim.finance_manager.report.repository.CashFlowReportRepository;
import com.amorim.finance_manager.shared.exception.InvalidReportPeriodException;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class CashFlowReportService {

    private static final LocalDate MIN_DATE = LocalDate.of(1, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private static final List<TransactionType> CASH_TYPES = List.of(
            TransactionType.INCOME,
            TransactionType.EXPENSE,
            TransactionType.CREDIT_CARD_PAYMENT
    );

    private final CashFlowReportRepository reportRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;
    private final CashFlowCalculator calculator;

    public DailyCashFlowResponse daily(LocalDate date) {
        validatePeriod(date, date);

        UUID userId = currentUserService.getCurrentUserId();

        List<CashFlowAggregate> rows = aggregate(userId, date, date);
        Map<UUID, String> names = loadCategoryNames(userId, rows);

        return new DailyCashFlowResponse(
                date,
                calculator.summarize(rows, names)
        );
    }

    public WeeklyCashFlowResponse weekly(LocalDate date) {
        validatePeriod(date, date);

        LocalDate start = date.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
        LocalDate end = start.plusDays(6);

        LocalDate previousStart = start.minusWeeks(1);
        LocalDate previousEnd = start.minusDays(1);

        validatePeriod(previousStart, end);

        UUID userId = currentUserService.getCurrentUserId();

        List<CashFlowAggregate> rows = aggregate(
                userId,
                previousStart,
                end
        );

        Map<UUID, String> names = loadCategoryNames(userId, rows);

        List<CashFlowAggregate> currentRows = rows.stream()
                .filter(row -> !row.effectiveDate().isBefore(start))
                .toList();

        List<CashFlowAggregate> previousRows = rows.stream()
                .filter(row -> row.effectiveDate().isBefore(start))
                .toList();

        CashFlowSummaryResponse current =
                calculator.summarize(currentRows, names);

        CashFlowSummaryResponse previous =
                calculator.summarize(previousRows, names);

        return new WeeklyCashFlowResponse(
                new CashFlowPeriodResponse(start, end, current),
                new CashFlowPeriodResponse(
                        previousStart,
                        previousEnd,
                        previous
                ),
                calculator.compare(current, previous)
        );
    }

    private List<CashFlowAggregate> aggregate(
            UUID userId,
            LocalDate start,
            LocalDate end
    ) {
        return reportRepository.aggregate(
                userId,
                start,
                end,
                TransactionStatus.COMPLETED,
                CASH_TYPES
        );
    }

    private Map<UUID, String> loadCategoryNames(
            UUID userId,
            List<CashFlowAggregate> rows
    ) {
        Set<UUID> categoryIds = rows.stream()
                .filter(row ->
                        row.type() != TransactionType.CREDIT_CARD_PAYMENT
                )
                .map(CashFlowAggregate::categoryId)
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

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start == null
                || end == null
                || start.isBefore(MIN_DATE)
                || end.isAfter(MAX_DATE)
                || start.isAfter(end)) {

            throw new InvalidReportPeriodException(
                    "O período deve estar entre 0001-01-01 e 9999-12-31"
            );
        }
    }


}
