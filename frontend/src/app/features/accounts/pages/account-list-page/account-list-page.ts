import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AccountApiService } from '../../data-access/account-api.service';
import { Account } from '../../models/account.models';
import { Router, RouterLink } from '@angular/router';
import { Card } from '../../../../shared/ui/card/card';
import { Badge } from '../../../../shared/ui/badge/badge';
import { Button } from '../../../../shared/ui/button/button';
import { EmptyState } from '../../../../shared/ui/empty-state/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { Skeleton } from '../../../../shared/ui/skeleton/skeleton';
import { ACCOUNT_TYPE_OPTIONS } from '../../models/account-type.options';
import { AccountType } from '../../models/account.models';

type LoadingState = 'loading' | 'success' | 'error';

@Component({
  selector: 'app-account-list-page',
  imports: [RouterLink, Card, Badge, Button, EmptyState, ErrorState, Skeleton],
  styleUrl: './account-list-page.scss',
  templateUrl: './account-list-page.html',
})
export class AccountListPage implements OnInit {
  private readonly accountApi = inject(AccountApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  protected readonly accounts = signal<Account[]>([]);
  protected readonly state = signal<LoadingState>('loading');

  ngOnInit(): void {
    this.loadAccounts();
  }

  protected loadAccounts(): void {
    this.state.set('loading');

    this.accountApi
      .findAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (accounts) => {
          this.accounts.set(accounts);
          this.state.set('success');
        },
        error: () => {
          this.state.set('error');
        },
      });
  }

  protected goToCreate(): void {
    void this.router.navigate(['/accounts/new']);
  }

  protected accountTypeLabel(type: AccountType): string {
    return ACCOUNT_TYPE_OPTIONS.find((option) => option.value === type)?.label ?? type;
  }

  protected accountMonogram(type: AccountType): string {
    const monograms: Record<AccountType, string> = {
      CHECKING: 'CC',
      SAVINGS: 'PO',
      WALLET: 'CT',
      DIGITAL_ACCOUNT: 'CD',
      OTHER: 'OU',
    };

    return monograms[type];
  }

  protected formatCurrency(value: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  }
}
