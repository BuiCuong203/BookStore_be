package com.vn.backend.service;

import com.vn.backend.dto.request.CreateProductRequest;
import com.vn.backend.dto.request.UpdateProductRequest;
import com.vn.backend.dto.response.AuthorResponse;
import com.vn.backend.dto.response.PagedResponse;
import com.vn.backend.dto.response.ProductImageResponse;
import com.vn.backend.dto.response.ProductResponse;
import com.vn.backend.exception.AppException;
import com.vn.backend.model.Author;
import com.vn.backend.model.Category;
import com.vn.backend.model.Product;
import com.vn.backend.model.ProductImage;
import com.vn.backend.repository.AuthorRepository;
import com.vn.backend.repository.CategoryRepository;
import com.vn.backend.repository.ProductImageRepository;
import com.vn.backend.repository.ProductRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProductService {

    ProductRepository productRepository;
    CategoryRepository categoryRepository;
    AuthorRepository authorRepository;
    ProductImageRepository productImageRepository;

    private AuthorResponse toAuthorResponse(Author author) {
        return AuthorResponse.builder()
                .id(author.getId())
                .name(author.getName())
                .build();
    }

    private ProductImageResponse toProductImageResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .build();
    }

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
                .authors(
                        product.getAuthors() != null
                                ? product.getAuthors().stream()
                                .map(this::toAuthorResponse)
                                .collect(Collectors.toList())
                                : List.of()
                )
                .images(
                        product.getImages() != null
                                ? product.getImages().stream()
                                .map(this::toProductImageResponse)
                                .collect(Collectors.toList())
                                : List.of()
                )
                .build();
    }

    /**
     * Get all products with pagination
     */
    public PagedResponse<ProductResponse> getAllProducts(String keyword, Pageable pageable) {
        Pageable pageableWithDefaultSort = pageable;
        if (pageable.getSort().isUnsorted()) {
            Sort defaultSort = Sort.by(Sort.Direction.DESC, "id");
            pageableWithDefaultSort = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    defaultSort
            );
        }

        Page<Product> products;

        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.findByKeyword(keyword.trim(), pageableWithDefaultSort);
        } else {
            products = productRepository.findAll(pageableWithDefaultSort);
        }

        List<ProductResponse> productResponses = products.getContent().stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());

        PagedResponse<ProductResponse> response = PagedResponse.<ProductResponse>builder()
                .data(productResponses)
                .totalElements(products.getTotalElements())
                .totalPages(products.getTotalPages())
                .currentPage(products.getNumber())
                .pageSize(products.getSize())
                .hasNext(products.hasNext())
                .hasPrevious(products.hasPrevious())
                .build();

        return response;
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

        // 1. Kiểm tra category tồn tại
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->
                        new AppException(HttpStatus.NOT_FOUND.value(), "Category not found"));

        // 2. Tạo product
        Product product = Product.builder()
                .category(category)
                .name(request.getName())
                .shortDescription(request.getShortDescription())
                .description(request.getDescription())
                .dimension(request.getDimension())
                .numberOfPages(
                        request.getNumberOfPages() != null ? request.getNumberOfPages() : 0
                )
                .isbn(request.getIsbn())
                .stockQuanity(request.getStockQuantity())
                .price(request.getPrice())
                .discount(request.getDiscount() != null ? request.getDiscount() : 0)
                .publisher(request.getPublisher())
                .publisherDate(request.getPublisherDate())
                .ratingAvg(0.0)
                .ratingCount(0)
                .build();

        Product savedProduct = productRepository.save(product);

        // 3. Lưu authors (nếu có)
        if (request.getAuthors() != null && !request.getAuthors().isEmpty()) {
            List<Author> authors = request.getAuthors().stream()
                    .map(authorReq -> {
                        Author author = new Author();
                        author.setName(authorReq.getName());
                        author.setProduct(savedProduct);
                        return author;
                    })
                    .collect(Collectors.toList());

            authorRepository.saveAll(authors);
            product.setAuthors(authors);
        }

        // 4. Lưu images (nếu có)
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<ProductImage> images = request.getImages().stream()
                    .map(imageReq -> {
                        ProductImage image = new ProductImage();
                        image.setImageUrl(imageReq.getImageUrl());
                        image.setProduct(savedProduct);
                        return image;
                    })
                    .collect(Collectors.toList());

            productImageRepository.saveAll(images);
            product.setImages(images);
        }

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

        // 2. Update Category
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

        // 4. Update Authors (Logic: Nếu gửi list mới -> Xóa cũ, thêm mới)
        if (request.getAuthors() != null) {
            List<Author> currentAuthors = product.getAuthors();

            if (currentAuthors == null) {
                currentAuthors = new ArrayList<>();
                product.setAuthors(currentAuthors);
            }

            currentAuthors.clear();

            List<Author> newAuthors = request.getAuthors().stream()
                    .map(req -> {
                        Author author = new Author();
                        author.setName(req.getName());
                        author.setProduct(product);
                        return author;
                    }).toList();

            currentAuthors.addAll(newAuthors);
        }

        // 5. Update Images (Logic tương tự Authors)
        if (request.getImages() != null) {
            List<ProductImage> currentImages = product.getImages();

            if (currentImages == null) {
                currentImages = new ArrayList<>();
                product.setImages(currentImages);
            }

            currentImages.clear();

            List<ProductImage> newImages = request.getImages().stream()
                    .map(req -> {
                        ProductImage img = new ProductImage();
                        img.setImageUrl(req.getImageUrl());
                        img.setProduct(product);
                        return img;
                    }).toList();

            currentImages.addAll(newImages);
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully with id: {}", updatedProduct.getId());

        return toProductResponse(updatedProduct);
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
     *
     * @param keyword  Từ khóa tìm kiếm
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
     *
     * @param categoryId ID danh mục
     * @param pageable   Phân trang
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
     *
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
     *
     * @param keyword    Từ khóa tìm kiếm (có thể null)
     * @param categoryId ID danh mục (có thể null)
     * @param minPrice   Giá tối thiểu (có thể null)
     * @param maxPrice   Giá tối đa (có thể null)
     * @param pageable   Phân trang và sắp xếp
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
     *
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
     *
     * @param minRating Rating tối thiểu (ví dụ 4.0)
     * @param pageable  Phân trang (nên sort theo ratingAvg desc)
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
     *
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
     *
     * @param productId ID sản phẩm
     * @param quantity  Số lượng cần kiểm tra
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
     *
     * @param productId ID sản phẩm
     * @param quantity  Số lượng thay đổi (dương = thêm, âm = trừ)
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