package com.campusknowledgeqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusknowledgeqa.dto.CategoryRequest;
import com.campusknowledgeqa.entity.KbCategory;
import com.campusknowledgeqa.mapper.KbCategoryMapper;
import com.campusknowledgeqa.mapper.KbDocumentMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/** 知识库分类 CRUD */
@Service
public class CategoryService {

    private final KbCategoryMapper categoryMapper;
    private final KbDocumentMapper documentMapper;

    public CategoryService(KbCategoryMapper categoryMapper, KbDocumentMapper documentMapper) {
        this.categoryMapper = categoryMapper;
        this.documentMapper = documentMapper;
    }

    public List<KbCategory> list() {
        return categoryMapper.selectList(new LambdaQueryWrapper<KbCategory>().orderByAsc(KbCategory::getSort));
    }

    public KbCategory create(CategoryRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        if (exists(request.name(), null)) {
            throw new IllegalArgumentException("分类名称已存在");
        }
        KbCategory category = new KbCategory();
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setSort(request.sort() == null ? 0 : request.sort());
        categoryMapper.insert(category);
        return categoryMapper.selectById(category.getId());
    }

    public KbCategory update(Long id, CategoryRequest request) {
        KbCategory category = categoryMapper.selectById(id);
        if (category == null) {
            return null;
        }
        if (request.name() != null && !request.name().isBlank()) {
            if (exists(request.name(), id)) {
                throw new IllegalArgumentException("分类名称已存在");
            }
            category.setName(request.name().trim());
        }
        category.setDescription(request.description());
        if (request.sort() != null) {
            category.setSort(request.sort());
        }
        categoryMapper.updateById(category);
        return categoryMapper.selectById(id);
    }

    /**
     * 删除分类：该分类下文档的 category_id 由外键置空（ON DELETE SET NULL）
     */
    public boolean delete(Long id) {
        KbCategory category = categoryMapper.selectById(id);
        if (category == null) {
            return false;
        }
        categoryMapper.deleteById(id);
        return true;
    }

    public long countDocuments(Long categoryId) {
        Long count = documentMapper.selectCount(
                new LambdaQueryWrapper<com.campusknowledgeqa.entity.KbDocument>()
                        .eq(com.campusknowledgeqa.entity.KbDocument::getCategoryId, categoryId));
        return count == null ? 0 : count;
    }

    private boolean exists(String name, Long excludeId) {
        LambdaQueryWrapper<KbCategory> wrapper = new LambdaQueryWrapper<KbCategory>()
                .eq(KbCategory::getName, name.trim());
        if (excludeId != null) {
            wrapper.ne(KbCategory::getId, excludeId);
        }
        Long count = categoryMapper.selectCount(wrapper);
        return count != null && count > 0;
    }
}
