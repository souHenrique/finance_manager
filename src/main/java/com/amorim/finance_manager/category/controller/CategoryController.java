package com.amorim.finance_manager.category.controller;

import com.amorim.finance_manager.category.dto.CategoryResponse;
import com.amorim.finance_manager.category.dto.CreateCategoryRequest;
import com.amorim.finance_manager.category.dto.UpdateCategoryRequest;
import com.amorim.finance_manager.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(categoryService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findAll() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }
}
