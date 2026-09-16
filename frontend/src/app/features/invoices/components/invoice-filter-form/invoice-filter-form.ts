import { Component, effect, inject, input, output } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';

import { Button } from '../../../../shared/ui/button/button';
import { InputDirective } from '../../../../shared/ui/form-control/input';
import { SelectDirective } from '../../../../shared/ui/form-control/select';
import { FormField } from '../../../../shared/ui/form-field/form-field';
import { CreditCard } from '../../../credit-cards/models/credit-card.models';
import { InvoiceFilters, InvoiceStatus } from '../../models/invoice.models';

function integerValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;

  if (value === null || value === '') {
    return null;
  }

  return Number.isInteger(Number(value)) ? null : { integer: true };
}

@Component({
  selector: 'app-invoice-filter-form',
  imports: [ReactiveFormsModule, Button, FormField, InputDirective, SelectDirective],
  templateUrl: './invoice-filter-form.html',
  styleUrl: './invoice-filter-form.scss',
})
export class InvoiceFilterFormComponent {
  private readonly formBuilder = inject(FormBuilder);

  readonly initialFilters = input<InvoiceFilters>({});
  readonly creditCards = input<CreditCard[]>([]);
  readonly submitting = input(false);

  readonly applied = output<InvoiceFilters>();
  readonly cleared = output<void>();

  readonly months = [
    { value: 1, label: 'Janeiro' },
    { value: 2, label: 'Fevereiro' },
    { value: 3, label: 'Março' },
    { value: 4, label: 'Abril' },
    { value: 5, label: 'Maio' },
    { value: 6, label: 'Junho' },
    { value: 7, label: 'Julho' },
    { value: 8, label: 'Agosto' },
    { value: 9, label: 'Setembro' },
    { value: 10, label: 'Outubro' },
    { value: 11, label: 'Novembro' },
    { value: 12, label: 'Dezembro' },
  ] as const;

  readonly statuses: { value: InvoiceStatus; label: string }[] = [
    { value: 'OPEN', label: 'Aberta' },
    { value: 'CLOSED', label: 'Fechada' },
    { value: 'PAID', label: 'Paga' },
    { value: 'CANCELLED', label: 'Cancelada' },
  ];

  readonly form = this.formBuilder.group({
    creditCardId: [''],
    referenceMonth: [
      null as number | null,
      [Validators.min(1), Validators.max(12), integerValidator],
    ],
    referenceYear: [
      null as number | null,
      [Validators.min(1), Validators.max(9999), integerValidator],
    ],
    status: [null as InvoiceStatus | null],
  });

  constructor() {
    effect(() => {
      const filters = this.initialFilters();

      this.form.reset({
        creditCardId: filters.creditCardId ?? '',
        referenceMonth: filters.referenceMonth ?? null,
        referenceYear: filters.referenceYear ?? null,
        status: filters.status ?? null,
      });
    });
  }

  apply(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.applied.emit(this.toFilters());
  }

  clear(): void {
    this.form.reset({
      creditCardId: '',
      referenceMonth: null,
      referenceYear: null,
      status: null,
    });

    this.cleared.emit();
  }

  monthError(): string | undefined {
    const control = this.form.controls.referenceMonth;

    if (!control.touched || control.valid) {
      return undefined;
    }

    if (control.hasError('integer')) {
      return 'Informe um mês inteiro.';
    }

    return 'Informe um mês entre 1 e 12.';
  }

  yearError(): string | undefined {
    const control = this.form.controls.referenceYear;

    if (!control.touched || control.valid) {
      return undefined;
    }

    if (control.hasError('integer')) {
      return 'Informe um ano inteiro.';
    }

    return 'Informe um ano entre 1 e 9999.';
  }

  private toFilters(): InvoiceFilters {
    const value = this.form.getRawValue();
    const filters: InvoiceFilters = {};

    if (value.creditCardId) {
      filters.creditCardId = value.creditCardId;
    }

    if (value.referenceMonth !== null && Number.isInteger(value.referenceMonth)) {
      filters.referenceMonth = value.referenceMonth;
    }

    if (value.referenceYear !== null && Number.isInteger(value.referenceYear)) {
      filters.referenceYear = value.referenceYear;
    }

    if (value.status) {
      filters.status = value.status;
    }

    return filters;
  }
}
