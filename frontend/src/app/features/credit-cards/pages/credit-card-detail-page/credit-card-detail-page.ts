import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { filter, finalize, forkJoin, switchMap, tap } from 'rxjs';

import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { AppDialogService } from '../../../../core/feedback/dialog/dialog.service';
import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { Badge } from '../../../../shared/ui/badge/badge';
import { Button } from '../../../../shared/ui/button/button';
import { Card } from '../../../../shared/ui/card/card';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { Skeleton } from '../../../../shared/ui/skeleton/skeleton';
import type { FeedbackTone } from '../../../../shared/ui/types/feedback-tone';
import { CreditCardApiService } from '../../data-access/credit-card-api.service';
import {
  CreditCard,
  CreditCardStatus,
} from '../../models/credit-card.models';

interface StatusChangeContent {
  title: string;
  message: string;
  confirmLabel: string;
  successTitle: string;
  successMessage: string;
  danger: boolean;
}

@Component({
  selector: 'app-credit-card-detail-page',
  imports: [
    DecimalPipe,
    Badge,
    Button,
    Card,
    ErrorState,
    Skeleton,
  ],
  templateUrl: './credit-card-detail-page.html',
  styleUrl: './credit-card-detail-page.scss',
})
export class CreditCardDetailPage implements OnInit {
  private readonly accountApi = inject(AccountApiService);
  private readonly creditCardApi = inject(CreditCardApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(AppDialogService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  private readonly creditCardId = this.route.snapshot.paramMap.get('id');

  readonly creditCard = signal<CreditCard | null>(null);
  readonly accounts = signal<Account[]>([]);
  readonly isLoading = signal(true);
  readonly hasLoadError = signal(false);
  readonly changingStatus = signal<CreditCardStatus | null>(null);

  readonly isChangingStatus = computed(() => this.changingStatus() !== null);

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

  goToEdit(): void {
    const creditCard = this.creditCard();

    if (creditCard) {
      void this.router.navigate(['/credit-cards', creditCard.id, 'edit']);
    }
  }

  goBack(): void {
    void this.router.navigate(['/credit-cards']);
  }

  confirmStatusChange(targetStatus: CreditCardStatus): void {
    const creditCard = this.creditCard();

    if (
      !creditCard ||
      creditCard.status === targetStatus ||
      this.isChangingStatus()
    ) {
      return;
    }

    const content = this.statusChangeContent(targetStatus);

    this.dialog
      .confirm({
        title: content.title,
        message: content.message,
        confirmLabel: content.confirmLabel,
        cancelLabel: 'Cancelar',
        danger: content.danger,
      })
      .pipe(
        filter((confirmed): confirmed is true => confirmed === true),
        tap(() => this.changingStatus.set(targetStatus)),
        switchMap(() =>
          this.creditCardApi
            .update(creditCard.id, { status: targetStatus })
            .pipe(finalize(() => this.changingStatus.set(null))),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.toast.show({
            tone: 'success',
            title: content.successTitle,
            message: content.successMessage,
          });

          this.loadData();
        },
        error: () => undefined,
      });
  }

  accountName(accountId: string): string {
    return this.accounts().find((account) => account.id === accountId)?.name ?? 'Conta não encontrada';
  }

  committedLimit(creditCard: CreditCard): number {
    return Math.max(0, creditCard.creditLimit - creditCard.availableLimit);
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

  private statusChangeContent(targetStatus: CreditCardStatus): StatusChangeContent {
    switch (targetStatus) {
      case 'ACTIVE':
        return {
          title: 'Ativar cartão?',
          message:
            'O cartão voltará a estar disponível para novas compras. O histórico financeiro será preservado.',
          confirmLabel: 'Ativar cartão',
          successTitle: 'Cartão ativado',
          successMessage: 'O cartão está disponível novamente para novas compras.',
          danger: false,
        };

      case 'BLOCKED':
        return {
          title: 'Bloquear cartão?',
          message:
            'O cartão ficará indisponível para novas compras. O histórico e as faturas existentes serão preservados.',
          confirmLabel: 'Bloquear cartão',
          successTitle: 'Cartão bloqueado',
          successMessage: 'O cartão não poderá receber novas compras enquanto estiver bloqueado.',
          danger: false,
        };

      case 'INACTIVE':
        return {
          title: 'Inativar cartão?',
          message:
            'O cartão ficará indisponível para novas compras. O histórico e as faturas existentes serão preservados.',
          confirmLabel: 'Inativar cartão',
          successTitle: 'Cartão inativado',
          successMessage: 'O cartão foi inativado com sucesso.',
          danger: true,
        };
    }
  }

  goToNewPurchase(): void {
    const creditCard = this.creditCard();

    if (creditCard?.status === 'ACTIVE') {
      void this.router.navigate([
        '/credit-cards',
        creditCard.id,
        'purchases',
        'new',
      ]);
    }
  }
}
