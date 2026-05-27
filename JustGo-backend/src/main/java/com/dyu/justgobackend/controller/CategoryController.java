package com.dyu.justgobackend.controller;

import com.dyu.justgobackend.common.ApiResponse;
import com.dyu.justgobackend.dto.response.activity.CategoryResponse;
import com.dyu.justgobackend.dto.response.activity.TagResponse;
import com.dyu.justgobackend.service.CategoryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> listCategories() {
        return ApiResponse.success(categoryService.listCategories());
    }

    @GetMapping("/tags")
    public ApiResponse<List<TagResponse>> listTags() {
        return ApiResponse.success(categoryService.listTags());
    }

    @PostMapping("/admin/categories")
    public ApiResponse<CategoryResponse> createCategory(
            @RequestParam @NotBlank @Size(max = 30) String name,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) Integer sortOrder) {
        return ApiResponse.success(categoryService.createCategory(name, icon, sortOrder));
    }

    @PutMapping("/admin/categories/{id}")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable @Positive Long id,
            @RequestParam(required = false) @Size(max = 30) String name,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) Integer sortOrder) {
        return ApiResponse.success(categoryService.updateCategory(id, name, icon, sortOrder));
    }

    @DeleteMapping("/admin/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable @Positive Long id) {
        categoryService.deleteCategory(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/admin/tags")
    public ApiResponse<TagResponse> createTag(@RequestParam @NotBlank @Size(max = 30) String name) {
        return ApiResponse.success(categoryService.createTag(name));
    }

    @PutMapping("/admin/tags/{id}")
    public ApiResponse<TagResponse> updateTag(@PathVariable @Positive Long id,
                                               @RequestParam @NotBlank @Size(max = 30) String name) {
        return ApiResponse.success(categoryService.updateTag(id, name));
    }

    @DeleteMapping("/admin/tags/{id}")
    public ApiResponse<Void> deleteTag(@PathVariable @Positive Long id) {
        categoryService.deleteTag(id);
        return ApiResponse.success(null);
    }
}
