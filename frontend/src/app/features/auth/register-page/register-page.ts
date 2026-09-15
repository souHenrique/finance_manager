import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ToastService } from '../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../core/http/api-request-error';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { InputDirective } from '../../../shared/ui/form-control/input';
import { FormField } from '../../../shared/ui/form-field/form-field';
import { AuthService } from '../services/auth.service';

const nonBlankValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;

  return typeof value === 'string' && value.trim().length > 0 ? null : { required: true };
};

const passwordsMatchValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const password = control.get('password')?.value;
  const confirmation = control.get('confirmPassword')?.value;

  return password === confirmation ? null : { passwordMismatch: true };
};

type RegisterField = 'name' | 'email' | 'password';

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, Alert, Button, InputDirective, FormField],
  templateUrl: './register-page.html',
  styleUrl: './register-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly submitted = signal(false);
  protected readonly submissionError = signal<string | undefined>(undefined);

  protected readonly form = new FormGroup(
    {
      name: new FormControl('', {
        nonNullable: true,
        validators: [nonBlankValidator],
      }),
      email: new FormControl('', {
        nonNullable: true,
        validators: [nonBlankValidator, Validators.email],
      }),
      password: new FormControl('', {
        nonNullable: true,
        validators: [nonBlankValidator],
      }),
      confirmPassword: new FormControl('', {
        nonNullable: true,
        validators: [nonBlankValidator],
      }),
    },
    {
      validators: [passwordsMatchValidator],
    },
  );

  protected submit(): void {
    this.submitted.set(true);
    this.submissionError.set(undefined);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { name, email, password } = this.form.getRawValue();

    this.loading.set(true);

    this.auth
      .register({
        name: name.trim(),
        email: email.trim(),
        password,
      })
      .pipe(
        finalize(() => {
          this.loading.set(false);
        }),
      )
      .subscribe({
        next: () => {
          this.toast.show({
            tone: 'success',
            title: 'Conta criada',
            message: 'Agora você já pode entrar.',
          });

          void this.router.navigate(['/login']);
        },
        error: (error: unknown) => {
          this.handleError(error);
        },
      });
  }

  protected fieldError(fieldName: RegisterField): string | undefined {
    const control = this.form.controls[fieldName];

    if (!this.submitted() && !control.touched) {
      return undefined;
    }

    if (control.hasError('required')) {
      return 'Campo obrigatório.';
    }

    if (fieldName === 'email' && control.hasError('email')) {
      return 'Informe um e-mail válido.';
    }

    const serverError = control.getError('server');

    return typeof serverError === 'string' ? serverError : undefined;
  }

  protected confirmationError(): string | undefined {
    const control = this.form.controls.confirmPassword;

    if (!this.submitted() && !control.touched) {
      return undefined;
    }

    if (control.hasError('required')) {
      return 'Campo obrigatório.';
    }

    if (this.form.hasError('passwordMismatch')) {
      return 'As senhas não coincidem.';
    }

    return undefined;
  }

  private handleError(error: unknown): void {
    if (!(error instanceof ApiRequestError)) {
      this.submissionError.set('Não foi possível criar sua conta. Tente novamente.');
      return;
    }

    this.submissionError.set(error.message);

    for (const fieldError of error.fieldErrors) {
      const control = this.form.get(fieldError.field);

      if (control) {
        control.setErrors({
          ...control.errors,
          server: fieldError.message,
        });
      }
    }
  }
}
