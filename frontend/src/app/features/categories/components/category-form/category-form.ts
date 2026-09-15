import { Component, effect, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import {
  Category,
  CategoryStatus,
  CategoryType,
  CreateCategoryRequest,
  UpdateCategoryRequest,
} from '../../models/category.models';

import { Button } from '../../../../shared/ui/button/button';
import { FormField } from '../../../../shared/ui/form-field/form-field';
import { InputDirective } from '../../../../shared/ui/form-control/input';
import { SelectDirective } from '../../../../shared/ui/form-control/select';

export type CategoryFormMode = 'create' | 'edit';

@Component({
  selector: 'app-category-form',
  imports: [ReactiveFormsModule, Button, FormField, InputDirective, SelectDirective],
  templateUrl: './category-form.html',
  styleUrl: './category-form.scss',
})
export class CategoryFormComponent {
  private readonly formBuilder = inject(FormBuilder);

  readonly mode = input.required<CategoryFormMode>();
  readonly category = input<Category | null>(null);
  readonly parentCategory = input<Category | null>(null);
  readonly defaultType = input<CategoryType>('EXPENSE');
  readonly submitting = input(false);

  readonly created = output<CreateCategoryRequest>();
  readonly updated = output<UpdateCategoryRequest>();
  readonly cancelled = output<void>();

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    type: ['EXPENSE' as CategoryType, Validators.required],
    status: ['ACTIVE' as CategoryStatus, Validators.required],
  });

  constructor() {
    effect(() => {
      const mode = this.mode();
      const category = this.category();
      const parent = this.parentCategory();

      if (mode === 'create') {
        this.form.reset({
          name: '',
          type: parent?.type ?? this.defaultType(),
          status: 'ACTIVE',
        });

        if (parent) {
          this.form.controls.type.disable();
        } else {
          this.form.controls.type.enable();
        }

        return;
      }

      if (category) {
        this.form.reset({
          name: category.name,
          type: category.type,
          status: category.status,
        });

        this.form.controls.type.disable();
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    if (this.mode() === 'create') {
      this.created.emit({
        name: value.name.trim(),
        type: value.type,
        parentCategoryId: this.parentCategory()?.id ?? null,
      });

      return;
    }

    this.updated.emit({
      name: value.name.trim(),
      status: value.status,
    });
  }
}
