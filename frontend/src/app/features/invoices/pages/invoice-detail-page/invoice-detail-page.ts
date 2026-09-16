import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { filter, forkJoin, map, switchMap, tap } from 'rxjs';

import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { CreditCardApiService } from '../../../credit-cards/data-access/credit-card-api.service';
import { CreditCard } from '../../../credit-cards/models/credit-card.models';
import { Transaction } from '../../../transactions/models/transaction.models';
import { AppDialogService } from '../../../../core/feedback/dialog/dialog.service';
import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../../core/http/api-request-error';
import { Alert } from '../../../../shared/ui/alert/alert';
import { Badge } from '../../../../shared/ui/badge/badge';
import { Button } from '../../../../shared/ui/button/button';
import { Card } from '../../../../shared/ui/card/card';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { SelectDirective } from '../../../../shared/ui/form-control/select';
import { FormField } from '../../../../shared/ui/form-field/form-field';
import { Skeleton } from '../../../../shared/ui/skeleton/skeleton';
import type { FeedbackTone } from '../../../../shared/ui/types/feedback-tone';
import { InvoiceApiService } from '../../data-access/invoice-api.service';
import { InvoiceDetail, InvoiceStatus } from '../../models/invoice.models';

type InvoiceDetailState = 'loading' | 'success' | 'error';
type InvoiceOperation = 'closing' | 'paying' | null;

@Component({
  selector: 'app-invoice-detail-page',
  imports: [
    DecimalPipe,
    ReactiveFormsModule,
    Alert,
    Badge,
    Button,
    Card,
    ErrorState,
    FormField,
    SelectDirective,
    Skeleton,
  ],
  templateUrl: './invoice-detail-page.html',
  styleUrl: './invoice-detail-page.scss',
})
export class InvoiceDetailPage implements OnInit {
  private readonly accountApi = inject(AccountApiService);
  private readonly creditCardApi = inject(CreditCardApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(AppDialogService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly invoiceApi = inject(InvoiceApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  private readonly invoiceId = this.route.snapshot.paramMap.get('id');

  readonly invoice = signal<InvoiceDetail | null>(null);
  readonly creditCard = signal<CreditCard | null>(null);
  readonly accounts = signal<Account[]>([]);
  readonly state = signal<InvoiceDetailState>('loading');
  readonly operation = signal<InvoiceOperation>(null);
  readonly isPaymentFormOpen = signal(false);
  readonly conflictMessage = signal('');

  readonly activeAccounts = computed(() =>
    this.accounts()
      .filter((account) => account.status === 'ACTIVE')
      .sort((first, second) => first.name.localeCompare(second.name, 'pt-BR')),
  );

  readonly isProcessing = computed(() => this.operation() !== null);

  readonly paymentForm = this.formBuilder.nonNullable.group({
    sourceAccountId: [''],
  });

  ngOnInit(): void {
    this.loadInvoice();
  }

  loadInvoice(): void {
    if (!this.invoiceId) {
      void this.router.navigate(['/invoices']);
      return;
    }

    this.state.set('loading');
    this.conflictMessage.set('');
    this.isPaymentFormOpen.set(false);

    this.invoiceApi
      .findById(this.invoiceId)
      .pipe(
        switchMap((invoice) =>
          forkJoin({
            creditCard: this.creditCardApi.findById(invoice.creditCardId),
            accounts: this.accountApi.findAll(),
          }).pipe(
            map(({ creditCard, accounts }) => ({
              invoice,
              creditCard,
              accounts,
            })),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ invoice, creditCard, accounts }) => {
          this.invoice.set(invoice);
          this.creditCard.set(creditCard);
          this.accounts.set(accounts);
          this.selectDefaultPaymentAccount(creditCard, accounts);
          this.state.set('success');
        },
        error: () => {
          this.state.set('error');
        },
      });
  }

  openPaymentForm(): void {
    const invoice = this.invoice();

    if (invoice?.status !== 'CLOSED' || this.isProcessing()) {
      return;
    }

    this.isPaymentFormOpen.set(true);
  }

  closeInvoice(): void {
    const invoice = this.invoice();

    if (!invoice || invoice.status !== 'OPEN' || this.isProcessing()) {
      return;
    }

    this.dialog
      .confirm({
        title: 'Fechar fatura?',
        message:
          'Depois de fechada, a fatura não aceitará novas compras. O backend confirmará se a data de fechamento já foi atingida.',
        confirmLabel: 'Fechar fatura',
        cancelLabel: 'Cancelar',
      })
      .pipe(
        filter((confirmed): confirmed is true => confirmed === true),
        tap(() => this.operation.set('closing')),
        switchMap(() =>
          this.invoiceApi.close(invoice.id, {
            expectedVersion: invoice.version,
          }),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.operation.set(null);

          this.toast.show({
            tone: 'success',
            title: 'Fatura fechada',
            message: 'A fatura foi fechada e está pronta para pagamento.',
          });

          this.loadInvoice();
        },
        error: (error: unknown) => {
          this.operation.set(null);
          this.handleMutationError(error);
        },
      });
  }

  payInvoice(): void {
    const invoice = this.invoice();

    if (!invoice || invoice.status !== 'CLOSED' || this.isProcessing()) {
      return;
    }

    const sourceAccountId = this.paymentForm.controls.sourceAccountId.value || null;
    const sourceAccount = this.activeAccounts().find((account) => account.id === sourceAccountId);
    const paymentSource = sourceAccount
      ? `usando a conta ${sourceAccount.name}`
      : 'com os créditos disponíveis';

    this.dialog
      .confirm({
        title: 'Pagar fatura?',
        message: `A fatura de ${this.formatAmount(invoice.totalAmount)} será quitada ${paymentSource}. Créditos aplicáveis e eventual valor a debitar serão definidos pelo backend.`,
        confirmLabel: 'Pagar fatura',
        cancelLabel: 'Revisar dados',
        danger: true,
      })
      .pipe(
        filter((confirmed): confirmed is true => confirmed === true),
        tap(() => this.operation.set('paying')),
        switchMap(() =>
          this.invoiceApi.pay(invoice.id, {
            sourceAccountId,
            expectedVersion: invoice.version,
          }),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.operation.set(null);

          this.toast.show({
            tone: 'success',
            title: 'Fatura paga',
            message: 'O pagamento foi registrado e o limite do cartão foi atualizado.',
          });

          this.loadInvoice();
        },
        error: (error: unknown) => {
          this.operation.set(null);
          this.handleMutationError(error);
        },
      });
  }

  goBack(): void {
    void this.router.navigate(['/invoices']);
  }

  goToCreditCard(): void {
    const creditCard = this.creditCard();

    if (creditCard) {
      void this.router.navigate(['/credit-cards', creditCard.id]);
    }
  }

  reloadAfterConflict(): void {
    this.loadInvoice();
  }

  referenceLabel(invoice: InvoiceDetail): string {
    return `${String(invoice.referenceMonth).padStart(2, '0')}/${invoice.referenceYear}`;
  }

  formatDate(value: string): string {
    const [year, month, day] = value.split('-');

    return `${day}/${month}/${year}`;
  }

  statusLabel(status: InvoiceStatus): string {
    const labels: Record<InvoiceStatus, string> = {
      OPEN: 'Aberta',
      CLOSED: 'Fechada',
      PAID: 'Paga',
      CANCELLED: 'Cancelada',
    };

    return labels[status];
  }

  statusTone(status: InvoiceStatus): FeedbackTone {
    const tones: Record<InvoiceStatus, FeedbackTone> = {
      OPEN: 'info',
      CLOSED: 'warning',
      PAID: 'success',
      CANCELLED: 'neutral',
    };

    return tones[status];
  }

  transactionTypeLabel(type: Transaction['type']): string {
    const labels: Record<Transaction['type'], string> = {
      INCOME: 'Receita',
      EXPENSE: 'Despesa',
      TRANSFER: 'Transferência',
      CREDIT_CARD_PURCHASE: 'Compra no cartão',
      CREDIT_CARD_PAYMENT: 'Pagamento de fatura',
      ADJUSTMENT: 'Ajuste',
    };

    return labels[type];
  }

  transactionStatusLabel(status: Transaction['status']): string {
    const labels: Record<Transaction['status'], string> = {
      PENDING: 'Pendente',
      COMPLETED: 'Concluída',
      CANCELLED: 'Cancelada',
    };

    return labels[status];
  }

  transactionStatusTone(status: Transaction['status']): FeedbackTone {
    const tones: Record<Transaction['status'], FeedbackTone> = {
      PENDING: 'warning',
      COMPLETED: 'success',
      CANCELLED: 'neutral',
    };

    return tones[status];
  }

  installmentLabel(transaction: Transaction): string {
    if (
      transaction.type !== 'CREDIT_CARD_PURCHASE' ||
      !transaction.installmentCount ||
      transaction.installmentCount === 1
    ) {
      return 'À vista';
    }

    return `Parcela ${transaction.installmentNumber} de ${transaction.installmentCount}`;
  }

  private selectDefaultPaymentAccount(creditCard: CreditCard, accounts: Account[]): void {
    const activeAccounts = accounts.filter((account) => account.status === 'ACTIVE');

    const selectedAccount =
      activeAccounts.find((account) => account.id === creditCard.defaultAccountId) ??
      activeAccounts[0];

    this.paymentForm.reset({
      sourceAccountId: selectedAccount?.id ?? '',
    });
  }

  private handleMutationError(error: unknown): void {
    if (error instanceof ApiRequestError && error.status === 409) {
      this.conflictMessage.set(error.message);
    }
  }

  private formatAmount(amount: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(amount);
  }
}
