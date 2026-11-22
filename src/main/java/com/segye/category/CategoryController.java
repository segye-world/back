package com.segye.category;

import com.segye.category.dto.CategoryDtos;
import com.segye.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<CategoryDtos.CategoryResponse>> list(@AuthenticationPrincipal Long memberId,
                                                                 @RequestParam(required = false) CategoryType type) {
        return ApiResponse.ok(service.list(memberId, type));
    }

    @PostMapping
    public ApiResponse<CategoryDtos.CategoryResponse> create(@AuthenticationPrincipal Long memberId,
                                                            @RequestBody @Valid CategoryDtos.UpsertRequest req) {
        return ApiResponse.ok(service.create(memberId, req));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryDtos.CategoryResponse> update(@AuthenticationPrincipal Long memberId,
                                                            @PathVariable Long id,
                                                            @RequestBody @Valid CategoryDtos.UpsertRequest req) {
        return ApiResponse.ok(service.update(memberId, id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long memberId, @PathVariable Long id) {
        service.delete(memberId, id);
        return ApiResponse.ok();
    }
}
