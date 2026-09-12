package com.amorim.finance_manager.dashboard.service;

import com.amorim.finance_manager.budget.dto.BudgetResponse;
import com.amorim.finance_manager.budget.model.BudgetAlertStatus;
import com.amorim.finance_manager.budget.service.BudgetService;
import com.amorim.finance_manager.dashboard.model.AccountingBasis;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.networth.model.NetWorthSnapshot;
import com.amorim.finance_manager.networth.service.NetWorthService;
import com.amorim.finance_manager.report.dto.CashFlowSummaryResponse;
import com.amorim.finance_manager.report.dto.CompetenceReportResponse;
import com.amorim.finance_manager.report.dto.MonthlyCashFlowResponse;
import com.amorim.finance_manager.report.service.CashFlowReportService;
import com.amorim.finance_manager.report.service.CompetenceReportService;
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID BUDGET_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    private static final LocalDate START =
            LocalDate.of(2026, 9, 1);

    private static final LocalDate END =
            LocalDate.of(2026, 9, 30);

    @Mock
    private CashFlowReportService cashFlowReportService;

    @Mock
    private CompetenceReportService competenceReportService;

    @Mock
    private BudgetService budgetService;

    @Mock
    private NetWorthService netWorthService;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CurrentUserService currentUserService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-09-10T12:00:00Z"),
                ZoneId.of("America/Sao_Paulo")
        );

        dashboardService = new DashboardService(
                cashFlowReportService,
                competenceReportService,
                budgetService,
                netWorthService,
                invoiceRepository,
                currentUserService,
                clock
        );
    }

    @Test
    void shouldAssembleDashboardUsingCashAndCompetenceRules() {
        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(cashFlowReportService.monthly(2026, 9))
                .thenReturn(new MonthlyCashFlowResponse(
                        2026,
                        9,
                        START,
                        END,
                        new CashFlowSummaryResponse(
                                money("3000.00"),
                                money("800.00"),
                                money("2200.00"),
                                money("500.00"),
                                List.of(),
                                List.of()
                        )
                ));

        when(competenceReportService.generate(START, END))
                .thenReturn(new CompetenceReportResponse(
                        START,
                        END,
                        money("3000.00"),
                        money("1000.00"),
                        money("2000.00"),
                        List.of(),
                        List.of()
                ));

        when(netWorthService.calculateSnapshot())
                .thenReturn(new NetWorthSnapshot(
                        money("5000.00"),
                        money("600.00"),
                        money("4400.00")
                ));

        when(invoiceRepository
                .sumTotalAmountOwnedByUserIdAndStatusIn(
                        USER_ID,
                        Set.of(InvoiceStatus.OPEN)
                ))
                .thenReturn(money("400.00"));

        BudgetResponse budget = new BudgetResponse(
                BUDGET_ID,
                CATEGORY_ID,
                9,
                2026,
                money("1000.00"),
                money("800.00"),
                money("80.00"),
                BudgetAlertStatus.ALERT,
                Instant.parse("2026-09-01T12:00:00Z"),
                Instant.parse("2026-09-10T12:00:00Z")
        );

        when(budgetService.findByPeriod(2026, 9))
                .thenReturn(List.of(budget));

        var response = dashboardService.get();

        assertThat(response.referenceDate())
                .isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(response.periodStart()).isEqualTo(START);
        assertThat(response.periodEnd()).isEqualTo(END);

        assertThat(response.consolidatedBalance().basis())
                .isEqualTo(AccountingBasis.CASH);
        assertThat(response.consolidatedBalance().amount())
                .isEqualByComparingTo("5000.00");

        assertThat(response.monthlyInflows().basis())
                .isEqualTo(AccountingBasis.CASH);
        assertThat(response.monthlyInflows().amount())
                .isEqualByComparingTo("3000.00");

        assertThat(response.cashOutflows().basis())
                .isEqualTo(AccountingBasis.CASH);
        assertThat(response.cashOutflows().amount())
                .isEqualByComparingTo("800.00");

        assertThat(response.competenceExpenses().basis())
                .isEqualTo(AccountingBasis.COMPETENCE);
        assertThat(response.competenceExpenses().amount())
                .isEqualByComparingTo("1000.00");

        assertThat(response.openInvoices().basis())
                .isEqualTo(AccountingBasis.COMPETENCE);
        assertThat(response.openInvoices().amount())
                .isEqualByComparingTo("400.00");

        assertThat(response.netWorth().basis())
                .isEqualTo(AccountingBasis.COMPETENCE);
        assertThat(response.netWorth().amount())
                .isEqualByComparingTo("4400.00");

        assertThat(response.budget().basis())
                .isEqualTo(AccountingBasis.COMPETENCE);
        assertThat(response.budget().totalLimit())
                .isEqualByComparingTo("1000.00");
        assertThat(response.budget().totalSpent())
                .isEqualByComparingTo("800.00");
        assertThat(response.budget().usagePercentage())
                .isEqualByComparingTo("80.00");
        assertThat(response.budget().items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.budgetId()).isEqualTo(BUDGET_ID);
                    assertThat(item.alertStatus())
                            .isEqualTo(BudgetAlertStatus.ALERT);
                });

        verify(cashFlowReportService).monthly(2026, 9);
        verify(competenceReportService).generate(START, END);
        verify(budgetService).findByPeriod(2026, 9);
        verify(netWorthService).calculateSnapshot();
    }

    @Test
    void shouldReturnZeroBudgetSummaryWithoutConfiguredBudgets() {
        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(cashFlowReportService.monthly(2026, 9))
                .thenReturn(new MonthlyCashFlowResponse(
                        2026,
                        9,
                        START,
                        END,
                        new CashFlowSummaryResponse(
                                money("0.00"),
                                money("0.00"),
                                money("0.00"),
                                money("0.00"),
                                List.of(),
                                List.of()
                        )
                ));

        when(competenceReportService.generate(START, END))
                .thenReturn(new CompetenceReportResponse(
                        START,
                        END,
                        money("0.00"),
                        money("0.00"),
                        money("0.00"),
                        List.of(),
                        List.of()
                ));

        when(netWorthService.calculateSnapshot())
                .thenReturn(new NetWorthSnapshot(
                        money("0.00"),
                        money("0.00"),
                        money("0.00")
                ));

        when(invoiceRepository
                .sumTotalAmountOwnedByUserIdAndStatusIn(
                        USER_ID,
                        Set.of(InvoiceStatus.OPEN)
                ))
                .thenReturn(money("0.00"));

        when(budgetService.findByPeriod(2026, 9))
                .thenReturn(List.of());

        var response = dashboardService.get();

        assertThat(response.budget().totalLimit()).isZero();
        assertThat(response.budget().totalSpent()).isZero();
        assertThat(response.budget().usagePercentage()).isZero();
        assertThat(response.budget().items()).isEmpty();
    }

    private BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }
}
