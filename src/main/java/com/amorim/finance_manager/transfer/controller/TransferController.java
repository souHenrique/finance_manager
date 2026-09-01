package com.amorim.finance_manager.transfer.controller;

import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transfer.dto.CreateTransferRequest;
import com.amorim.finance_manager.transfer.service.TransferService;
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
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody CreateTransferRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transferService.transfer(request));
    }
}
