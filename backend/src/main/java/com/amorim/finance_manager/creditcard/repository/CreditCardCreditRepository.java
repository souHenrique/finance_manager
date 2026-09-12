package com.amorim.finance_manager.creditcard.repository;

import com.amorim.finance_manager.creditcard.entity.CreditCardCredit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CreditCardCreditRepository extends JpaRepository<CreditCardCredit, UUID> {

    @Query("""
        SELECT credit
        FROM CreditCardCredit credit,
             CreditCardRefundItem item,
             Invoice originalInvoice
        WHERE credit.refundItemId = item.id
          AND item.originalInvoiceId = originalInvoice.id
          AND credit.userId = :userId
          AND credit.creditCardId = :creditCardId
          AND originalInvoice.creditCardId = :creditCardId
          AND credit.remainingAmount > 0
          AND (
              originalInvoice.referenceYear < :referenceYear
              OR (
                  originalInvoice.referenceYear = :referenceYear
                  AND originalInvoice.referenceMonth < :referenceMonth
              )
          )
        ORDER BY credit.createdAt ASC, credit.id ASC
        """)
    List<CreditCardCredit> findEligibleCredits(
            @Param("userId") UUID userId,
            @Param("creditCardId") UUID creditCardId,
            @Param("referenceYear") Integer referenceYear,
            @Param("referenceMonth") Integer referenceMonth,
            Pageable pageable
    );
}
