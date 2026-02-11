package com.segye.category;

import com.segye.category.dto.CategoryDtos;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    public List<CategoryDtos.CategoryResponse> list(CategoryType type) {
        List<Category> categories = (type == null) ? repo.findAll() : repo.findByType(type);
        return categories.stream()
                .map(c -> new CategoryDtos.CategoryResponse(c.getId(), c.getName(), c.getType()))
                .toList();
    }

    public CategoryDtos.CategoryResponse create(CategoryDtos.UpsertRequest req) {
        Category saved = repo.save(new Category(req.getName(), req.getType()));
        return new CategoryDtos.CategoryResponse(saved.getId(), saved.getName(), saved.getType());
    }

    public CategoryDtos.CategoryResponse update(Long id, CategoryDtos.UpsertRequest req) {
        Category c = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("카테고리가 없습니다."));
        c.update(req.getName(), req.getType());
        return new CategoryDtos.CategoryResponse(c.getId(), c.getName(), c.getType());
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("카테고리가 없습니다.");
        }
        repo.deleteById(id);
    }
}
