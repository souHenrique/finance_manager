package com.amorim.finance_manager.dashboard.controller;

import com.amorim.finance_manager.config.openapi.OpenApiConfig;
import com.amorim.finance_manager.dashboard.api.DashboardApiDocs;
import com.amorim.finance_manager.dashboard.dto.DashboardResponse;
import com.amorim.finance_manager.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard financeiro")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class DashboardController implements DashboardApiDocs {

    private final DashboardService dashboardService;

    @Override
    @GetMapping
    public ResponseEntity<DashboardResponse> get() {
        return ResponseEntity.ok(dashboardService.get());
    }
}
