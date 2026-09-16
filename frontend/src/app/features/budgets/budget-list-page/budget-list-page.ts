import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, forkJoin, switchMap, take } from 'rxjs';

import { AppDialogService } from '../../../core/feedback/dialog/dialog.service';
import { ToastService } from '../../../core/feedback/toast/toast.service';
import { Badge } from '../../../shared/ui/badge/badge';
import { Button } from '../../../shared/ui/button/button';
import { Card } from '../../../shared/ui/card/card';
import { EmptyState } from '../../../shared/ui/empty-state/empty-state';
import { ErrorState } from '../../../shared/ui/error-state/error-state';
import { InputDirective } from '../../../shared/ui/form-control/input';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import type { FeedbackTone } from '../../../shared/ui/types/feedback-tone';
import { CategoryApiService } from '../../categories/data-access/category-api.service';
import { Category } from '../../categories/models/category.models';
import { BudgetFormComponent, BudgetFormMode } from '../components/budget-form/budget-form';
import { BudgetApiService } from '../data-access/budget-api.service';
import {
  Budget,
  BudgetAlertStatus,
  CreateBudgetRequest,
  UpdateBudgetRequest,
} from '../models/budget.models';

type BudgetListState = 'loading' | 'success' | 'error';

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
  selector: 'app-budget-list-page',
  imports: [
    DecimalPipe,
    Badge,
    BudgetFormComponent,
    Button,
    Card,
    EmptyState,
    ErrorState,
    InputDirective,
    Skeleton,
  ],
  styleUrl: './budget-list-page.scss',
  templateUrl: './budget-list-page.html',
})
export class BudgetListPage implements OnInit {
  private readonly budgetApi = inject(BudgetApiService);
  private readonly categoryApi = inject(CategoryApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(AppDialogService);
  private readonly toast = inject(ToastService);

  private readonly currentDate = new Date();

  readonly budgets = signal<Budget[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly state = signal<BudgetListState>('loading');
  readonly selectedMonth = signal(this.currentDate.getMonth() + 1);
  readonly selectedYear = signal(this.currentDate.getFullYear());
  readonly formMode = signal<BudgetFormMode | null>(null);
  readonly selectedBudget = signal<Budget | null>(null);
  readonly isSubmitting = signal(false);
  readonly deletingBudgetId = signal<string | null>(null);

  readonly expenseCategories = computed(() =>
    this.categories()
      .filter((category) => category.type === 'EXPENSE')
      .sort((first, second) => first.name.localeCompare(second.name, 'pt-BR')),
  );

  readonly visibleBudgets = computed(() =>
    this.budgets()
      .filter(
        (budget) => budget.month === this.selectedMonth() && budget.year === this.selectedYear(),
      )
      .sort((first, second) =>
        this.categoryName(first.categoryId).localeCompare(
          this.categoryName(second.categoryId),
          'pt-BR',
        ),
      ),
  );

  readonly periodValue = computed(
    () => `${this.selectedYear()}-${String(this.selectedMonth()).padStart(2, '0')}`,
  );

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.state.set('loading');

    forkJoin({
      budgets: this.budgetApi.findAll(),
      categories: this.categoryApi.findAll(),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ budgets, categories }) => {
          this.budgets.set(budgets);
          this.categories.set(categories);
          this.state.set('success');
        },
        error: () => this.state.set('error'),
      });
  }

  selectPeriod(value: string): void {
    const match = /^(\d{4})-(\d{2})$/.exec(value);

    if (!match) {
      return;
    }

    const year = Number(match[1]);
    const month = Number(match[2]);

    if (month < 1 || month > 12 || year < 1 || year > 9999) {
      return;
    }

    this.selectedMonth.set(month);
    this.selectedYear.set(year);
    this.closeForm();
  }

  startCreate(): void {
    this.selectedBudget.set(null);
    this.formMode.set('create');
  }

  startEdit(budget: Budget): void {
    this.selectedBudget.set(budget);
    this.formMode.set('edit');
  }

  createBudget(request: CreateBudgetRequest): void {
    if (this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);

    this.budgetApi
      .create(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.selectedMonth.set(request.month);
          this.selectedYear.set(request.year);
          this.closeForm();
          this.toast.show({
            tone: 'success',
            title: 'Orçamento criado',
            message: 'O limite mensal da categoria foi salvo.',
          });
          this.loadData();
        },
        error: () => this.isSubmitting.set(false),
      });
  }

  updateBudget(request: UpdateBudgetRequest): void {
    const budget = this.selectedBudget();

    if (!budget || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);

    this.budgetApi
      .update(budget.id, request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedBudget) => {
          this.isSubmitting.set(false);
          this.selectedMonth.set(updatedBudget.month);
          this.selectedYear.set(updatedBudget.year);
          this.closeForm();
          this.toast.show({
            tone: 'success',
            title: 'Orçamento atualizado',
            message: 'As alterações do limite mensal foram salvas.',
          });
          this.loadData();
        },
        error: () => this.isSubmitting.set(false),
      });
  }

  confirmDelete(budget: Budget): void {
    if (this.deletingBudgetId()) {
      return;
    }

    this.deletingBudgetId.set(budget.id);

    this.dialog
      .confirm({
        title: 'Excluir orçamento?',
        message: `O orçamento de ${this.categoryName(budget.categoryId)} para ${this.periodLabel(budget.month, budget.year)} será excluído permanentemente. Esta ação não pode ser desfeita.`,
        confirmLabel: 'Excluir orçamento',
        cancelLabel: 'Cancelar',
        danger: true,
      })
      .pipe(
        take(1),
        switchMap((confirmed) => (confirmed ? this.budgetApi.delete(budget.id) : EMPTY)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.deletingBudgetId.set(null);
          this.toast.show({
            tone: 'success',
            title: 'Orçamento excluído',
            message: 'O orçamento foi removido permanentemente.',
          });
          this.loadData();
        },
        error: () => this.deletingBudgetId.set(null),
        complete: () => {
          if (this.deletingBudgetId() === budget.id) {
            this.deletingBudgetId.set(null);
          }
        },
      });
  }

  closeForm(): void {
    this.formMode.set(null);
    this.selectedBudget.set(null);
  }

  categoryName(categoryId: string): string {
    return (
      this.categories().find((category) => category.id === categoryId)?.name ?? 'Categoria removida'
    );
  }

  periodLabel(month = this.selectedMonth(), year = this.selectedYear()): string {
    return `${MONTHS[month - 1]} de ${year}`;
  }

  alertLabel(budget: Budget): string {
    if (budget.alertStatus === 'NORMAL') {
      return 'Consumo normal';
    }

    if (budget.alertStatus === 'ALERT') {
      return 'Alerta de consumo';
    }

    return budget.usagePercentage > 100 ? 'Limite excedido' : 'Limite atingido';
  }

  alertMessage(budget: Budget): string {
    const percentage = this.formatPercentage(budget.usagePercentage);

    if (budget.alertStatus === 'NORMAL') {
      return `Consumo normal: ${percentage} do limite utilizado.`;
    }

    if (budget.alertStatus === 'ALERT') {
      return `Alerta: ${percentage} do limite utilizado. Acompanhe os próximos gastos.`;
    }

    return budget.usagePercentage > 100
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

  progressValue(budget: Budget): number {
    return Math.max(0, Math.min(budget.usagePercentage, 100));
  }

  isDeleting(budget: Budget): boolean {
    return this.deletingBudgetId() === budget.id;
  }

  private formatPercentage(value: number): string {
    return `${value.toLocaleString('pt-BR', { maximumFractionDigits: 2 })}%`;
  }
}
