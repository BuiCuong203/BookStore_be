package com.vn.backend.service;

import com.vn.backend.dto.response.*;
import com.vn.backend.repository.CategoryRepository;
import com.vn.backend.repository.OrderRepository;
import com.vn.backend.repository.ProductRepository;
import com.vn.backend.repository.UserRepository;
import com.vn.backend.util.enums.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    public ApiResponse<AdminDashboardResponse> getAdminDashboardData() {
        // 1. Lấy thống kê tổng quan
        DashboardStatsResponse statsData = getDashboardStats();

        // 2. Lấy dữ liệu theo tháng (8 tháng gần nhất)
        List<MonthlyDataResponse> monthlyData = getMonthlyData();

        // 3. Lấy dữ liệu phân bố danh mục
        List<CategoryDataResponse> categoryData = getCategoryData();

        // 4. Lấy dữ liệu đơn hàng 7 ngày gần đây
        List<RecentOrderDataResponse> recentOrdersData = getRecentOrdersData();

        AdminDashboardResponse response = AdminDashboardResponse.builder()
                .statsData(statsData)
                .monthlyData(monthlyData)
                .categoryData(categoryData)
                .recentOrdersData(recentOrdersData)
                .build();

        return ApiResponse.<AdminDashboardResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Lấy thông tin người dùng thành công")
                .data(response)
                .build();
    }

    private DashboardStatsResponse getDashboardStats() {
        // Get statistics
        Long totalUsers = userRepository.count();
        Long totalProducts = productRepository.count();
        Long totalOrders = orderRepository.count();
        BigDecimal totalRevenueBigDecimal = orderRepository.getTotalRevenue(OrderStatus.DELIVERED);
        Double totalRevenue = totalRevenueBigDecimal != null ? totalRevenueBigDecimal.doubleValue() : 0.0;

        // Đơn hàng hôm nay
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        Long todayOrders = orderRepository.countOrdersByDateRange(startOfDay, endOfDay);

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .todayOrders(todayOrders)
                .build();
    }

    private List<MonthlyDataResponse> getMonthlyData() {
        List<MonthlyDataResponse> monthlyData = new ArrayList<>();

        // Lấy 8 tháng gần nhất
        for (int i = 7; i >= 0; i--) {
            LocalDate monthStart = LocalDate.now().minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            String monthName = "T" + (8 - i);

            // Query data cho mỗi tháng (có thể cần implement custom query)
            Long users = userRepository.countUsersByDateRange(
                    monthStart.atStartOfDay(),
                    monthEnd.atTime(23, 59, 59)
            );

            Long orders = orderRepository.countOrdersByDateRange(
                    monthStart.atStartOfDay(),
                    monthEnd.atTime(23, 59, 59)
            );

            BigDecimal revenueBigDecimal = orderRepository.getRevenueByDateRange(
                    monthStart.atStartOfDay(),
                    monthEnd.atTime(23, 59, 59),
                    OrderStatus.DELIVERED
            );
            Double revenue = revenueBigDecimal != null ? revenueBigDecimal.doubleValue() : 0.0;

            // Product entity doesn't have createdAt, so use total count
            Long books = productRepository.count();

            monthlyData.add(MonthlyDataResponse.builder()
                    .name(monthName)
                    .users(users)
                    .orders(orders)
                    .revenue(revenue)
                    .books(books)
                    .build());
        }

        return monthlyData;
    }

    private List<CategoryDataResponse> getCategoryData() {
        List<CategoryDataResponse> categoryData = new ArrayList<>();
        List<Object[]> categoryStats = categoryRepository.getCategoryProductCount();

        for (Object[] stat : categoryStats) {
            String categoryName = (String) stat[0];
            Long productCount = (Long) stat[1];

            categoryData.add(CategoryDataResponse.builder()
                    .name(categoryName)
                    .value(productCount.doubleValue())
                    .build());
        }

        return categoryData;
    }

    private List<RecentOrderDataResponse> getRecentOrdersData() {
        List<RecentOrderDataResponse> recentData = new ArrayList<>();

        String[] dayNames = {"Hôm nay", "Hôm qua", "2 ngày trước", "3 ngày trước",
                "4 ngày trước", "5 ngày trước", "6 ngày trước"};

        for (int i = 0; i < 7; i++) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            Long orders = orderRepository.countOrdersByDateRange(startOfDay, endOfDay);
            BigDecimal revenueBigDecimal = orderRepository.getRevenueByDateRange(startOfDay, endOfDay, OrderStatus.DELIVERED);
            Double revenue = revenueBigDecimal != null ? revenueBigDecimal.doubleValue() : 0.0;

            recentData.add(RecentOrderDataResponse.builder()
                    .name(dayNames[i])
                    .orders(orders)
                    .revenue(revenue)
                    .build());
        }

        return recentData;
    }
}
