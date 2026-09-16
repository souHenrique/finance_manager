import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';

import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../../core/http/api-request-error';
import { Alert } from '../../../../shared/ui/alert/alert';
import { Button } from '../../../../shared/ui/button/button';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { Skeleton } from '../../../../shared/ui/skeleton/skeleton';
import { CreditCardFormComponent } from '../../components/credit-card-form/credit-card-form';
import { CreditCardApiService } from '../../data-access/credit-card-api.service';
import {
  CreditCard,
  UpdateCreditCardRequest,
} from '../../models/credit-card.models';

@Component({
  selector: 'app-credit-card-edit-page',
  imports: [
    DecimalPipe,
    Alert,
    Button,
    CreditCardFormComponent,
    ErrorState,
    Skeleton,
  ],
  templateUrl: './credit-card-edit-page.html',
  styleUrl: './credit-card-edit-page.scss',
})
export class CreditCardEditPage implements OnInit {
  private readonly accountApi = inject(AccountApiService);
  private readonly creditCardApi = inject(CreditCardApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  private readonly creditCardId = this.route.snapshot.paramMap.get('id');

  readonly creditCard = signal<CreditCard | null>(null);
  readonly accounts = signal<Account[]>([]);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly hasLoadError = signal(false);
  readonly hasConflict = signal(false);
  readonly conflictMessage = signal('');

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    if (!this.creditCardId) {
      void this.router.navigate(['/credit-cards']);
      return;
    }

    this.isLoading.set(true);
    this.hasLoadError.set(false);
    this.hasConflict.set(false);
    this.conflictMessage.set('');

    forkJoin({
      creditCard: this.creditCardApi.findById(this.creditCardId),
      accounts: this.accountApi.findAll(),
    })
      .pipe(
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ creditCard, accounts }) => {
          this.creditCard.set(creditCard);
          this.accounts.set(accounts);
        },
        error: () => {
          this.hasLoadError.set(true);
        },
      });
  }

  updateCreditCard(request: UpdateCreditCardRequest): void {
    const currentCreditCard = this.creditCard();

    if (!currentCreditCard || !this.creditCardId || this.isSubmitting()) {
      return;
    }

    this.hasConflict.set(false);
    this.conflictMessage.set('');

    const changes = this.getChanges(currentCreditCard, request);

    if (Object.keys(changes).length === 0) {
      this.toast.show({
        tone: 'info',
        title: 'Nenhuma alteração',
        message: 'Altere pelo menos um campo antes de salvar.',
      });
      return;
    }

    this.isSubmitting.set(true);

    this.creditCardApi
      .update(this.creditCardId, changes)
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (creditCard) => {
          this.creditCard.set(creditCard);

          this.toast.show({
            tone: 'success',
            title: 'Cartão atualizado',
            message: 'As alterações foram salvas com sucesso.',
          });

          void this.router.navigate(['/credit-cards', creditCard.id]);
        },
        error: (error: unknown) => {
          if (error instanceof ApiRequestError && error.status === 409) {
            this.hasConflict.set(true);
            this.conflictMessage.set(error.message);
          }
        },
      });
  }

  reloadAfterConflict(): void {
    this.loadData();
  }

  goBack(): void {
    const creditCard = this.creditCard();

    void this.router.navigate(
      creditCard ? ['/credit-cards', creditCard.id] : ['/credit-cards'],
    );
  }

  private getChanges(
    currentCreditCard: CreditCard,
    request: UpdateCreditCardRequest,
  ): UpdateCreditCardRequest {
    const changes: UpdateCreditCardRequest = {};

    if (request.name !== currentCreditCard.name) {
      changes.name = request.name;
    }

    if (request.creditLimit !== currentCreditCard.creditLimit) {
      changes.creditLimit = request.creditLimit;
    }

    if (request.closingDay !== currentCreditCard.closingDay) {
      changes.closingDay = request.closingDay;
    }

    if (request.dueDay !== currentCreditCard.dueDay) {
      changes.dueDay = request.dueDay;
    }

    if (request.defaultAccountId !== currentCreditCard.defaultAccountId) {
      changes.defaultAccountId = request.defaultAccountId;
    }

    return changes;
  }
}
