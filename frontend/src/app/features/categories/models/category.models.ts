export type CategoryType = 'INCOME' | 'EXPENSE';
export type CategoryStatus = 'ACTIVE' | 'INACTIVE';

export interface Category {
  id: string;
  name: string;
  type: CategoryType;
  parentCategoryId: string | null;
  status: CategoryStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCategoryRequest {
  name: string;
  type: CategoryType;
  parentCategoryId?: string | null;
}

export interface UpdateCategoryRequest {
  name?: string;
  status?: CategoryStatus;
}
