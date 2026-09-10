package com.amorim.finance_manager.export.service;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.transaction.dto.TransactionFilterRequest;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transaction.service.TransactionService;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionCsvExportService {

    private static final String[] HEADER = {
            "id",
            "description",
            "type",
            "status",
            "amount",
            "competenceDate",
            "effectiveDate",
            "category",
            "account"
    };

    private final TransactionService transactionService;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public byte[] export(TransactionFilterRequest filters) {
        UUID userId = currentUserService.getCurrentUserId();

        List<TransactionResponse> transactions = transactionService.listForExport(filters);

        Map<UUID, String> categories = loadCategories(userId, transactions);

        Map<UUID, String> accounts = loadAccounts(userId, transactions);

        StringBuilder csv = new StringBuilder();
        appendRow(csv, HEADER);

        for (TransactionResponse transaction : transactions) {
            appendRow(
                    csv,
                    transaction.id().toString(),
                    transaction.description(),
                    transaction.type().name(),
                    transaction.status().name(),
                    transaction.amount().toPlainString(),
                    formatDate(transaction.competenceDate()),
                    formatDate(transaction.effectiveDate()),
                    nameOf(transaction.categoryId(), categories),
                    resolveAccount(transaction, accounts)
            );
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Map<UUID, String> loadCategories(UUID userId, List<TransactionResponse> transactions) {
        Set<UUID> ids = transactions.stream()
                .map(TransactionResponse::categoryId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return Map.of();
        }

        return categoryRepository
                .findAllByUserIdAndIdIn(userId, ids)
                .stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        Category::getName
                ));
    }

    private Map<UUID, String> loadAccounts(UUID userId, List<TransactionResponse> transactions) {
        Set<UUID> ids = new LinkedHashSet<>();

        for (TransactionResponse transaction : transactions) {
            if (transaction.sourceAccountId() != null) {
                ids.add(transaction.sourceAccountId());
            }

            if (transaction.destinationAccountId() != null) {
                ids.add(transaction.destinationAccountId());
            }
        }

        if (ids.isEmpty()) {
            return Map.of();
        }

        return accountRepository
                .findAllByUserIdAndIdIn(userId, ids)
                .stream()
                .collect(Collectors.toMap(
                        Account::getId,
                        Account::getName
                ));
    }

    private String resolveAccount(TransactionResponse transaction, Map<UUID, String> accounts) {
        String source = nameOf(transaction.sourceAccountId(), accounts);

        String destination = nameOf(transaction.destinationAccountId(), accounts);

        if (!source.isBlank() && !destination.isBlank()) {
            return source + " -> " + destination;
        }

        return !source.isBlank() ? source : destination;
    }

    private String nameOf(UUID id, Map<UUID, String> names) {
        return id == null ? "" : names.getOrDefault(id, "");
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private void appendRow(StringBuilder csv, String... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                csv.append(',');
            }
            csv.append(escape(values[index]));
        }
        csv.append("\r\n");
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean requiresQuotes =
                value.indexOf(',') >= 0
                        || value.indexOf('"') >= 0
                        || value.indexOf('\r') >= 0
                        || value.indexOf('\n') >= 0;

        String escaped = value.replace("\"", "\"\"");

        return requiresQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
