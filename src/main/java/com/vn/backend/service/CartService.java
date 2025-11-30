package com.vn.backend.service;

import com.vn.backend.dto.request.AddToCartRequest;
import com.vn.backend.dto.request.UpdateCartItemRequest;
import com.vn.backend.dto.response.CartItemResponse;
import com.vn.backend.dto.response.CartResponse;
import com.vn.backend.exception.AppException;
import com.vn.backend.model.Cart;
import com.vn.backend.model.CartItem;
import com.vn.backend.model.Product;
import com.vn.backend.model.User;
import com.vn.backend.repository.CartItemRepository;
import com.vn.backend.repository.CartRepository;
import com.vn.backend.repository.ProductRepository;
import com.vn.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CartService {

    CartRepository cartRepository;
    CartItemRepository cartItemRepository;
    ProductRepository productRepository;
    UserRepository userRepository;

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "User not found"));
    }

    /**
     * Get or create cart for user
     */
    private Cart getOrCreateCart(User user) {
        return cartRepository.findByCustomer(user)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .customer(user)
                            .total(0L)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    /**
     * Calculate cart total
     */
    private Long calculateCartTotal(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCart(cart);
        return items.stream()
                .mapToLong(CartItem::getTotal)
                .sum();
    }

    /**
     * Calculate item total based on product price, discount and quantity
     */
    private Long calculateItemTotal(Product product, int quantity) {
        long originalPrice = product.getPrice();
        int discount = product.getDiscount();
        long finalPrice = originalPrice - (originalPrice * discount / 100);
        return finalPrice * quantity;
    }

    /**
     * Convert CartItem to CartItemResponse
     */
    private CartItemResponse toCartItemResponse(CartItem cartItem) {
        Product product = cartItem.getProduct();
        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productPrice(product.getPrice())
                .productDiscount(product.getDiscount())
                .quantity(cartItem.getQuantity())
                .total(cartItem.getTotal())
                .build();
    }

    /**
     * Convert Cart to CartResponse
     */
    private CartResponse toCartResponse(Cart cart, List<CartItem> items) {
        List<CartItemResponse> itemResponses = items.stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());

        int totalItems = items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .customerId(cart.getCustomer().getId())
                .items(itemResponses)
                .total(cart.getTotal())
                .totalItems(totalItems)
                .build();
    }

    /**
     * Get user cart
     */
    public CartResponse getCart() {
        User currentUser = getCurrentUser();
        Cart cart = getOrCreateCart(currentUser);
        List<CartItem> items = cartItemRepository.findByCart(cart);
        return toCartResponse(cart, items);
    }

    /**
     * Add item to cart
     */
    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {
        User currentUser = getCurrentUser();
        Cart cart = getOrCreateCart(currentUser);

        // Check if product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Product not found"));

        // Check stock availability
        if (product.getStockQuanity() < request.getQuantity()) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "Not enough stock available");
        }

        // Check if item already exists in cart
        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .map(existingItem -> {
                    int newQuantity = existingItem.getQuantity() + request.getQuantity();
                    if (product.getStockQuanity() < newQuantity) {
                        throw new AppException(HttpStatus.BAD_REQUEST.value(), "Not enough stock available");
                    }
                    existingItem.setQuantity(newQuantity);
                    existingItem.setTotal(calculateItemTotal(product, newQuantity));
                    return existingItem;
                })
                .orElseGet(() -> {
                    Long itemTotal = calculateItemTotal(product, request.getQuantity());
                    return CartItem.builder()
                            .cart(cart)
                            .product(product)
                            .quantity(request.getQuantity())
                            .total(itemTotal)
                            .build();
                });

        cartItemRepository.save(cartItem);

        // Update cart total
        cart.setTotal(calculateCartTotal(cart));
        cartRepository.save(cart);

        List<CartItem> items = cartItemRepository.findByCart(cart);
        return toCartResponse(cart, items);
    }

    /**
     * Update cart item quantity
     */
    @Transactional
    public CartResponse updateCartItem(Long itemId, UpdateCartItemRequest request) {
        User currentUser = getCurrentUser();
        Cart cart = getOrCreateCart(currentUser);

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Cart item not found"));

        // Verify item belongs to user's cart
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN.value(), "This item does not belong to your cart");
        }

        Product product = cartItem.getProduct();

        // Check stock availability
        if (product.getStockQuanity() < request.getQuantity()) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "Not enough stock available");
        }

        // Update quantity and total
        cartItem.setQuantity(request.getQuantity());
        cartItem.setTotal(calculateItemTotal(product, request.getQuantity()));
        cartItemRepository.save(cartItem);

        // Update cart total
        cart.setTotal(calculateCartTotal(cart));
        cartRepository.save(cart);

        List<CartItem> items = cartItemRepository.findByCart(cart);
        return toCartResponse(cart, items);
    }

    /**
     * Remove item from cart
     */
    @Transactional
    public CartResponse removeCartItem(Long itemId) {
        User currentUser = getCurrentUser();
        Cart cart = getOrCreateCart(currentUser);

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Cart item not found"));

        // Verify item belongs to user's cart
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN.value(), "This item does not belong to your cart");
        }

        cartItemRepository.delete(cartItem);

        // Update cart total
        cart.setTotal(calculateCartTotal(cart));
        cartRepository.save(cart);

        List<CartItem> items = cartItemRepository.findByCart(cart);
        return toCartResponse(cart, items);
    }

    /**
     * Clear all items from cart
     */
    @Transactional
    public void clearCart() {
        User currentUser = getCurrentUser();
        Cart cart = cartRepository.findByCustomer(currentUser)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Cart not found"));

        cartItemRepository.deleteByCartId(cart.getId());

        cart.setTotal(0L);
        cartRepository.save(cart);
    }
}
