import { Component, effect, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import {
  Category,
  CategoryIcon,
  CategoryStatus,
  CategoryType,
  CreateCategoryRequest,
  DEFAULT_CATEGORY_ICON,
  UpdateCategoryRequest,
} from '../../models/category.models';

import { Button } from '../../../../shared/ui/button/button';
import { FormField } from '../../../../shared/ui/form-field/form-field';
import { InputDirective } from '../../../../shared/ui/form-control/input';
import { SelectDirective } from '../../../../shared/ui/form-control/select';
import { CATEGORY_ICON_OPTIONS, CategoryIconComponent } from '../category-icon/category-icon';

export type CategoryFormMode = 'create' | 'edit';

@Component({
  selector: 'app-category-form',
  imports: [
    ReactiveFormsModule,
    Button,
    CategoryIconComponent,
    FormField,
    InputDirective,
    SelectDirective,
  ],
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
  readonly iconOptions = CATEGORY_ICON_OPTIONS;

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    icon: [DEFAULT_CATEGORY_ICON as CategoryIcon, Validators.required],
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
          icon: DEFAULT_CATEGORY_ICON,
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
          icon: category.icon ?? DEFAULT_CATEGORY_ICON,
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
        icon: value.icon,
        type: value.type,
        parentCategoryId: this.parentCategory()?.id ?? null,
      });

      return;
    }

    const request: UpdateCategoryRequest = {
      name: value.name.trim(),
      status: value.status,
    };

    if (value.icon !== (this.category()?.icon ?? DEFAULT_CATEGORY_ICON)) {
      request.icon = value.icon;
    }

    this.updated.emit(request);
  }

  selectIcon(icon: CategoryIcon): void {
    if (!this.submitting()) {
      this.form.controls.icon.setValue(icon);
    }
  }
}
