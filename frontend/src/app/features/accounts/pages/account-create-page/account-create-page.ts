import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AccountApiService } from '../../data-access/account-api.service';
import { CreateAccountRequest } from '../../models/account.models';
import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { AccountFormComponent } from '../../components/account-form/account-form';

@Component({
  selector: 'app-account-create-page',
  imports: [AccountFormComponent],
  templateUrl: './account-create-page.html',
  styleUrl: './account-create-page.scss',
})
export class AccountCreatePageComponent {
  private readonly accountApi = inject(AccountApiService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly isSubmitting = signal(false);

  createAccount(request: CreateAccountRequest): void {
    this.isSubmitting.set(true);

    this.accountApi
      .create(request)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (account) => {
          this.toast.show({
            tone: 'success',
            title: 'Conta criada',
            message: 'A conta foi criada com sucesso.',
          });

          void this.router.navigate(['/accounts', account.id]);
        },
      });
  }

  goBack(): void {
    void this.router.navigate(['/accounts']);
  }
}
