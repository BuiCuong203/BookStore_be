package com.vn.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminDashboardResponse {
    DashboardStatsResponse statsData;
    List<MonthlyDataResponse> monthlyData;
    List<CategoryDataResponse> categoryData;
    List<RecentOrderDataResponse> recentOrdersData;
}
