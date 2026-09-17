import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, forkJoin, of } from 'rxjs';

import { Badge } from '../../../shared/ui/badge/badge';
import { Button } from '../../../shared/ui/button/button';
import { Card } from '../../../shared/ui/card/card';
import { ErrorState } from '../../../shared/ui/error-state/error-state';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import type { FeedbackTone } from '../../../shared/ui/types/feedback-tone';
import { BudgetAlertStatus } from '../../budgets/models/budget.models';
import { CategoryApiService } from '../../categories/data-access/category-api.service';
import { Category } from '../../categories/models/category.models';
import { DashboardApiService } from '../data-access/dashboard-api.service';
import {
  AccountingBasis,
  Dashboard,
  DashboardBudgetItem,
  DashboardIndicator,
} from '../models/dashboard.models';

type DashboardState = 'loading' | 'success' | 'error';

interface IndicatorCard {
  title: string;
  description: string;
  indicator: DashboardIndicator;
}

const MONTHS = [
  'Janeiro',
  'Fevereiro',
  'Março',
  'Abril',
  'Maio',
  'Junho',
  'Julho',
  'Agosto',
  'Setembro',
  'Outubro',
  'Novembro',
  'Dezembro',
] as const;

@Component({
  selector: 'app-dashboard-page',
  imports: [Badge, Button, Card, CurrencyPipe, DecimalPipe, ErrorState, Skeleton],
  styleUrl: './dashboard-page.scss',
  templateUrl: './dashboard-page.html',
})
export class DashboardPage implements OnInit {
  private readonly dashboardApi = inject(DashboardApiService);
  private readonly categoryApi = inject(CategoryApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly state = signal<DashboardState>('loading');
  readonly dashboard = signal<Dashboard | null>(null);
  readonly categories = signal<Category[]>([]);

  readonly indicatorCards = computed<IndicatorCard[]>(() => {
    const dashboard = this.dashboard();

    if (!dashboard) {
      return [];
    }

    return [
      {
        title: 'Saldo',
        description: 'Entradas menos todas as saídas do mês, incluindo a fatura de referência.',
        indicator: dashboard.monthlyBalance,
      },
      {
        title: 'Entradas mensais',
        description: 'Total acumulado de entradas efetivadas no mês de referência.',
        indicator: dashboard.monthlyInflows,
      },
      {
        title: 'Saídas mensais',
        description: 'Saídas de caixa efetivadas no mês de referência.',
        indicator: dashboard.monthlyOutflows,
      },
      {
        title: 'Saídas totais',
        description: 'Todas as saídas de caixa efetivadas no histórico.',
        indicator: dashboard.totalOutflows,
      },
      {
        title: 'Saídas de compras no crédito',
        description: 'Compras e parcelas vinculadas à fatura do mês de referência.',
        indicator: dashboard.creditCardPurchaseOutflows,
      },
      {
        title: 'Despesas por competência',
        description: 'Despesas reconhecidas no período.',
        indicator: dashboard.competenceExpenses,
      },
      {
        title: 'Faturas abertas',
        description: 'Total atual das faturas em aberto.',
        indicator: dashboard.openInvoices,
      },
      {
        title: 'Saldo consolidado',
        description: 'Soma dos saldos atuais das contas.',
        indicator: dashboard.consolidatedBalance,
      },
    ];
  });

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.state.set('loading');

    forkJoin({
      dashboard: this.dashboardApi.get(),
      categories: this.categoryApi.findAll().pipe(catchError(() => of([]))),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ dashboard, categories }) => {
          this.dashboard.set(dashboard);
          this.categories.set(categories);
          this.state.set('success');
        },
        error: () => this.state.set('error'),
      });
  }

  periodLabel(): string {
    const dashboard = this.dashboard();

    if (!dashboard) {
      return '';
    }

    return `${MONTHS[dashboard.month - 1]} de ${dashboard.year}`;
  }

  basisDescription(basis: AccountingBasis): string {
    if (basis === 'CASH') {
      return 'CASH · Regime de caixa';
    }

    if (basis === 'COMPETENCE') {
      return 'COMPETENCE · Regime de competência';
    }

    return 'CASH + FATURA · Movimentos efetivos e fatura do mês';
  }

  basisLabel(basis: AccountingBasis): string {
    return basis === 'CASH_AND_INVOICE' ? 'CASH + FATURA' : basis;
  }

  categoryName(categoryId: string): string {
    return (
      this.categories().find((category) => category.id === categoryId)?.name ??
      'Categoria indisponível'
    );
  }

  alertLabel(item: DashboardBudgetItem): string {
    if (item.alertStatus === 'NORMAL') {
      return 'Consumo normal';
    }

    if (item.alertStatus === 'ALERT') {
      return 'Alerta de consumo';
    }

    return item.usagePercentage > 100 ? 'Limite excedido' : 'Limite atingido';
  }

  alertMessage(item: DashboardBudgetItem): string {
    const percentage = this.formatPercentage(item.usagePercentage);

    if (item.alertStatus === 'NORMAL') {
      return `Consumo normal: ${percentage} do limite utilizado.`;
    }

    if (item.alertStatus === 'ALERT') {
      return `Alerta: ${percentage} do limite utilizado. Acompanhe os próximos gastos.`;
    }

    return item.usagePercentage > 100
      ? `Limite excedido: ${percentage} do limite utilizado.`
      : `Limite atingido: ${percentage} do limite utilizado.`;
  }

  alertTone(status: BudgetAlertStatus): FeedbackTone {
    const tones: Record<BudgetAlertStatus, FeedbackTone> = {
      NORMAL: 'success',
      ALERT: 'warning',
      LIMIT_REACHED: 'danger',
    };

    return tones[status];
  }

  progressValue(percentage: number): number {
    return Math.max(0, Math.min(percentage, 100));
  }

  private formatPercentage(value: number): string {
    return `${value.toLocaleString('pt-BR', { maximumFractionDigits: 2 })}%`;
  }
}
