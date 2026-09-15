import { BudgetAlertStatus } from '../../budgets/models/budget.models';

export type AccountingBasis = 'CASH' | 'COMPETENCE';

export interface DashboardIndicator {
  basis: AccountingBasis;
  amount: number;
}

export interface DashboardBudgetItem {
  budgetId: string;
  categoryId: string;
  amountLimit: number;
  spentAmount: number;
  usagePercentage: number;
  alertStatus: BudgetAlertStatus;
}

export interface DashboardBudget {
  basis: 'COMPETENCE';
  totalLimit: number;
  totalSpent: number;
  usagePercentage: number;
  items: DashboardBudgetItem[];
}

export interface Dashboard {
  referenceDate: string;
  year: number;
  month: number;
  periodStart: string;
  periodEnd: string;
  consolidatedBalance: DashboardIndicator;
  monthlyInflows: DashboardIndicator;
  cashOutflows: DashboardIndicator;
  competenceExpenses: DashboardIndicator;
  openInvoices: DashboardIndicator;
  budget: DashboardBudget;
  netWorth: DashboardIndicator;
}
