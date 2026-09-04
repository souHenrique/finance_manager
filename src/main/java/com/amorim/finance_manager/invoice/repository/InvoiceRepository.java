package com.amorim.finance_manager.invoice.repository;

import com.amorim.finance_manager.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByCreditCardIdAndReferenceMonthAndReferenceYear(
            UUID creditCardId,
            Integer referenceMonth,
            Integer referenceYear);
}
