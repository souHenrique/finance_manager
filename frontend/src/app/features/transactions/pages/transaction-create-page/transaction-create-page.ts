import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { Skeleton } from '../../../../shared/ui/skeleton/skeleton';
import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { CategoryApiService } from '../../../categories/data-access/category-api.service';
import { Category } from '../../../categories/models/category.models';
import { TransactionFormComponent } from '../../components/transaction-form/transaction-form';
import {
  CreateTransactionRequest,
  UpdateTransactionRequest,
} from '../../models/transaction.models';
import { TransactionApiService } from '../../data-access/transaction-api.service';

type OptionsState = 'loading' | 'success' | 'error';

@Component({
  selector: 'app-transaction-create-page',
  imports: [ErrorState, Skeleton, TransactionFormComponent],
  templateUrl: './transaction-create-page.html',
  styleUrl: './transaction-create-page.scss',
})
export class TransactionCreatePage implements OnInit {
  private readonly accountApi = inject(AccountApiService);
  private readonly categoryApi = inject(CategoryApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly transactionApi = inject(TransactionApiService);

  readonly accounts = signal<Account[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly optionsState = signal<OptionsState>('loading');
  readonly submitting = signal(false);

  ngOnInit(): void {
    this.loadOptions();
  }

  loadOptions(): void {
    this.optionsState.set('loading');

    forkJoin({
      accounts: this.accountApi.findAll(),
      categories: this.categoryApi.findAll(),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ accounts, categories }) => {
          this.accounts.set(
            accounts
              .filter((account) => account.status === 'ACTIVE')
              .sort((first, second) => first.name.localeCompare(second.name, 'pt-BR')),
          );

          this.categories.set(
            categories
              .filter((category) => category.status === 'ACTIVE')
              .sort((first, second) => first.name.localeCompare(second.name, 'pt-BR')),
          );

          this.optionsState.set('success');
        },
        error: () => {
          this.optionsState.set('error');
        },
      });
  }

  create(payload: CreateTransactionRequest | UpdateTransactionRequest): void {
    if (!this.isCreateTransactionRequest(payload) || this.submitting()) {
      return;
    }

    this.submitting.set(true);

    this.transactionApi
      .create(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (transaction) => {
          this.toast.show({
            tone: 'success',
            title: 'Transação criada',
            message: 'A transação foi cadastrada com sucesso.',
          });

          void this.router.navigate(['/transactions', transaction.id]);
        },
        error: () => {
          this.submitting.set(false);
        },
      });
  }

  goBack(): void {
    void this.router.navigate(['/transactions']);
  }

  private isCreateTransactionRequest(
    payload: CreateTransactionRequest | UpdateTransactionRequest,
  ): payload is CreateTransactionRequest {
    return 'type' in payload && 'status' in payload && 'paymentMethod' in payload;
  }
}
