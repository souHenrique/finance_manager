import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {
  ActivatedRoute,
  Router,
  RouterLink,
} from '@angular/router';
import { finalize } from 'rxjs';

import { ApiRequestError } from '../../../core/http/api-request-error';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { FormField } from '../../../shared/ui/form-field/form-field';
import { InputDirective } from '../../../shared/ui/form-control/input';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    Alert,
    Button,
    FormField,
    InputDirective,
  ],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly loading = signal(false);
  protected readonly submitted = signal(false);
  protected readonly submissionError = signal<string | undefined>(
    undefined,
  );

  protected readonly form = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.email,
      ],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  protected submit(): void {
    this.submitted.set(true);
    this.submissionError.set(undefined);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);

    this.auth
      .login(this.form.getRawValue())
      .pipe(
        finalize(() => {
          this.loading.set(false);
        }),
      )
      .subscribe({
        next: () => {
          void this.router.navigateByUrl(this.getReturnUrl());
        },
        error: (error: unknown) => {
          this.handleError(error);
        },
      });
  }

  protected fieldError(
    fieldName: 'email' | 'password',
  ): string | undefined {
    const control = this.form.controls[fieldName];

    if (!this.submitted() && !control.touched) {
      return undefined;
    }

    if (control.hasError('required')) {
      return 'Campo obrigatório.';
    }

    if (
      fieldName === 'email' &&
      control.hasError('email')
    ) {
      return 'Informe um e-mail válido.';
    }

    const serverError = control.getError('server');

    return typeof serverError === 'string'
      ? serverError
      : undefined;
  }

  private getReturnUrl(): string {
    const returnUrl =
      this.route.snapshot.queryParamMap.get('returnUrl');

    if (
      returnUrl?.startsWith('/') &&
      !returnUrl.startsWith('//')
    ) {
      return returnUrl;
    }

    return '/dashboard';
  }

  private handleError(error: unknown): void {
    if (!(error instanceof ApiRequestError)) {
      this.submissionError.set(
        'Não foi possível entrar. Tente novamente.',
      );
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
