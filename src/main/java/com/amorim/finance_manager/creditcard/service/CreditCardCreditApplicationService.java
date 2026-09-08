package com.amorim.finance_manager.creditcard.service;

import com.amorim.finance_manager.creditcard.entity.CreditCardCredit;
import com.amorim.finance_manager.creditcard.entity.CreditCardCreditApplication;
import com.amorim.finance_manager.creditcard.repository.CreditCardCreditApplicationRepository;
import com.amorim.finance_manager.creditcard.repository.CreditCardCreditRepository;
import com.amorim.finance_manager.invoice.entity.Invoice;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditCardCreditApplicationService {

    private final CreditCardCreditRepository creditRepository;
    private final CreditCardCreditApplicationRepository applicationRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public BigDecimal apply(UUID userId, Invoice invoice, BigDecimal amountDue) {
        if (amountDue == null || amountDue.signum() < 0) {
            throw new IllegalArgumentException("O valor devido não pode ser negativo");
        }

        BigDecimal remainingDue = amountDue;

        while (remainingDue.signum() > 0) {
            List<CreditCardCredit> credits =
                    creditRepository.findEligibleCredits(
                            userId,
                            invoice.getCreditCardId(),
                            invoice.getReferenceYear(),
                            invoice.getReferenceMonth(),
                            PageRequest.of(0, 100)
                    );

            if (credits.isEmpty()) {
                break;
            }

            List<CreditCardCredit> changedCredits = new ArrayList<>();
            List<CreditCardCreditApplication> applications = new ArrayList<>();

            for (CreditCardCredit credit : credits) {
                if (remainingDue.signum() == 0) {
                    break;
                }

                BigDecimal appliedAmount = credit.getRemainingAmount().min(remainingDue);

                credit.setRemainingAmount(credit.getRemainingAmount().subtract(appliedAmount));

                CreditCardCreditApplication application = new CreditCardCreditApplication();

                application.setCreditId(credit.getId());
                application.setInvoiceId(invoice.getId());
                application.setAmount(appliedAmount);

                changedCredits.add(credit);
                applications.add(application);

                remainingDue = remainingDue.subtract(appliedAmount);
            }
            creditRepository.saveAllAndFlush(changedCredits);
            applicationRepository.saveAllAndFlush(applications);
        }
        return amountDue.subtract(remainingDue);
    }
}
