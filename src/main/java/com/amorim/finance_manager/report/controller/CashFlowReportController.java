package com.amorim.finance_manager.report.controller;

import com.amorim.finance_manager.config.openapi.OpenApiConfig;
import com.amorim.finance_manager.report.api.CashFlowReportApiDocs;
import com.amorim.finance_manager.report.dto.*;
import com.amorim.finance_manager.report.service.CashFlowReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports/cash")
@Tag(name = "Relatórios de caixa")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class CashFlowReportController implements CashFlowReportApiDocs {

    private final CashFlowReportService reportService;

    @Override
    @GetMapping("/daily")
    public ResponseEntity<DailyCashFlowResponse> daily(@Valid @ModelAttribute CashFlowReportRequest request) {
        return ResponseEntity.ok(reportService.daily(request.date()));
    }

    @Override
    @GetMapping("/weekly")
    public ResponseEntity<WeeklyCashFlowResponse> weekly(@Valid @ModelAttribute CashFlowReportRequest request) {
        return ResponseEntity.ok(reportService.weekly(request.date()));
    }

    @Override
    @GetMapping("/monthly")
    public ResponseEntity<MonthlyCashFlowResponse> monthly(
            @Valid @ModelAttribute MonthlyCashFlowReportRequest request
    ) {
        return ResponseEntity.ok(reportService.monthly(request.year(), request.month()));
    }

    @Override
    @GetMapping("/annual")
    public ResponseEntity<AnnualCashFlowResponse> annual(
            @Valid @ModelAttribute AnnualCashFlowReportRequest request
    ) {
        return ResponseEntity.ok(reportService.annual(request.year()));
    }
}
