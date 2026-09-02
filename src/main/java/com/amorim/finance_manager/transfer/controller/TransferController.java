package com.amorim.finance_manager.transfer.controller;

import com.amorim.finance_manager.config.openapi.OpenApiConfig;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transfer.api.TransferApiDocs;
import com.amorim.finance_manager.transfer.dto.CreateTransferRequest;
import com.amorim.finance_manager.transfer.service.TransferService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transferências")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class TransferController implements TransferApiDocs {

    private final TransferService transferService;

    @Override
    @PostMapping
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody CreateTransferRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transferService.transfer(request));
    }
}
