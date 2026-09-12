package com.amorim.finance_manager.invoice.mapper;

import com.amorim.finance_manager.invoice.dto.InvoiceDetailResponse;
import com.amorim.finance_manager.invoice.dto.InvoiceSummaryResponse;
import com.amorim.finance_manager.invoice.entity.Invoice;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    InvoiceSummaryResponse toSummary(Invoice invoice);

    @Mapping(target = "transactions", source = "transactions")
    @Mapping(target = "creditAppliedAmount", source = "creditAppliedAmount")
    InvoiceDetailResponse toDetail(
            Invoice invoice,
            List<TransactionResponse> transactions,
            BigDecimal creditAppliedAmount
    );
}
