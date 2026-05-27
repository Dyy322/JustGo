package com.dyu.justgobackend.service;

import com.dyu.justgobackend.dto.response.activity.CategoryResponse;
import com.dyu.justgobackend.dto.response.activity.TagResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> listCategories();

    CategoryResponse createCategory(String name, String icon, Integer sortOrder);

    CategoryResponse updateCategory(Long id, String name, String icon, Integer sortOrder);

    void deleteCategory(Long id);

    List<TagResponse> listTags();

    TagResponse createTag(String name);

    TagResponse updateTag(Long id, String name);

    void deleteTag(Long id);
}
