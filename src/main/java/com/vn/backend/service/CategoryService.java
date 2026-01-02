package com.vn.backend.service;

import com.vn.backend.dto.request.CreateCategoryRequest;
import com.vn.backend.dto.request.UpdateCategoryRequest;
import com.vn.backend.dto.response.CategoryResponse;
import com.vn.backend.dto.response.PagedResponse;
import com.vn.backend.exception.AppException;
import com.vn.backend.model.Category;
import com.vn.backend.repository.CategoryRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
     * Chuyển đổi Category entity sang CategoryResponse DTO
     * Bao gồm cả tên danh mục cha nếu có
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
     * Lấy tất cả danh mục (bao gồm cả cha và con)
     *
     * @return Danh sách tất cả danh mục
     */
    public PagedResponse<CategoryResponse> getAllCategories(String keyword, Pageable pageable) {
        Pageable pageableWithDefaultSort = pageable;
        if (pageable.getSort().isUnsorted()) {
            Sort defaultSort = Sort.by(Sort.Direction.DESC, "id");
            pageableWithDefaultSort = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    defaultSort
            );
        }

        Page<Category> categories;

        if (keyword != null && !keyword.trim().isEmpty()) {
            categories = categoryRepository.findByKeyword(keyword.trim(), pageableWithDefaultSort);
        } else {
            categories = categoryRepository.findAll(pageableWithDefaultSort);
        }

        List<CategoryResponse> categoryResponses = categories.getContent().stream()
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());

        PagedResponse<CategoryResponse> response = PagedResponse.<CategoryResponse>builder()
                .data(categoryResponses)
                .totalElements(categories.getTotalElements())
                .totalPages(categories.getTotalPages())
                .currentPage(categories.getNumber())
                .pageSize(categories.getSize())
                .hasNext(categories.hasNext())
                .hasPrevious(categories.hasPrevious())
                .build();

        return response;
    }

    /**
     * Lấy danh sách danh mục gốc (không có danh mục cha)
     *
     * @return Danh sách danh mục gốc
     */
    public List<CategoryResponse> getRootCategories() {
        log.info("Getting root categories");
        return categoryRepository.findByParentIdIsNull().stream()
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách danh mục con theo ID danh mục cha
     *
     * @param parentId ID của danh mục cha
     * @return Danh sách danh mục con
     * @throws AppException nếu danh mục cha không tồn tại
     */
    public List<CategoryResponse> getChildCategories(Long parentId) {
        log.info("Getting child categories for parent id: {}", parentId);

        // Kiểm tra danh mục cha có tồn tại không
        if (!categoryRepository.existsById(parentId)) {
            throw new AppException(HttpStatus.NOT_FOUND.value(), "Parent category not found");
        }

        return categoryRepository.findByParentId(parentId).stream()
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy thông tin chi tiết danh mục theo ID
     *
     * @param id ID của danh mục cần lấy
     * @return Thông tin danh mục
     * @throws AppException nếu danh mục không tồn tại
     */
    public CategoryResponse getCategoryById(Long id) {
        log.info("Getting category with id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Category not found"));
        return toCategoryResponse(category);
    }

    /**
     * Tạo danh mục mới
     *
     * @param request Thông tin danh mục cần tạo (tên, ID danh mục cha nếu có)
     * @return Thông tin danh mục vừa tạo
     * @throws AppException nếu danh mục cha không tồn tại (khi có parentId)
     */
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        log.info("Creating new category: {}", request.getName());

        // Kiểm tra danh mục cha có tồn tại không (nếu có parentId)
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
     * Cập nhật thông tin danh mục
     *
     * @param id      ID của danh mục cần cập nhật
     * @param request Thông tin mới (tên, parentId)
     * @return Thông tin danh mục sau khi cập nhật
     * @throws AppException nếu:
     *                      - Danh mục không tồn tại
     *                      - Danh mục cha không tồn tại
     *                      - Danh mục tự tham chiếu chính nó làm cha
     */
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        log.info("Updating category with id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Category not found"));

        // Kiểm tra tính hợp lệ của parentId nếu có
        if (request.getParentId() != null) {
            // Ngăn chặn tự tham chiếu (danh mục không thể là cha của chính nó)
            if (request.getParentId().equals(id)) {
                throw new AppException(HttpStatus.BAD_REQUEST.value(), "Category cannot be its own parent");
            }

            if (!categoryRepository.existsById(request.getParentId())) {
                throw new AppException(HttpStatus.NOT_FOUND.value(), "Parent category not found");
            }
        }

        // Cập nhật các trường nếu được cung cấp
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
     * Xóa danh mục
     *
     * @param id ID của danh mục cần xóa
     * @throws AppException nếu:
     *                      - Danh mục không tồn tại
     *                      - Danh mục có danh mục con (phải xóa con trước)
     */
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Deleting category with id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Category not found"));

        // Kiểm tra xem danh mục có danh mục con không
        List<Category> children = categoryRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                    "Cannot delete category with child categories. Delete children first.");
        }

        categoryRepository.delete(category);
        log.info("Category deleted successfully with id: {}", id);
    }

    // Method mới: Tìm kiếm categories với keyword và pagination
    public List<CategoryResponse> searchCategories(String keyword, Pageable pageable) {
        Page<Category> categories;

        if (keyword != null && !keyword.trim().isEmpty()) {
            // Tìm kiếm theo keyword
            categories = categoryRepository.findByNameContaining(keyword.trim(), pageable);
        } else {
            // Nếu không có keyword, lấy tất cả
            categories = categoryRepository.findAll(pageable);
        }

        return categories.getContent().stream()
                .map(this::toCategoryResponse)
                .toList();
    }
}