import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { finalize } from 'rxjs';

import { ToastService } from '../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../core/http/api-request-error';
import { User } from '../../../shared/models/user.models';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Card } from '../../../shared/ui/card/card';
import { ErrorState } from '../../../shared/ui/error-state/error-state';
import { InputDirective } from '../../../shared/ui/form-control/input';
import { FormField } from '../../../shared/ui/form-field/form-field';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import { ProfileApiService } from '../data-access/profile-api.service';
import { UpdateProfileRequest } from '../models/profile.models';

const nonBlankValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const value = control.value;

  return typeof value === 'string' && value.trim().length > 0
    ? null
    : { required: true };
};

type ProfileField = 'name' | 'email';

@Component({
  selector: 'app-profile-page',
  imports: [
    ReactiveFormsModule,
    Alert,
    Button,
    Card,
    ErrorState,
    FormField,
    InputDirective,
    Skeleton,
  ],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePage implements OnInit {
  private readonly profileApi = inject(ProfileApiService);
  private readonly toast = inject(ToastService);

  protected readonly profile = signal<User | null>(null);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly submitted = signal(false);
  protected readonly loadError = signal<string | undefined>(
    undefined,
  );
  protected readonly submissionError = signal<string | undefined>(
    undefined,
  );

  protected readonly form = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [
        nonBlankValidator,
        Validators.maxLength(120),
      ],
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [
        nonBlankValidator,
        Validators.email,
        Validators.maxLength(320),
      ],
    }),
  });

  ngOnInit(): void {
    this.loadProfile();
  }

  protected loadProfile(): void {
    this.loading.set(true);
    this.loadError.set(undefined);

    this.profileApi
      .getCurrentUser()
      .pipe(
        finalize(() => {
          this.loading.set(false);
        }),
      )
      .subscribe({
        next: (profile) => {
          this.setProfile(profile);
        },
        error: (error: unknown) => {
          this.loadError.set(this.errorMessage(error));
        },
      });
  }

  protected submit(): void {
    this.submitted.set(true);
    this.submissionError.set(undefined);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request = this.buildUpdateRequest();

    if (!request) {
      this.toast.show({
        tone: 'info',
        title: 'Nenhuma alteração',
        message: 'Altere o nome ou o e-mail antes de salvar.',
      });
      return;
    }

    this.saving.set(true);

    this.profileApi
      .updateCurrentUser(request)
      .pipe(
        finalize(() => {
          this.saving.set(false);
        }),
      )
      .subscribe({
        next: (profile) => {
          this.setProfile(profile);
          this.submitted.set(false);

          this.toast.show({
            tone: 'success',
            title: 'Perfil atualizado',
            message: 'Suas informações foram salvas.',
          });
        },
        error: (error: unknown) => {
          this.handleUpdateError(error);
        },
      });
  }

  protected fieldError(
    fieldName: ProfileField,
  ): string | undefined {
    const control = this.form.controls[fieldName];

    if (!this.submitted() && !control.touched) {
      return undefined;
    }

    if (control.hasError('required')) {
      return 'Campo obrigatório.';
    }

    if (
      fieldName === 'name' &&
      control.hasError('maxlength')
    ) {
      return 'Nome deve possuir no máximo 120 caracteres.';
    }

    if (
      fieldName === 'email' &&
      control.hasError('email')
    ) {
      return 'Informe um e-mail válido.';
    }

    if (
      fieldName === 'email' &&
      control.hasError('maxlength')
    ) {
      return 'E-mail deve possuir no máximo 320 caracteres.';
    }

    const serverError = control.getError('server');

    return typeof serverError === 'string'
      ? serverError
      : undefined;
  }

  private setProfile(profile: User): void {
    this.profile.set(profile);

    this.form.reset({
      name: profile.name,
      email: profile.email,
    });

    this.form.markAsPristine();
    this.form.markAsUntouched();
  }

  private buildUpdateRequest(): UpdateProfileRequest | null {
    const currentProfile = this.profile();

    if (!currentProfile) {
      return null;
    }

    const values = this.form.getRawValue();
    const name = values.name.trim();
    const email = values.email.trim().toLowerCase();

    const request: UpdateProfileRequest = {};

    if (name !== currentProfile.name) {
      request.name = name;
    }

    if (email !== currentProfile.email.toLowerCase()) {
      request.email = email;
    }

    return Object.keys(request).length > 0
      ? request
      : null;
  }

  private handleUpdateError(error: unknown): void {
    this.submissionError.set(this.errorMessage(error));

    if (!(error instanceof ApiRequestError)) {
      return;
    }

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

  private errorMessage(error: unknown): string {
    if (error instanceof ApiRequestError) {
      return error.message;
    }

    return 'Não foi possível carregar ou atualizar o perfil.';
  }
}
