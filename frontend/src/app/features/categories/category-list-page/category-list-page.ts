import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { ToastService } from '../../../core/feedback/toast/toast.service';
import { Button } from '../../../shared/ui/button/button';
import { EmptyState } from '../../../shared/ui/empty-state/empty-state';
import { ErrorState } from '../../../shared/ui/error-state/error-state';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import { CategoryFormComponent, CategoryFormMode } from '../components/category-form/category-form';
import { CategoryTreeComponent } from '../components/category-tree/category-tree';
import { CategoryApiService } from '../data-access/category-api.service';
import {
  Category,
  CategoryType,
  CreateCategoryRequest,
  UpdateCategoryRequest,
} from '../models/category.models';
import { buildCategoryTree } from '../models/category-tree.utils';

type LoadingState = 'loading' | 'success' | 'error';

@Component({
  selector: 'app-category-list-page',
  imports: [Button, CategoryFormComponent, CategoryTreeComponent, EmptyState, ErrorState, Skeleton],
  templateUrl: './category-list-page.html',
  styleUrl: './category-list-page.scss',
})
export class CategoryListPage implements OnInit {
  private readonly categoryApi = inject(CategoryApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toast = inject(ToastService);

  readonly categories = signal<Category[]>([]);
  readonly state = signal<LoadingState>('loading');
  readonly activeType = signal<CategoryType>('EXPENSE');

  readonly formMode = signal<CategoryFormMode | null>(null);
  readonly selectedCategory = signal<Category | null>(null);
  readonly selectedParent = signal<Category | null>(null);
  readonly isSubmitting = signal(false);

  readonly activeTree = computed(() => buildCategoryTree(this.categories(), this.activeType()));

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.state.set('loading');

    this.categoryApi
      .findAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (categories) => {
          this.categories.set(categories);
          this.state.set('success');
        },
        error: () => this.state.set('error'),
      });
  }

  selectType(type: CategoryType): void {
    this.activeType.set(type);
    this.closeForm();
  }

  startCreateRoot(): void {
    this.selectedCategory.set(null);
    this.selectedParent.set(null);
    this.formMode.set('create');
  }

  startCreateChild(parent: Category): void {
    this.selectedCategory.set(null);
    this.selectedParent.set(parent);
    this.formMode.set('create');
  }

  startEdit(category: Category): void {
    this.selectedCategory.set(category);
    this.selectedParent.set(null);
    this.formMode.set('edit');
  }

  createCategory(request: CreateCategoryRequest): void {
    this.isSubmitting.set(true);

    this.categoryApi
      .create(request)
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.toast.show({
            tone: 'success',
            title: 'Categoria criada',
            message: 'A categoria foi salva com sucesso.',
          });

          this.closeForm();
          this.loadCategories();
        },
      });
  }

  updateCategory(request: UpdateCategoryRequest): void {
    const category = this.selectedCategory();

    if (!category) {
      return;
    }

    this.isSubmitting.set(true);

    this.categoryApi
      .update(category.id, request)
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.toast.show({
            tone: 'success',
            title: 'Categoria atualizada',
            message: 'As alterações foram salvas com sucesso.',
          });

          this.closeForm();
          this.loadCategories();
        },
      });
  }

  closeForm(): void {
    this.formMode.set(null);
    this.selectedCategory.set(null);
    this.selectedParent.set(null);
  }
}
