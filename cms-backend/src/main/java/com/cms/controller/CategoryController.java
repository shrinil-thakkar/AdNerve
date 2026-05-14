package com.cms.controller;

import com.cms.dto.request.UpsertCategoryRequest;
import com.cms.dto.response.ApiResponse;
import com.cms.dto.response.CategoryResponse;
import com.cms.entity.CategoryType;
import com.cms.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/screens/{screenId}/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> list(@PathVariable UUID screenId) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.listCategories(screenId)));
    }

    @PutMapping("/{category}")
    public ResponseEntity<ApiResponse<CategoryResponse>> upsert(
            @PathVariable UUID screenId,
            @PathVariable CategoryType category,
            @Valid @RequestBody UpsertCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.upsertCategory(screenId, category, request)));
    }

    @PatchMapping("/{category}/toggle")
    public ResponseEntity<ApiResponse<CategoryResponse>> toggle(
            @PathVariable UUID screenId,
            @PathVariable CategoryType category) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.toggleActive(screenId, category)));
    }
}
