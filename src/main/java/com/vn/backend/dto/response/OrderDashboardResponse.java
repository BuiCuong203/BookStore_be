package com.vn.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderDashboardResponse {
    private Long totalOrders;
    private Double totalRevenue;
    private Long deliveringOrders; // Số đơn đang giao
    private List<OrderStatusStat> statusBreakdown; // Dữ liệu cho biểu đồ

    @Data
    @Builder
    public static class OrderStatusStat {
        private String status;
        private Long count;
    }
}
