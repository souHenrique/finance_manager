package com.amorim.finance_manager.transaction.specification;

import com.amorim.finance_manager.transaction.dto.TransactionFilterRequest;
import com.amorim.finance_manager.transaction.entity.Transaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> withFilters(
            UUID userId,
            TransactionFilterRequest filters
    ) {
        Objects.requireNonNull(userId, "Usuário autenticado é obrigatório");
        Objects.requireNonNull(filters, "Filtros são obrigatórios");

        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(
                    builder.equal(root.get("userId"), userId)
            );

            if (filters.startDate() != null) {
                predicates.add(
                        builder.greaterThanOrEqualTo(
                                root.<LocalDate>get("competenceDate"),
                                filters.startDate()
                        )
                );
            }

            if (filters.endDate() != null) {
                predicates.add(
                        builder.lessThanOrEqualTo(
                                root.<LocalDate>get("competenceDate"),
                                filters.endDate()
                        )
                );
            }

            if (filters.categoryId() != null) {
                predicates.add(
                        builder.equal(
                                root.get("categoryId"),
                                filters.categoryId()
                        )
                );
            }

            if (filters.accountId() != null) {
                predicates.add(
                        builder.or(
                                builder.equal(
                                        root.get("sourceAccountId"),
                                        filters.accountId()
                                ),
                                builder.equal(
                                        root.get("destinationAccountId"),
                                        filters.accountId()
                                )
                        )
                );
            }

            if (filters.creditCardId() != null) {
                predicates.add(
                        builder.equal(
                                root.get("creditCardId"),
                                filters.creditCardId()
                        )
                );
            }

            if (filters.type() != null) {
                predicates.add(
                        builder.equal(root.get("type"), filters.type())
                );
            }

            if (filters.status() != null) {
                predicates.add(
                        builder.equal(root.get("status"), filters.status())
                );
            }

            if (filters.minAmount() != null) {
                predicates.add(
                        builder.greaterThanOrEqualTo(
                                root.<BigDecimal>get("amount"),
                                filters.minAmount()
                        )
                );
            }

            if (filters.maxAmount() != null) {
                predicates.add(
                        builder.lessThanOrEqualTo(
                                root.<BigDecimal>get("amount"),
                                filters.maxAmount()
                        )
                );
            }

            if (filters.description() != null
                    && !filters.description().isBlank()) {

                String text = filters.description()
                        .strip()
                        .toLowerCase(Locale.ROOT);

                predicates.add(
                        builder.like(
                                builder.lower(root.<String>get("description")),
                                "%" + escapeLike(text) + "%",
                                '!'
                        )
                );
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
