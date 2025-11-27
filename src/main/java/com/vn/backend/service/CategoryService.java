package com.vn.backend.service;

import com.vn.backend.dto.request.CreateCategoryRequest;
import com.vn.backend.dto.request.UpdateCategoryRequest;
import com.vn.backend.dto.response.CategoryResponse;
import com.vn.backend.exception.AppException;
import com.vn.backend.model.Category;
import com.vn.backend.repository.CategoryRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CategoryService {

    CategoryRepository categoryRepository;

    /**
     * Convert Category to CategoryResponse
     */
    private CategoryResponse toCategoryResponse(Category category) {
        String parentName = null;
        if (category.getParentId() != null) {
            parentName = categoryRepository.findById(category.getParentId())
                    .map(Category::getName)
                    .orElse(null);
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParentId())
                .parentName(parentName)
                .build();
    }

    /**
     * Get all categories
     */
    public List<CategoryResponse> getAllCategories() {
        log.info("Getting all categories");
        return categoryRepository.findAll().stream()
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get root categories (categories without parent)
     */
    public List<CategoryResponse> getRootCategories() {
        log.info("Getting root categories");
        return categoryRepository.findByParentIdIsNull().stream()
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get child categories by parent ID
     */
    public List<CategoryResponse> getChildCategories(Long parentId) {
        log.info("Getting child categories for parent id: {}", parentId);

        // Verify parent exists
        if (!categoryRepository.existsById(parentId)) {
            throw new AppException(HttpStatus.NOT_FOUND.value(), "Parent category not found");
        }

        return categoryRepository.findByParentId(parentId).stream()
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get category by ID
     */
    public CategoryResponse getCategoryById(Long id) {
        log.info("Getting category with id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Category not found"));
        return toCategoryResponse(category);
    }

    /**
     * Create new category
     */
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        log.info("Creating new category: {}", request.getName());

        // Validate parent category exists if parentId is provided
        if (request.getParentId() != null) {
            if (!categoryRepository.existsById(request.getParentId())) {
                throw new AppException(HttpStatus.NOT_FOUND.value(), "Parent category not found");
            }
        }

        Category category = Category.builder()
                .name(request.getName())
                .parentId(request.getParentId())
                .build();

        category = categoryRepository.save(category);
        log.info("Category created successfully with id: {}", category.getId());

        return toCategoryResponse(category);
    }

    /**
     * Update category
     */
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        log.info("Updating category with id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Category not found"));

        // Validate parent category exists if parentId is provided
        if (request.getParentId() != null) {
            // Prevent self-referencing
            if (request.getParentId().equals(id)) {
                throw new AppException(HttpStatus.BAD_REQUEST.value(), "Category cannot be its own parent");
            }

            if (!categoryRepository.existsById(request.getParentId())) {
                throw new AppException(HttpStatus.NOT_FOUND.value(), "Parent category not found");
            }
        }

        // Update fields if provided
        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getParentId() != null) {
            category.setParentId(request.getParentId());
        }

        category = categoryRepository.save(category);
        log.info("Category updated successfully with id: {}", category.getId());

        return toCategoryResponse(category);
    }

    /**
     * Delete category
     */
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Deleting category with id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Category not found"));

        // Check if category has children
        List<Category> children = categoryRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                    "Cannot delete category with child categories. Delete children first.");
        }

        categoryRepository.delete(category);
        log.info("Category deleted successfully with id: {}", id);
    }
}

