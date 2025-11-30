package com.vn.backend.service;

import com.vn.backend.dto.request.CreateCouponRequest;
import com.vn.backend.dto.request.UpdateCouponRequest;
import com.vn.backend.dto.response.CouponResponse;
import com.vn.backend.exception.AppException;
import com.vn.backend.model.Coupon;
import com.vn.backend.repository.CouponRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CouponService {

    CouponRepository couponRepository;

    /**
     * Convert Coupon to CouponResponse
     */
    private CouponResponse toCouponResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .stockQuantity(coupon.getStockQuantity())
                .discountType(coupon.getDiscountType())
                .discount(coupon.getDiscount())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }

    /**
     * Validate discount value based on type
     */
    private void validateDiscount(String discountType, int discount) {
        if ("PERCENTAGE".equals(discountType) && discount > 100) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                "Percentage discount cannot exceed 100%");
        }
        if ("FIXED".equals(discountType) && discount <= 0) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                "Fixed discount must be greater than 0");
        }
    }

    /**
     * Get all coupons with pagination
     */
    public Page<CouponResponse> getAllCoupons(Pageable pageable) {
        log.info("Getting all coupons with pagination");
        return couponRepository.findAll(pageable)
                .map(this::toCouponResponse);
    }

    /**
     * Get all available coupons (stock > 0)
     */
    public List<CouponResponse> getAvailableCoupons() {
        log.info("Getting available coupons");
        return couponRepository.findByStockQuantityGreaterThan(0).stream()
                .map(this::toCouponResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get coupon by ID
     */
    public CouponResponse getCouponById(Long id) {
        log.info("Getting coupon with id: {}", id);
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Coupon not found"));
        return toCouponResponse(coupon);
    }

    /**
     * Get coupon by code
     */
    public CouponResponse getCouponByCode(String code) {
        log.info("Getting coupon with code: {}", code);
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Coupon not found"));
        return toCouponResponse(coupon);
    }

    /**
     * Validate and apply coupon
     */
    public CouponResponse validateCoupon(String code) {
        log.info("Validating coupon: {}", code);

        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(),
                    "Coupon code is invalid"));

        if (coupon.getStockQuantity() <= 0) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                "This coupon has been used up");
        }

        log.info("Coupon {} is valid", code);
        return toCouponResponse(coupon);
    }

    /**
     * Create new coupon
     */
    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        log.info("Creating new coupon: {}", request.getCode());

        // Check if coupon code already exists
        if (couponRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                "Coupon code already exists");
        }

        // Validate discount
        validateDiscount(request.getDiscountType(), request.getDiscount());

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .stockQuantity(request.getStockQuantity())
                .discountType(request.getDiscountType().toUpperCase())
                .discount(request.getDiscount())
                .build();

        coupon = couponRepository.save(coupon);
        log.info("Coupon created successfully with id: {}", coupon.getId());

        return toCouponResponse(coupon);
    }

    /**
     * Update coupon
     */
    @Transactional
    public CouponResponse updateCoupon(Long id, UpdateCouponRequest request) {
        log.info("Updating coupon with id: {}", id);

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Coupon not found"));

        // Update fields if provided
        if (request.getDescription() != null) {
            coupon.setDescription(request.getDescription());
        }
        if (request.getStockQuantity() != null) {
            coupon.setStockQuantity(request.getStockQuantity());
        }
        if (request.getDiscountType() != null) {
            coupon.setDiscountType(request.getDiscountType().toUpperCase());
        }
        if (request.getDiscount() != null) {
            coupon.setDiscount(request.getDiscount());
        }

        // Validate discount if type or value changed
        if (request.getDiscountType() != null || request.getDiscount() != null) {
            validateDiscount(coupon.getDiscountType(), coupon.getDiscount());
        }

        coupon = couponRepository.save(coupon);
        log.info("Coupon updated successfully with id: {}", coupon.getId());

        return toCouponResponse(coupon);
    }

    /**
     * Delete coupon
     */
    @Transactional
    public void deleteCoupon(Long id) {
        log.info("Deleting coupon with id: {}", id);

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Coupon not found"));

        couponRepository.delete(coupon);
        log.info("Coupon deleted successfully with id: {}", id);
    }

    /**
     * Use coupon (decrease stock by 1)
     */
    @Transactional
    public void useCoupon(String code) {
        log.info("Using coupon: {}", code);

        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Coupon not found"));

        if (coupon.getStockQuantity() <= 0) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                "This coupon has been used up");
        }

        coupon.setStockQuantity(coupon.getStockQuantity() - 1);
        couponRepository.save(coupon);

        log.info("Coupon {} used. Remaining stock: {}", code, coupon.getStockQuantity());
    }

    /**
     * Calculate discount amount
     */
    public Long calculateDiscount(String code, Long originalAmount) {
        log.info("Calculating discount for coupon: {} with amount: {}", code, originalAmount);

        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Coupon not found"));

        if (coupon.getStockQuantity() <= 0) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                "This coupon has been used up");
        }

        Long discountAmount;
        if ("PERCENTAGE".equals(coupon.getDiscountType())) {
            discountAmount = (originalAmount * coupon.getDiscount()) / 100;
        } else { // FIXED
            discountAmount = Long.valueOf(coupon.getDiscount());
        }

        log.info("Discount amount calculated: {}", discountAmount);
        return discountAmount;
    }
}

