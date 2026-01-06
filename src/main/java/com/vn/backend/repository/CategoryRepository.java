package com.vn.backend.repository;

import com.vn.backend.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByParentIdIsNull();

    List<Category> findByParentId(Long parentId);
    @Query("SELECT c.name, COUNT(p) FROM Category c LEFT JOIN Product p ON c.id = p.category.id GROUP BY c.id, c.name")
    List<Object[]> getCategoryProductCount();

    @Query("SELECT c FROM Category c WHERE " +
            "c.name LIKE %:keyword%")
    Page<Category> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Tìm kiếm categories theo tên (có phân trang)
    @Query("SELECT c FROM Category c WHERE c.name LIKE %:keyword%")
    Page<Category> findByNameContaining(@Param("keyword") String keyword, Pageable pageable);
}
