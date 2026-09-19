export type CategoryType = 'INCOME' | 'EXPENSE';
export type CategoryStatus = 'ACTIVE' | 'INACTIVE';
export type CategoryIcon =
  | 'TAG'
  | 'HOME'
  | 'FOOD'
  | 'SHOPPING'
  | 'TRANSPORT'
  | 'HEALTH'
  | 'EDUCATION'
  | 'LEISURE'
  | 'BILLS'
  | 'TRAVEL'
  | 'WORK'
  | 'GIFT'
  | 'PET'
  | 'INVESTMENT'
  | 'OTHER';

export const DEFAULT_CATEGORY_ICON: CategoryIcon = 'TAG';

export interface Category {
  id: string;
  name: string;
  icon?: CategoryIcon;
  type: CategoryType;
  parentCategoryId: string | null;
  status: CategoryStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCategoryRequest {
  name: string;
  icon: CategoryIcon;
  type: CategoryType;
  parentCategoryId?: string | null;
}

export interface UpdateCategoryRequest {
  name?: string;
  icon?: CategoryIcon;
  status?: CategoryStatus;
}
