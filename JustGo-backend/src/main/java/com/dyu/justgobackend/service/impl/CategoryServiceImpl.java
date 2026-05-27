package com.dyu.justgobackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dyu.justgobackend.dto.response.activity.CategoryResponse;
import com.dyu.justgobackend.dto.response.activity.TagResponse;
import com.dyu.justgobackend.entity.activity.Activity;
import com.dyu.justgobackend.entity.activity.ActivityCategory;
import com.dyu.justgobackend.entity.activity.ActivityTag;
import com.dyu.justgobackend.exception.BusinessException;
import com.dyu.justgobackend.mapper.activity.ActivityCategoryMapper;
import com.dyu.justgobackend.mapper.activity.ActivityMapper;
import com.dyu.justgobackend.mapper.activity.ActivityTagMapper;
import com.dyu.justgobackend.mapper.activity.ActivityTagRelMapper;
import com.dyu.justgobackend.service.CategoryService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final String CATEGORY_CACHE_KEY = "jg:categories";
    private static final String TAG_CACHE_KEY = "jg:tags";
    private static final long CACHE_TTL_HOURS = 1;

    private final ActivityCategoryMapper categoryMapper;
    private final ActivityTagMapper tagMapper;
    private final ActivityTagRelMapper tagRelMapper;
    private final ActivityMapper activityMapper;
    private final StringRedisTemplate redisTemplate;

    public CategoryServiceImpl(
            ActivityCategoryMapper categoryMapper,
            ActivityTagMapper tagMapper,
            ActivityTagRelMapper tagRelMapper,
            ActivityMapper activityMapper,
            StringRedisTemplate redisTemplate) {
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.tagRelMapper = tagRelMapper;
        this.activityMapper = activityMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<CategoryResponse> listCategories() {
        List<CategoryResponse> cached = getCachedCategories();
        if (cached != null) {
            return cached;
        }

        List<ActivityCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<ActivityCategory>().orderByAsc(ActivityCategory::getSortOrder));
        List<CategoryResponse> result = categories.stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getIcon(), c.getSortOrder()))
                .toList();

        cacheCategories(result);
        return result;
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(String name, String icon, Integer sortOrder) {
        ActivityCategory category = new ActivityCategory();
        category.setName(name);
        category.setIcon(icon);
        category.setSortOrder(sortOrder != null ? sortOrder : 0);
        categoryMapper.insert(category);
        evictCategoryCache();
        return toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, String name, String icon, Integer sortOrder) {
        ActivityCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(404, "分类不存在");
        }
        if (name != null) category.setName(name);
        if (icon != null) category.setIcon(icon);
        if (sortOrder != null) category.setSortOrder(sortOrder);
        categoryMapper.updateById(category);
        evictCategoryCache();
        return toResponse(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        ActivityCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(404, "分类不存在");
        }

        long count = activityMapper.selectCount(
                new LambdaQueryWrapper<Activity>()
                        .eq(Activity::getCategoryId, id));
        if (count > 0) {
            throw new BusinessException(400, "该分类下有 " + count + " 个活动，无法删除");
        }

        categoryMapper.deleteById(id);
        evictCategoryCache();
    }

    @Override
    public List<TagResponse> listTags() {
        List<TagResponse> cached = getCachedTags();
        if (cached != null) {
            return cached;
        }

        List<ActivityTag> tags = tagMapper.selectList(null);
        List<TagResponse> result = tags.stream()
                .map(t -> new TagResponse(t.getId(), t.getName()))
                .toList();

        cacheTags(result);
        return result;
    }

    @Override
    @Transactional
    public TagResponse createTag(String name) {
        ActivityTag tag = new ActivityTag();
        tag.setName(name);
        tagMapper.insert(tag);
        evictTagCache();
        return toResponse(tag);
    }

    @Override
    @Transactional
    public TagResponse updateTag(Long id, String name) {
        ActivityTag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(404, "标签不存在");
        }
        tag.setName(name);
        tagMapper.updateById(tag);
        evictTagCache();
        return toResponse(tag);
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        ActivityTag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(404, "标签不存在");
        }
        tagRelMapper.deleteByTagId(id);
        tagMapper.deleteById(id);
        evictTagCache();
    }

    @SuppressWarnings("unchecked")
    private List<CategoryResponse> getCachedCategories() {
        String json = redisTemplate.opsForValue().get(CATEGORY_CACHE_KEY);
        if (json == null) return null;
        try {
            return com.fasterxml.jackson.databind.json.JsonMapper.builder()
                    .build()
                    .readValue(json,
                            new com.fasterxml.jackson.core.type.TypeReference<List<CategoryResponse>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private void cacheCategories(List<CategoryResponse> categories) {
        try {
            String json = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                    .build()
                    .writeValueAsString(categories);
            redisTemplate.opsForValue().set(CATEGORY_CACHE_KEY, json, CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception ignored) {
        }
    }

    private void evictCategoryCache() {
        redisTemplate.delete(CATEGORY_CACHE_KEY);
    }

    @SuppressWarnings("unchecked")
    private List<TagResponse> getCachedTags() {
        String json = redisTemplate.opsForValue().get(TAG_CACHE_KEY);
        if (json == null) return null;
        try {
            return com.fasterxml.jackson.databind.json.JsonMapper.builder()
                    .build()
                    .readValue(json,
                            new com.fasterxml.jackson.core.type.TypeReference<List<TagResponse>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private void cacheTags(List<TagResponse> tags) {
        try {
            String json = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                    .build()
                    .writeValueAsString(tags);
            redisTemplate.opsForValue().set(TAG_CACHE_KEY, json, CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception ignored) {
        }
    }

    private void evictTagCache() {
        redisTemplate.delete(TAG_CACHE_KEY);
    }

    private static CategoryResponse toResponse(ActivityCategory c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getIcon(), c.getSortOrder());
    }

    private static TagResponse toResponse(ActivityTag t) {
        return new TagResponse(t.getId(), t.getName());
    }
}
