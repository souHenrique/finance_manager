package com.amorim.finance_manager.invoice.specification;

import com.amorim.finance_manager.creditcard.entity.CreditCard;
import com.amorim.finance_manager.invoice.dto.InvoiceFilterRequest;
import com.amorim.finance_manager.invoice.entity.Invoice;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class InvoiceSpecifications {

    private InvoiceSpecifications() {
    }

    public static Specification<Invoice> from(
            UUID userId,
            InvoiceFilterRequest filters
    ) {
        return ownedBy(userId)
                .and(equalCreditCard(filters.creditCardId()))
                .and(equalReferenceYear(filters.referenceYear()))
                .and(equalReferenceMonth(filters.referenceMonth()))
                .and(equalStatus(filters.status()));
    }

    private static Specification<Invoice> ownedBy(UUID userId) {
        return (root, query, builder) -> {
            Subquery<UUID> ownedCards = query.subquery(UUID.class);
            Root<CreditCard> card = ownedCards.from(CreditCard.class);

            ownedCards
                    .select(card.get("id"))
                    .where(builder.equal(card.get("userId"), userId));

            return root.get("creditCardId").in(ownedCards);
        };
    }

    private static Specification<Invoice> equalCreditCard(UUID creditCardId) {
        return (root, query, builder) ->
                creditCardId == null
                        ? builder.conjunction()
                        : builder.equal(root.get("creditCardId"), creditCardId);
    }

    private static Specification<Invoice> equalReferenceYear(Integer year) {
        return (root, query, builder) ->
                year == null
                        ? builder.conjunction()
                        : builder.equal(root.get("referenceYear"), year);
    }

    private static Specification<Invoice> equalReferenceMonth(Integer month) {
        return (root, query, builder) ->
                month == null
                        ? builder.conjunction()
                        : builder.equal(root.get("referenceMonth"), month);
    }

    private static Specification<Invoice> equalStatus(InvoiceStatus status) {
        return (root, query, builder) ->
                status == null
                        ? builder.conjunction()
                        : builder.equal(root.get("status"), status);
    }
}
