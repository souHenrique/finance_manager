package com.amorim.finance_manager.creditcard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credit_card_refunds")
@Getter
@Setter
@NoArgsConstructor
public class CreditCardRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "credit_card_id", nullable = false)
    private UUID creditCardId;

    @Column(name = "selected_transaction_id", nullable = false)
    private UUID selectedTransactionId;

    @Column(name = "installment_group_id")
    private UUID installmentGroupId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(
            name = "total_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal totalAmount;

    @Column(
            name = "limit_restored_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal limitRestoredAmount;

    @Column(
            name = "paid_compensation_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal paidCompensationAmount;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
