package com.amorim.finance_manager.creditcard.entity;

import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "credit_card_refund_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_refund_items_original_transaction",
                columnNames = "original_transaction_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class CreditCardRefundItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "refund_id", nullable = false)
    private UUID refundId;

    @Column(name = "original_transaction_id", nullable = false)
    private UUID originalTransactionId;

    @Column(name = "original_invoice_id", nullable = false)
    private UUID originalInvoiceId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "original_invoice_status",
            nullable = false,
            length = 20
    )
    private InvoiceStatus originalInvoiceStatus;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CreditCardRefundTreatment treatment;
}
