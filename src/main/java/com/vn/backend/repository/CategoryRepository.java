package com.vn.backend.repository;

import com.vn.backend.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByParentIdIsNull();

    List<Category> findByParentId(Long parentId);

    @Query("SELECT c.name, COUNT(p) FROM Category c LEFT JOIN Product p ON c.id = p.category.id GROUP BY c.id, c.name")
    List<Object[]> getCategoryProductCount();
}
