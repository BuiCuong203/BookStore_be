package com.vn.backend.controller;

import com.vn.backend.dto.response.AdminDashboardResponse;
import com.vn.backend.dto.response.ApiResponse;
import com.vn.backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboard() {
        try {
            ApiResponse<AdminDashboardResponse> res = dashboardService.getAdminDashboardData();
            return ResponseEntity.status(res.getStatusCode()).body(res);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get admin dashboard data", e);
        }
    }
}
