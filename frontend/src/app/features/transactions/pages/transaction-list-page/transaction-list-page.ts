import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, forkJoin, of } from 'rxjs';

import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { CategoryApiService } from '../../../categories/data-access/category-api.service';
import { Category } from '../../../categories/models/category.models';
import { CreditCardApiService } from '../../../credit-cards/data-access/credit-card-api.service';
import { CreditCard } from '../../../credit-cards/models/credit-card.models';
import { PageResponse } from '../../../../shared/models/pagination';
import { TransactionApiService } from '../../data-access/transaction-api.service';

import { Transaction, TransactionFilters, TransactionQuery } from '../../models/transaction.models';

import { RouterLink } from '@angular/router';

import { Badge } from '../../../../shared/ui/badge/badge';
import { Button } from '../../../../shared/ui/button/button';
import { EmptyState } from '../../../../shared/ui/empty-state/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { Pagination } from '../../../../shared/ui/pagination/pagination';
import { Skeleton } from '../../../../shared/ui/skeleton/skeleton';
import { SelectDirective } from '../../../../shared/ui/form-control/select';
import { TransactionFilterFormComponent } from '../../components/transaction-filter-form/transaction-filter-form';

const TRANSACTION_SORT_OPTIONS = [
  {
    value: 'competenceDate,desc',
    label: 'Competência: mais recente',
  },
  {
    value: 'competenceDate,asc',
    label: 'Competência: mais antiga',
  },
  {
    value: 'amount,desc',
    label: 'Valor: maior primeiro',
  },
  {
    value: 'amount,asc',
    label: 'Valor: menor primeiro',
  },
  {
    value: 'description,asc',
    label: 'Descrição: A–Z',
  },
  {
    value: 'description,desc',
    label: 'Descrição: Z–A',
  },
] as const;

type TransactionSort = (typeof TRANSACTION_SORT_OPTIONS)[number]['value'];

type TransactionListState = 'loading' | 'success' | 'error';

@Component({
  imports: [
    RouterLink,
    Badge,
    Button,
    EmptyState,
    ErrorState,
    Pagination,
    SelectDirective,
    Skeleton,
    TransactionFilterFormComponent,
  ],
  selector: 'app-transaction-list-page',
  styleUrl: './transaction-list-page.scss',
  templateUrl: './transaction-list-page.html',
})
export class TransactionListPage implements OnInit {
  private readonly accountApi = inject(AccountApiService);
  private readonly categoryApi = inject(CategoryApiService);
  private readonly creditCardApi = inject(CreditCardApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly transactionApi = inject(TransactionApiService);

  readonly accounts = signal<Account[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly creditCards = signal<CreditCard[]>([]);

  readonly filters = signal<TransactionFilters>({});
  readonly page = signal(0);
  readonly pageSize = signal(20);
  readonly sort = signal<TransactionSort>('competenceDate,desc');

  readonly result = signal<PageResponse<Transaction> | null>(null);
  readonly listState = signal<TransactionListState>('loading');

  readonly sortOptions = TRANSACTION_SORT_OPTIONS;

  readonly isLoadingFilterOptions = signal(true);
  readonly hasFilterOptionsError = signal(false);

  readonly accountNameById = computed(
    () => new Map(this.accounts().map((account) => [account.id, account.name])),
  );

  readonly categoryNameById = computed(
    () => new Map(this.categories().map((category) => [category.id, category.name])),
  );

  readonly creditCardNameById = computed(
    () => new Map(this.creditCards().map((card) => [card.id, card.name])),
  );

  accountName(id: string | null): string {
    return id ? (this.accountNameById().get(id) ?? 'Conta removida') : '—';
  }

  ngOnInit(): void {
    this.loadFilterOptions();
    this.loadTransactions();
  }

  loadFilterOptions(): void {
    this.isLoadingFilterOptions.set(true);
    this.hasFilterOptionsError.set(false);

    forkJoin({
      accounts: this.accountApi.findAll().pipe(catchError(() => of([]))),
      categories: this.categoryApi.findAll().pipe(catchError(() => of([]))),
      creditCards: this.creditCardApi.findAll().pipe(catchError(() => of([]))),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ accounts, categories, creditCards }) => {
          this.accounts.set(
            [...accounts].sort((first, second) => first.name.localeCompare(second.name, 'pt-BR')),
          );

          this.categories.set(
            [...categories].sort((first, second) => first.name.localeCompare(second.name, 'pt-BR')),
          );

          this.creditCards.set(
            [...creditCards].sort((first, second) =>
              first.name.localeCompare(second.name, 'pt-BR'),
            ),
          );

          this.isLoadingFilterOptions.set(false);
        },
        error: () => {
          this.hasFilterOptionsError.set(true);
          this.isLoadingFilterOptions.set(false);
        },
      });
  }
  categoryLabel(category: Category): string {
    if (!category.parentCategoryId) {
      return category.name;
    }

    const parent = this.categories().find((item) => item.id === category.parentCategoryId);

    return parent ? `${parent.name} — ${category.name}` : category.name;
  }

  private buildQuery(): TransactionQuery {
    return {
      ...this.filters(),
      page: this.page(),
      size: this.pageSize(),
      sort: this.sort(),
    };
  }

  loadTransactions(): void {
    this.listState.set('loading');

    this.transactionApi
      .findAll(this.buildQuery())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          this.result.set(result);
          this.listState.set('success');
        },
        error: () => {
          this.listState.set('error');
        },
      });
  }

  applyFilters(filters: TransactionFilters): void {
    this.filters.set(filters);
    this.page.set(0);
    this.loadTransactions();
  }

  clearFilters(): void {
    this.filters.set({});
    this.page.set(0);
    this.loadTransactions();
  }

  changePage(visiblePage: number): void {
    const apiPage = visiblePage - 1;

    if (apiPage < 0 || apiPage === this.page()) {
      return;
    }

    this.page.set(apiPage);
    this.loadTransactions();
  }

  changeSort(sort: TransactionSort): void {
    if (sort === this.sort()) {
      return;
    }

    this.sort.set(sort);
    this.page.set(0);
    this.loadTransactions();
  }

  categoryName(id: string | null): string {
    return id ? (this.categoryNameById().get(id) ?? 'Categoria removida') : '—';
  }

  creditCardName(id: string | null): string {
    return id ? (this.creditCardNameById().get(id) ?? 'Cartão removido') : '—';
  }

  transactionTypeLabel(type: Transaction['type']): string {
    const labels: Record<Transaction['type'], string> = {
      INCOME: 'Receita',
      EXPENSE: 'Despesa',
      TRANSFER: 'Transferência',
      CREDIT_CARD_PURCHASE: 'Compra no cartão',
      CREDIT_CARD_PAYMENT: 'Pagamento de fatura',
      ADJUSTMENT: 'Ajuste',
    };

    return labels[type];
  }

  transactionStatusLabel(status: Transaction['status']): string {
    const labels: Record<Transaction['status'], string> = {
      PENDING: 'Pendente',
      COMPLETED: 'Concluída',
      CANCELLED: 'Cancelada',
    };

    return labels[status];
  }

  transactionStatusTone(status: Transaction['status']): 'warning' | 'success' | 'neutral' {
    if (status === 'PENDING') {
      return 'warning';
    }

    if (status === 'COMPLETED') {
      return 'success';
    }

    return 'neutral';
  }
}
