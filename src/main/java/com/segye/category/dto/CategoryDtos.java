package com.segye.category.dto;

import com.segye.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public class CategoryDtos {

    @Getter
    public static class UpsertRequest {
        @NotBlank
        private String name;

        @NotNull
        private CategoryType type;
    }

    public record CategoryResponse(Long id, String name, CategoryType type) {
    }
}
