package com.vn.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * @return Danh sách tất cả danh mục
     */
    public List<CategoryResponse> getAllCategories() {
        log.info("Getting all categories");
        return categoryRepository.findAll().stream()
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách danh mục gốc (không có danh mục cha)
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
     * @param id ID của danh mục cần cập nhật
     * @param request Thông tin mới (tên, parentId)
     * @return Thông tin danh mục sau khi cập nhật
     * @throws AppException nếu:
     *         - Danh mục không tồn tại
     *         - Danh mục cha không tồn tại
     *         - Danh mục tự tham chiếu chính nó làm cha
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
     * @param id ID của danh mục cần xóa
     * @throws AppException nếu:
     *         - Danh mục không tồn tại
     *         - Danh mục có danh mục con (phải xóa con trước)
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
}