package com.vn.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vn.backend.model.Category;
import com.vn.backend.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // Tìm kiếm theo tên (không phân biệt hoa thường)
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    // Lọc theo danh mục
    Page<Product> findByCategory(Category category, Pageable pageable);
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    
    // Lọc theo khoảng giá
    Page<Product> findByPriceBetween(Long minPrice, Long maxPrice, Pageable pageable);
    
    // Sản phẩm còn hàng
    Page<Product> findByStockQuanityGreaterThan(Integer minStock, Pageable pageable);
    
    // Top rated products
    Page<Product> findByRatingAvgGreaterThanEqual(Double minRating, Pageable pageable);
    
    // Tìm kiếm nâng cao: theo tên và danh mục
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> searchProducts(@Param("keyword") String keyword,
                                  @Param("categoryId") Long categoryId,
                                  @Param("minPrice") Long minPrice,
                                  @Param("maxPrice") Long maxPrice,
                                  Pageable pageable);
}

