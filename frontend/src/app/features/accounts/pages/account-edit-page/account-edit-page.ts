import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AccountApiService } from '../../data-access/account-api.service';
import { Account, UpdateAccountRequest } from '../../models/account.models';
import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../../core/http/api-request-error';
import { Alert } from '../../../../shared/ui/alert/alert';
import { Button } from '../../../../shared/ui/button/button';
import { AccountFormComponent } from '../../components/account-form/account-form';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { Skeleton } from '../../../../shared/ui/skeleton/skeleton';

@Component({
  selector: 'app-account-edit-page',
  imports: [AccountFormComponent, Alert, Button, ErrorState, Skeleton],
  templateUrl: './account-edit-page.html',
  styleUrl: './account-edit-page.scss',
})
export class AccountEditPageComponent implements OnInit {
  private readonly accountApi = inject(AccountApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly account = signal<Account | null>(null);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly hasLoadError = signal(false);
  readonly hasConflict = signal(false);

  private readonly accountId = this.route.snapshot.paramMap.get('id');

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
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (account) => this.account.set(account),
        error: () => this.hasLoadError.set(true),
      });
  }

  updateAccount(request: UpdateAccountRequest): void {
    const currentAccount = this.account();

    if (!currentAccount || !this.accountId) {
      return;
    }

    this.hasConflict.set(false);

    const changes = this.getChanges(currentAccount, request);

    if (Object.keys(changes).length === 0) {
      this.toast.show({
        tone: 'info',
        title: 'Nenhuma alteração',
        message: 'Altere pelo menos um campo antes de salvar.',
      });
      return;
    }

    this.isSubmitting.set(true);

    this.accountApi
      .update(this.accountId, changes)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (account) => {
          this.account.set(account);

          this.toast.show({
            tone: 'success',
            title: 'Conta atualizada',
            message: 'As alterações foram salvas com sucesso.',
          });

          void this.router.navigate(['/accounts', account.id]);
        },
        error: (error: unknown) => {
          if (error instanceof ApiRequestError && error.status === 409) {
            this.hasConflict.set(true);
          }
        },
      });
  }

  reloadAfterConflict(): void {
    this.hasConflict.set(false);
    this.loadAccount();
  }

  goBack(): void {
    const account = this.account();

    void this.router.navigate(account ? ['/accounts', account.id] : ['/accounts']);
  }

  private getChanges(current: Account, request: UpdateAccountRequest): UpdateAccountRequest {
    const changes: UpdateAccountRequest = {};

    if (request.name !== current.name) {
      changes.name = request.name;
    }

    if (request.type !== current.type) {
      changes.type = request.type;
    }

    if (request.institution !== (current.institution ?? '')) {
      changes.institution = request.institution;
    }

    return changes;
  }
}
