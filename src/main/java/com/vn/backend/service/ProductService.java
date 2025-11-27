package com.vn.backend.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}

