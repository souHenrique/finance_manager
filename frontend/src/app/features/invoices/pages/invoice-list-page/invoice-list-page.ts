import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Alert } from '../../../../shared/ui/alert/alert';
import { Badge } from '../../../../shared/ui/badge/badge';
import { EmptyState } from '../../../../shared/ui/empty-state/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { Pagination } from '../../../../shared/ui/pagination/pagination';
import { Skeleton } from '../../../../shared/ui/skeleton/skeleton';
import { PageResponse } from '../../../../shared/models/pagination';
import type { FeedbackTone } from '../../../../shared/ui/types/feedback-tone';
import { CreditCardApiService } from '../../../credit-cards/data-access/credit-card-api.service';
import { CreditCard } from '../../../credit-cards/models/credit-card.models';
import { InvoiceFilterFormComponent } from '../../components/invoice-filter-form/invoice-filter-form';
import { InvoiceApiService } from '../../data-access/invoice-api.service';
import {
  InvoiceFilters,
  InvoiceQuery,
  InvoiceStatus,
  InvoiceSummary,
} from '../../models/invoice.models';

type InvoiceListState = 'loading' | 'success' | 'error';

@Component({
  selector: 'app-invoice-list-page',
  imports: [
    DecimalPipe,
    RouterLink,
    Alert,
    Badge,
    EmptyState,
    ErrorState,
    InvoiceFilterFormComponent,
    Pagination,
    Skeleton,
  ],
  templateUrl: './invoice-list-page.html',
  styleUrl: './invoice-list-page.scss',
})
export class InvoiceListPage implements OnInit {
  private readonly creditCardApi = inject(CreditCardApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly invoiceApi = inject(InvoiceApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly creditCards = signal<CreditCard[]>([]);
  readonly filters = signal<InvoiceFilters>({});
  readonly page = signal(0);
  readonly pageSize = signal(20);
  readonly result = signal<PageResponse<InvoiceSummary> | null>(null);
  readonly state = signal<InvoiceListState>('loading');
  readonly hasCreditCardsError = signal(false);

  readonly creditCardNameById = computed(
    () => new Map(this.creditCards().map((creditCard) => [creditCard.id, creditCard.name])),
  );

  ngOnInit(): void {
    const creditCardId = this.route.snapshot.queryParamMap.get('creditCardId');

    if (creditCardId) {
      this.filters.set({ creditCardId });
    }

    this.loadCreditCards();
    this.loadInvoices();
  }

  loadCreditCards(): void {
    this.hasCreditCardsError.set(false);

    this.creditCardApi
      .findAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (creditCards) => {
          this.creditCards.set(
            [...creditCards].sort((first, second) =>
              first.name.localeCompare(second.name, 'pt-BR'),
            ),
          );
        },
        error: () => {
          this.hasCreditCardsError.set(true);
        },
      });
  }

  loadInvoices(): void {
    this.state.set('loading');

    this.invoiceApi
      .findAll(this.buildQuery())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          this.result.set(result);
          this.state.set('success');
        },
        error: () => {
          this.state.set('error');
        },
      });
  }

  applyFilters(filters: InvoiceFilters): void {
    this.filters.set(filters);
    this.page.set(0);
    this.syncCreditCardFilterInUrl(filters.creditCardId);
    this.loadInvoices();
  }

  clearFilters(): void {
    this.filters.set({});
    this.page.set(0);
    this.syncCreditCardFilterInUrl();
    this.loadInvoices();
  }

  changePage(visiblePage: number): void {
    const apiPage = visiblePage - 1;

    if (apiPage < 0 || apiPage === this.page()) {
      return;
    }

    this.page.set(apiPage);
    this.loadInvoices();
  }

  creditCardName(creditCardId: string): string {
    return this.creditCardNameById().get(creditCardId) ?? 'Cartão não encontrado';
  }

  referenceLabel(invoice: InvoiceSummary): string {
    return `${String(invoice.referenceMonth).padStart(2, '0')}/${invoice.referenceYear}`;
  }

  formatDate(value: string): string {
    const [year, month, day] = value.split('-');

    return `${day}/${month}/${year}`;
  }

  statusLabel(status: InvoiceStatus): string {
    const labels: Record<InvoiceStatus, string> = {
      OPEN: 'Aberta',
      CLOSED: 'Fechada',
      PAID: 'Paga',
      CANCELLED: 'Cancelada',
    };

    return labels[status];
  }

  statusTone(status: InvoiceStatus): FeedbackTone {
    const tones: Record<InvoiceStatus, FeedbackTone> = {
      OPEN: 'info',
      CLOSED: 'warning',
      PAID: 'success',
      CANCELLED: 'neutral',
    };

    return tones[status];
  }

  private buildQuery(): InvoiceQuery {
    return {
      ...this.filters(),
      page: this.page(),
      size: this.pageSize(),
      sort: ['referenceYear,desc', 'referenceMonth,desc'],
    };
  }

  private syncCreditCardFilterInUrl(creditCardId?: string): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { creditCardId: creditCardId ?? null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }
}
