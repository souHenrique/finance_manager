import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AccountApiService } from '../../data-access/account-api.service';
import { Account } from '../../models/account.models';

type LoadingState = 'loading' | 'success' | 'error';

@Component({
  imports: [],
  selector: 'app-account-list-page',
  styleUrl: './account-list-page.scss',
  templateUrl: './account-list-page.html',
})
export class AccountListPage implements OnInit {
  private readonly accountApi = inject(AccountApiService);
  private readonly destroyRef = inject(DestroyRef);

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
}
