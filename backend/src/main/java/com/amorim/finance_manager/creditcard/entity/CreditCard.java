package com.amorim.finance_manager.creditcard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.AuditTable;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "credit_cards",
        indexes = {
                @Index(
                        name = "idx_credit_cards_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_credit_cards_user_status",
                        columnList = "user_id, status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@Audited
@AuditTable(value = "credit_cards_aud")
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(
            name = "credit_limit",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal creditLimit;

    @Column(
            name = "available_limit",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal availableLimit;

    @Column(name = "closing_day", nullable = false)
    private Integer closingDay;

    @Column(name = "due_day", nullable = false)
    private Integer dueDay;

    @Column(name = "default_account_id", nullable = false)
    private UUID defaultAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CreditCardStatus status;

    @NotAudited
    @Version
    @Column(nullable = false)
    private Long version;
}
