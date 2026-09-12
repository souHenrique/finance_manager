package com.amorim.finance_manager.export.controller;

import com.amorim.finance_manager.config.openapi.OpenApiConfig;
import com.amorim.finance_manager.export.api.TransactionExportApiDocs;
import com.amorim.finance_manager.export.service.TransactionCsvExportService;
import com.amorim.finance_manager.transaction.dto.TransactionFilterRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/exports")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class TransactionExportController implements TransactionExportApiDocs {

    private static final MediaType CSV_MEDIA_TYPE = MediaType.parseMediaType("text/csv;charset=UTF-8");

    private final TransactionCsvExportService exportService;

    @Override
    @GetMapping(value = "/transactions.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportTransactions(@Valid @ModelAttribute TransactionFilterRequest filters) {
        byte[] csv = exportService.export(filters);

        String disposition = ContentDisposition
                .attachment()
                .filename("transactions.csv")
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(CSV_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentLength(csv.length)
                .body(csv);
    }
}
