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
        name = "credit_card_credit_applications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_credit_applications_credit_invoice",
                columnNames = {"credit_id", "invoice_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class CreditCardCreditApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "credit_id", nullable = false)
    private UUID creditId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

}
