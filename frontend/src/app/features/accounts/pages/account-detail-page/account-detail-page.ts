import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { filter, finalize, switchMap, tap } from 'rxjs';

import { AppDialogService } from '../../../../core/feedback/dialog/dialog.service';
import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { Badge } from '../../../../shared/ui/badge/badge';
import { Button } from '../../../../shared/ui/button/button';
import { Card } from '../../../../shared/ui/card/card';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { Skeleton } from '../../../../shared/ui/skeleton/skeleton';
import { AccountApiService } from '../../data-access/account-api.service';
import { Account, AccountStatus, AccountType } from '../../models/account.models';
import { ACCOUNT_TYPE_OPTIONS } from '../../models/account-type.options';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-account-detail-page',
  imports: [
    DecimalPipe,
    Badge,
    Button,
    Card,
    ErrorState,
    Skeleton,
  ],
  templateUrl: './account-detail-page.html',
  styleUrl: './account-detail-page.scss',
})
export class AccountDetailPage implements OnInit {
  private readonly accountApi = inject(AccountApiService);
  private readonly dialog = inject(AppDialogService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  private readonly accountId = this.route.snapshot.paramMap.get('id');

  readonly account = signal<Account | null>(null);
  readonly isLoading = signal(true);
  readonly hasLoadError = signal(false);
  readonly isChangingStatus = signal(false);

  ngOnInit(): void {
    this.loadAccount();
  }

  loadAccount(): void {
    if (!this.accountId) {
      void this.router.navigate(['/accounts']);
      return;
    }

    this.isLoading.set(true);
    this.hasLoadError.set(false);

    this.accountApi
      .findById(this.accountId)
      .pipe(
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (account) => this.account.set(account),
        error: () => this.hasLoadError.set(true),
      });
  }

  goToEdit(): void {
    const account = this.account();

    if (account) {
      void this.router.navigate(['/accounts', account.id, 'edit']);
    }
  }

  confirmStatusChange(): void {
    const account = this.account();

    if (!account) {
      return;
    }

    const targetStatus: AccountStatus =
      account.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';

    const isDeactivation = targetStatus === 'INACTIVE';

    this.dialog
      .confirm({
        title: isDeactivation ? 'Inativar conta?' : 'Ativar conta?',
        message: isDeactivation
          ? 'A conta deixará de estar disponível para novos lançamentos. Os dados financeiros serão preservados.'
          : 'A conta voltará a ficar disponível para novos lançamentos.',
        confirmLabel: isDeactivation ? 'Inativar conta' : 'Ativar conta',
        cancelLabel: 'Cancelar',
        danger: isDeactivation,
      })
      .pipe(
        filter((confirmed): confirmed is true => confirmed === true),
        tap(() => this.isChangingStatus.set(true)),
        switchMap(() =>
          this.accountApi
            .updateStatus(account.id, { status: targetStatus })
            .pipe(finalize(() => this.isChangingStatus.set(false))),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.toast.show({
            tone: 'success',
            title: targetStatus === 'ACTIVE' ? 'Conta ativada' : 'Conta inativada',
            message:
              targetStatus === 'ACTIVE'
                ? 'A conta está disponível novamente.'
                : 'A conta foi inativada com sucesso.',
          });

          this.loadAccount();
        },
      });
  }

  accountTypeLabel(type: AccountType): string {
    return (
      ACCOUNT_TYPE_OPTIONS.find((option) => option.value === type)?.label ?? type
    );
  }

  statusLabel(status: AccountStatus): string {
    return status === 'ACTIVE' ? 'Ativa' : 'Inativa';
  }

  statusTone(status: AccountStatus): 'success' | 'neutral' {
    return status === 'ACTIVE' ? 'success' : 'neutral';
  }
}
