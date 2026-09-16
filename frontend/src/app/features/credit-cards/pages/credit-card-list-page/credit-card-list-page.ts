import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { Badge } from '../../../../shared/ui/badge/badge';
import { Button } from '../../../../shared/ui/button/button';
import { Card } from '../../../../shared/ui/card/card';
import { EmptyState } from '../../../../shared/ui/empty-state/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { Skeleton } from '../../../../shared/ui/skeleton/skeleton';
import type { FeedbackTone } from '../../../../shared/ui/types/feedback-tone';
import { CreditCardApiService } from '../../data-access/credit-card-api.service';
import { CreditCard, CreditCardStatus } from '../../models/credit-card.models';

type CreditCardListState = 'loading' | 'success' | 'error';

@Component({
  selector: 'app-credit-card-list-page',
  imports: [DecimalPipe, RouterLink, Badge, Button, Card, EmptyState, ErrorState, Skeleton],
  templateUrl: './credit-card-list-page.html',
  styleUrl: './credit-card-list-page.scss',
})
export class CreditCardListPage implements OnInit {
  private readonly accountApi = inject(AccountApiService);
  private readonly creditCardApi = inject(CreditCardApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly creditCards = signal<CreditCard[]>([]);
  readonly accounts = signal<Account[]>([]);
  readonly state = signal<CreditCardListState>('loading');

  ngOnInit(): void {
    this.loadCreditCards();
  }

  loadCreditCards(): void {
    this.state.set('loading');

    forkJoin({
      creditCards: this.creditCardApi.findAll(),
      accounts: this.accountApi.findAll(),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ creditCards, accounts }) => {
          this.creditCards.set(creditCards);
          this.accounts.set(accounts);
          this.state.set('success');
        },
        error: () => {
          this.state.set('error');
        },
      });
  }

  accountName(accountId: string): string {
    return (
      this.accounts().find((account) => account.id === accountId)?.name ?? 'Conta não encontrada'
    );
  }

  statusLabel(status: CreditCardStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'Ativo';
      case 'INACTIVE':
        return 'Inativo';
      case 'BLOCKED':
        return 'Bloqueado';
    }
  }

  statusTone(status: CreditCardStatus): FeedbackTone {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'INACTIVE':
        return 'neutral';
      case 'BLOCKED':
        return 'warning';
    }
  }
}
