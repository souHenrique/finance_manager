export type BudgetAlertStatus = 'NORMAL' | 'ALERT' | 'LIMIT_REACHED';

export interface Budget {
  id: string;
  categoryId: string;
  month: number;
  year: number;
  amountLimit: number;
  spentAmount: number;
  usagePercentage: number;
  alertStatus: BudgetAlertStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBudgetRequest {
  categoryId: string;
  month: number;
  year: number;
  amountLimit: number;
}

export interface UpdateBudgetRequest {
  categoryId?: string;
  month?: number;
  year?: number;
  amountLimit?: number;
}
