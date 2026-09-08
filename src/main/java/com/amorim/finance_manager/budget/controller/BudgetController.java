package com.amorim.finance_manager.budget.controller;

import com.amorim.finance_manager.budget.dto.BudgetResponse;
import com.amorim.finance_manager.budget.dto.CreateBudgetRequest;
import com.amorim.finance_manager.budget.dto.UpdateBudgetRequest;
import com.amorim.finance_manager.budget.service.BudgetService;
import com.amorim.finance_manager.config.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/budgets")
@Tag(name = "Orçamentos" , description = "Gerenciamento de orçamentos mensais")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class BudgetController implements BudgetDocsApi {

    private final BudgetService budgetService;

    @Override
    @PostMapping
    public ResponseEntity<BudgetResponse> create(@Valid @RequestBody CreateBudgetRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(budgetService.create(request));
    }

    @Override
    @GetMapping
    public ResponseEntity<List<BudgetResponse>> findAll() {
        return ResponseEntity.ok(budgetService.findAll());
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(budgetService.findById(id));
    }

    @Override
    @PatchMapping("/{id}")
    public ResponseEntity<BudgetResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBudgetRequest request
    ) {
        return ResponseEntity.ok(budgetService.update(id, request));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        budgetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
