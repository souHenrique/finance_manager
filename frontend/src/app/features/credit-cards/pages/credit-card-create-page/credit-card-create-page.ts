import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { Alert } from '../../../../shared/ui/alert/alert';
import { Button } from '../../../../shared/ui/button/button';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { Skeleton } from '../../../../shared/ui/skeleton/skeleton';
import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { CreditCardFormComponent } from '../../components/credit-card-form/credit-card-form';
import { CreditCardApiService } from '../../data-access/credit-card-api.service';
import { CreateCreditCardRequest } from '../../models/credit-card.models';

type AccountLoadState = 'loading' | 'success' | 'error';

@Component({
  selector: 'app-credit-card-create-page',
  imports: [RouterLink, Alert, Button, CreditCardFormComponent, ErrorState, Skeleton],
  templateUrl: './credit-card-create-page.html',
  styleUrl: './credit-card-create-page.scss',
})
export class CreditCardCreatePage implements OnInit {
  private readonly accountApi = inject(AccountApiService);
  private readonly creditCardApi = inject(CreditCardApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly accounts = signal<Account[]>([]);
  readonly accountLoadState = signal<AccountLoadState>('loading');
  readonly isSubmitting = signal(false);

  readonly hasActiveAccounts = computed(() =>
    this.accounts().some((account) => account.status === 'ACTIVE'),
  );

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.accountLoadState.set('loading');

    this.accountApi
      .findAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (accounts) => {
          this.accounts.set(accounts);
          this.accountLoadState.set('success');
        },
        error: () => {
          this.accountLoadState.set('error');
        },
      });
  }

  createCreditCard(request: CreateCreditCardRequest): void {
    if (this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);

    this.creditCardApi
      .create(request)
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (creditCard) => {
          this.toast.show({
            tone: 'success',
            title: 'Cartão criado',
            message: 'O cartão de crédito foi criado com sucesso.',
          });

          void this.router.navigate(['/credit-cards', creditCard.id]);
        },
        error: () => undefined,
      });
  }

  goBack(): void {
    void this.router.navigate(['/credit-cards']);
  }
}
