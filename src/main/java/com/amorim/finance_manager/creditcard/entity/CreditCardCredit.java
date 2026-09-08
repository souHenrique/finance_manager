package com.amorim.finance_manager.creditcard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "credit_card_credits",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_card_credits_refund_item",
                columnNames = "refund_item_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class CreditCardCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "credit_card_id", nullable = false)
    private UUID creditCardId;

    @Column(name = "refund_item_id", nullable = false)
    private UUID refundItemId;

    @Column(
            name = "original_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal originalAmount;

    @Column(
            name = "remaining_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal remainingAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
