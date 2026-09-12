package com.amorim.finance_manager.transaction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Audited
@AuditTable(value = "transactions_aud")
@Table(
        name = "transactions",
        indexes = {
                @Index(
                        name = "idx_transactions_user_competence_date",
                        columnList = "user_id, competence_date"
                ),
                @Index(
                        name = "idx_transactions_user_effective_date",
                        columnList = "user_id, effective_date"
                ),
                @Index(
                        name = "idx_transactions_user_status",
                        columnList = "user_id, status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "competence_date", nullable = false)
    private LocalDate competenceDate;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "source_account_id")
    private UUID sourceAccountId;

    @Column(name = "destination_account_id")
    private UUID destinationAccountId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "credit_card_id")
    private UUID creditCardId;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "installment_group_id")
    private UUID installmentGroupId;

    @Column(name = "installment_number")
    private Integer installmentNumber;

    @Column(name = "installment_count")
    private Integer installmentCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if (status == null) {
            status = TransactionStatus.PENDING;
        }

        createdAt = now;
        updatedAt = now;
    }

    @NotAudited
    @Version
    @Column(nullable = false)
    private Long version;

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
