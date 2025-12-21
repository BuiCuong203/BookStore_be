package com.vn.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vn.backend.dto.request.CreateProductRequest;
import com.vn.backend.dto.request.UpdateProductRequest;
import com.vn.backend.dto.response.ProductResponse;
import com.vn.backend.exception.AppException;
import com.vn.backend.model.Category;
import com.vn.backend.model.Product;
import com.vn.backend.repository.CategoryRepository;
import com.vn.backend.repository.ProductRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProductService {

    ProductRepository productRepository;
    CategoryRepository categoryRepository;

    /**
     * Convert Product to ProductResponse
     */
    private ProductResponse toProductResponse(Product product) {
        long finalPrice = product.getPrice() - (product.getPrice() * product.getDiscount() / 100);

        return ProductResponse.builder()
                .id(product.getId())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .name(product.getName())
                .shortDescription(product.getShortDescription())
                .description(product.getDescription())
                .dimension(product.getDimension())
                .numberOfPages(product.getNumberOfPages())
                .isbn(product.getIsbn())
                .stockQuantity(product.getStockQuanity())
                .price(product.getPrice())
                .discount(product.getDiscount())
                .finalPrice(finalPrice)
                .publisher(product.getPublisher())
                .publisherDate(product.getPublisherDate())
                .ratingAvg(product.getRatingAvg())
                .ratingCount(product.getRatingCount())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    /**
     * Get all products with pagination
     */
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        log.info("Getting all products with pagination");
        return productRepository.findAll(pageable)
                .map(this::toProductResponse);
    }

    /**
     * Get product by ID
     */
    public ProductResponse getProductById(Long id) {
        log.info("Getting product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Product not found"));
        return toProductResponse(product);
    }

    /**
     * Create new product
     */
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating new product: {}", request.getName());

        // Validate category exists
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Category not found"));

        Product product = Product.builder()
                .category(category)
                .name(request.getName())
                .shortDescription(request.getShortDescription())
                .description(request.getDescription())
                .dimension(request.getDimension())
                .numberOfPages(request.getNumberOfPages() != null ? request.getNumberOfPages() : 0)
                .isbn(request.getIsbn())
                .stockQuanity(request.getStockQuantity())
                .price(request.getPrice())
                .discount(request.getDiscount() != null ? request.getDiscount() : 0)
                .publisher(request.getPublisher())
                .publisherDate(request.getPublisherDate())
                .ratingAvg(0.0)
                .ratingCount(0)
                .build();

        product = productRepository.save(product);
        log.info("Product created successfully with id: {}", product.getId());

        return toProductResponse(product);
    }

    /**
     * Update product
     */
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        log.info("Updating product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Product not found"));

        // Update category if provided
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Category not found"));
            product.setCategory(category);
        }

        // Update fields if provided
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getShortDescription() != null) {
            product.setShortDescription(request.getShortDescription());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getDimension() != null) {
            product.setDimension(request.getDimension());
        }
        if (request.getNumberOfPages() != null) {
            product.setNumberOfPages(request.getNumberOfPages());
        }
        if (request.getIsbn() != null) {
            product.setIsbn(request.getIsbn());
        }
        if (request.getStockQuantity() != null) {
            product.setStockQuanity(request.getStockQuantity());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getDiscount() != null) {
            product.setDiscount(request.getDiscount());
        }
        if (request.getPublisher() != null) {
            product.setPublisher(request.getPublisher());
        }
        if (request.getPublisherDate() != null) {
            product.setPublisherDate(request.getPublisherDate());
        }

        product = productRepository.save(product);
        log.info("Product updated successfully with id: {}", product.getId());

        return toProductResponse(product);
    }

    /**
     * Delete product
     */
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Product not found"));

        productRepository.delete(product);
        log.info("Product deleted successfully with id: {}", id);
    }

    /**
     * Tìm kiếm sản phẩm theo tên
     * @param keyword Từ khóa tìm kiếm
     * @param pageable Phân trang
     * @return Danh sách sản phẩm tìm được
     */
    public Page<ProductResponse> searchProductsByName(String keyword, Pageable pageable) {
        log.info("Searching products with keyword: {}", keyword);
        return productRepository.findByNameContainingIgnoreCase(keyword, pageable)
                .map(this::toProductResponse);
    }

    /**
     * Lọc sản phẩm theo danh mục
     * @param categoryId ID danh mục
     * @param pageable Phân trang
     * @return Danh sách sản phẩm trong danh mục
     */
    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        log.info("Getting products by category id: {}", categoryId);
        
        // Kiểm tra danh mục có tồn tại
        if (!categoryRepository.existsById(categoryId)) {
            throw new AppException(HttpStatus.NOT_FOUND.value(), "Category not found");
        }
        
        return productRepository.findByCategoryId(categoryId, pageable)
                .map(this::toProductResponse);
    }

    /**
     * Lọc sản phẩm theo khoảng giá
     * @param minPrice Giá tối thiểu
     * @param maxPrice Giá tối đa
     * @param pageable Phân trang
     * @return Danh sách sản phẩm trong khoảng giá
     */
    public Page<ProductResponse> getProductsByPriceRange(Long minPrice, Long maxPrice, Pageable pageable) {
        log.info("Getting products with price range: {} - {}", minPrice, maxPrice);
        
        if (minPrice < 0 || maxPrice < 0 || minPrice > maxPrice) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "Invalid price range");
        }
        
        return productRepository.findByPriceBetween(minPrice, maxPrice, pageable)
                .map(this::toProductResponse);
    }

    /**
     * Tìm kiếm nâng cao với nhiều bộ lọc
     * @param keyword Từ khóa tìm kiếm (có thể null)
     * @param categoryId ID danh mục (có thể null)
     * @param minPrice Giá tối thiểu (có thể null)
     * @param maxPrice Giá tối đa (có thể null)
     * @param pageable Phân trang và sắp xếp
     * @return Danh sách sản phẩm tìm được
     */
    public Page<ProductResponse> searchProductsAdvanced(String keyword, Long categoryId, 
                                                        Long minPrice, Long maxPrice, 
                                                        Pageable pageable) {
        log.info("Advanced search - keyword: {}, categoryId: {}, price: {}-{}", 
                 keyword, categoryId, minPrice, maxPrice);
        
        // Validate giá nếu được cung cấp
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "Min price cannot be greater than max price");
        }
        
        return productRepository.searchProducts(
                keyword != null ? keyword : "", 
                categoryId, 
                minPrice, 
                maxPrice, 
                pageable
        ).map(this::toProductResponse);
    }

    /**
     * Lấy sản phẩm mới nhất
     * @param pageable Phân trang (nên sort theo createdAt desc)
     * @return Danh sách sản phẩm mới nhất
     */
    public Page<ProductResponse> getNewestProducts(Pageable pageable) {
        log.info("Getting newest products");
        return productRepository.findAll(pageable)
                .map(this::toProductResponse);
    }

    /**
     * Lấy sản phẩm có rating cao
     * @param minRating Rating tối thiểu (ví dụ 4.0)
     * @param pageable Phân trang (nên sort theo ratingAvg desc)
     * @return Danh sách sản phẩm có rating cao
     */
    public Page<ProductResponse> getTopRatedProducts(Double minRating, Pageable pageable) {
        log.info("Getting top rated products with min rating: {}", minRating);
        
        if (minRating < 0 || minRating > 5) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "Rating must be between 0 and 5");
        }
        
        return productRepository.findByRatingAvgGreaterThanEqual(minRating, pageable)
                .map(this::toProductResponse);
    }

    /**
     * Lấy sản phẩm còn hàng
     * @param pageable Phân trang
     * @return Danh sách sản phẩm còn hàng (stock > 0)
     */
    public Page<ProductResponse> getAvailableProducts(Pageable pageable) {
        log.info("Getting available products (in stock)");
        return productRepository.findByStockQuanityGreaterThan(0, pageable)
                .map(this::toProductResponse);
    }

    /**
     * Kiểm tra sản phẩm còn đủ hàng không
     * @param productId ID sản phẩm
     * @param quantity Số lượng cần kiểm tra
     * @return true nếu đủ hàng, false nếu không
     */
    public boolean checkStockAvailability(Long productId, Integer quantity) {
        log.info("Checking stock for product {} with quantity {}", productId, quantity);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Product not found"));
        
        return product.getStockQuanity() >= quantity;
    }

    /**
     * Cập nhật số lượng tồn kho
     * @param productId ID sản phẩm
     * @param quantity Số lượng thay đổi (dương = thêm, âm = trừ)
     * @return Thông tin sản phẩm sau khi cập nhật
     */
    @Transactional
    public ProductResponse updateStock(Long productId, Integer quantity) {
        log.info("Updating stock for product {} by {}", productId, quantity);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Product not found"));
        
        int newStock = product.getStockQuanity() + quantity;
        
        if (newStock < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), 
                    "Insufficient stock. Current: " + product.getStockQuanity() + ", Requested: " + Math.abs(quantity));
        }
        
        product.setStockQuanity(newStock);
        product = productRepository.save(product);
        
        log.info("Stock updated successfully. New stock: {}", newStock);
        return toProductResponse(product);
    }
}

