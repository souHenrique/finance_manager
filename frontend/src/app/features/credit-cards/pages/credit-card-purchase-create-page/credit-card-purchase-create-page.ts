import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';

import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../../core/http/api-request-error';
import { Alert } from '../../../../shared/ui/alert/alert';
import { Button } from '../../../../shared/ui/button/button';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { Skeleton } from '../../../../shared/ui/skeleton/skeleton';
import { CategoryApiService } from '../../../categories/data-access/category-api.service';
import { Category } from '../../../categories/models/category.models';
import { CreditCardPurchaseFormComponent } from '../../components/credit-card-purchase-form/credit-card-purchase-form';
import { CreditCardApiService } from '../../data-access/credit-card-api.service';
import { CreateCreditCardPurchaseRequest, CreditCard } from '../../models/credit-card.models';

type PurchaseDataState = 'loading' | 'success' | 'error';

@Component({
  selector: 'app-credit-card-purchase-create-page',
  imports: [Alert, Button, CreditCardPurchaseFormComponent, ErrorState, Skeleton],
  templateUrl: './credit-card-purchase-create-page.html',
  styleUrl: './credit-card-purchase-create-page.scss',
})
export class CreditCardPurchaseCreatePage implements OnInit {
  private readonly categoryApi = inject(CategoryApiService);
  private readonly creditCardApi = inject(CreditCardApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  private readonly creditCardId = this.route.snapshot.paramMap.get('id');

  readonly creditCard = signal<CreditCard | null>(null);
  readonly categories = signal<Category[]>([]);
  readonly state = signal<PurchaseDataState>('loading');
  readonly isSubmitting = signal(false);
  readonly conflictMessage = signal('');

  readonly hasActiveExpenseCategories = computed(() =>
    this.categories().some(
      (category) => category.type === 'EXPENSE' && category.status === 'ACTIVE',
    ),
  );

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    if (!this.creditCardId) {
      void this.router.navigate(['/credit-cards']);
      return;
    }

    this.state.set('loading');
    this.conflictMessage.set('');

    forkJoin({
      creditCard: this.creditCardApi.findById(this.creditCardId),
      categories: this.categoryApi.findAll(),
    })
      .pipe(
        finalize(() => {
          if (this.state() === 'loading') {
            this.state.set('success');
          }
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ creditCard, categories }) => {
          this.creditCard.set(creditCard);
          this.categories.set(categories);
          this.state.set('success');
        },
        error: () => {
          this.state.set('error');
        },
      });
  }

  createPurchase(request: CreateCreditCardPurchaseRequest): void {
    const creditCard = this.creditCard();

    if (!creditCard || creditCard.status !== 'ACTIVE' || this.isSubmitting()) {
      return;
    }

    this.conflictMessage.set('');
    this.isSubmitting.set(true);

    this.creditCardApi
      .createPurchase(creditCard.id, request)
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (transactions) => {
          const installmentMessage =
            transactions.length === 1
              ? 'A compra foi adicionada à fatura do cartão.'
              : `${transactions.length} parcelas foram adicionadas às faturas correspondentes.`;

          this.toast.show({
            tone: 'success',
            title: 'Compra registrada',
            message: installmentMessage,
          });

          void this.router.navigate(['/credit-cards', creditCard.id]);
        },
        error: (error: unknown) => {
          if (error instanceof ApiRequestError && error.status === 409) {
            this.conflictMessage.set(error.message);
          }
        },
      });
  }

  goBack(): void {
    const creditCard = this.creditCard();

    void this.router.navigate(creditCard ? ['/credit-cards', creditCard.id] : ['/credit-cards']);
  }

  goToCategories(): void {
    void this.router.navigate(['/categories']);
  }
}
