import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';

import { AppDialogService } from '../../../core/feedback/dialog/dialog.service';
import { ToastService } from '../../../core/feedback/toast/toast.service';
import { CategoryApiService } from '../../categories/data-access/category-api.service';
import { Category } from '../../categories/models/category.models';
import { BudgetApiService } from '../data-access/budget-api.service';
import { Budget, CreateBudgetRequest, UpdateBudgetRequest } from '../models/budget.models';
import { BudgetListPage } from './budget-list-page';

describe('BudgetListPage', () => {
  let fixture: ComponentFixture<BudgetListPage>;
  let component: BudgetListPage;
  let budgetApi: {
    create: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
    findAll: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let categoryApi: { findAll: ReturnType<typeof vi.fn> };
  let dialog: { confirm: ReturnType<typeof vi.fn> };
  let toast: { show: ReturnType<typeof vi.fn> };

  const categories: Category[] = [
    {
      id: 'e1cb3b90-0d3b-4a25-bab8-4f74c6e76a21',
      name: 'Alimentação',
      type: 'EXPENSE',
      parentCategoryId: null,
      status: 'ACTIVE',
      createdAt: '2026-09-16T10:00:00Z',
      updatedAt: '2026-09-16T10:00:00Z',
    },
    {
      id: 'ae9075e1-74c2-4fd0-9056-943f44549f4e',
      name: 'Transporte',
      type: 'EXPENSE',
      parentCategoryId: null,
      status: 'ACTIVE',
      createdAt: '2026-09-16T10:00:00Z',
      updatedAt: '2026-09-16T10:00:00Z',
    },
  ];

  const budgets: Budget[] = [
    {
      id: '18ef5f4a-bffd-4d86-9366-e9088b0bda30',
      categoryId: categories[0].id,
      month: 9,
      year: 2026,
      amountLimit: 1000,
      spentAmount: 799.9,
      usagePercentage: 79.99,
      alertStatus: 'NORMAL',
      createdAt: '2026-09-16T10:00:00Z',
      updatedAt: '2026-09-16T10:00:00Z',
    },
    {
      id: 'f6f060b3-92db-4ebd-aefe-2be7f4c0c768',
      categoryId: categories[1].id,
      month: 9,
      year: 2026,
      amountLimit: 1000,
      spentAmount: 800,
      usagePercentage: 80,
      alertStatus: 'ALERT',
      createdAt: '2026-09-16T10:00:00Z',
      updatedAt: '2026-09-16T10:00:00Z',
    },
    {
      id: '4f33aa47-117f-4fe5-bec5-b0628915a5da',
      categoryId: categories[0].id,
      month: 9,
      year: 2026,
      amountLimit: 1000,
      spentAmount: 1000,
      usagePercentage: 100,
      alertStatus: 'LIMIT_REACHED',
      createdAt: '2026-09-16T10:00:00Z',
      updatedAt: '2026-09-16T10:00:00Z',
    },
    {
      id: 'f5b5bbc8-c564-4f11-bd38-4f52fbb1b424',
      categoryId: categories[1].id,
      month: 9,
      year: 2026,
      amountLimit: 1000,
      spentAmount: 1200,
      usagePercentage: 120,
      alertStatus: 'LIMIT_REACHED',
      createdAt: '2026-09-16T10:00:00Z',
      updatedAt: '2026-09-16T10:00:00Z',
    },
    {
      id: '6ee7e53d-a7e3-453e-b6d4-9810d9a70dc9',
      categoryId: categories[0].id,
      month: 10,
      year: 2026,
      amountLimit: 800,
      spentAmount: 0,
      usagePercentage: 0,
      alertStatus: 'NORMAL',
      createdAt: '2026-09-16T10:00:00Z',
      updatedAt: '2026-09-16T10:00:00Z',
    },
  ];

  const newBudget: CreateBudgetRequest = {
    categoryId: categories[0].id,
    month: 10,
    year: 2026,
    amountLimit: 1200,
  };

  beforeEach(async () => {
    budgetApi = {
      create: vi.fn().mockReturnValue(of({ ...budgets[0], ...newBudget })),
      delete: vi.fn().mockReturnValue(of(void 0)),
      findAll: vi.fn().mockReturnValue(of(budgets)),
      update: vi.fn().mockReturnValue(of({ ...budgets[0], amountLimit: 1100 })),
    };
    categoryApi = { findAll: vi.fn().mockReturnValue(of(categories)) };
    dialog = { confirm: vi.fn().mockReturnValue(of(false)) };
    toast = { show: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [BudgetListPage],
      providers: [
        { provide: BudgetApiService, useValue: budgetApi },
        { provide: CategoryApiService, useValue: categoryApi },
        { provide: AppDialogService, useValue: dialog },
        { provide: ToastService, useValue: toast },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BudgetListPage);
    component = fixture.componentInstance;
    component.selectPeriod('2026-09');
    fixture.detectChanges();
  });

  it('should render consumption states with text in addition to color', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('79,99%');
    expect(element.textContent).toContain('Consumo normal');
    expect(element.textContent).toContain('Alerta de consumo');
    expect(element.textContent).toContain('Limite atingido');
    expect(element.textContent).toContain('Limite excedido');
    expect(element.textContent).toContain('120% do limite utilizado');
    expect(element.querySelectorAll('progress')).toHaveLength(4);
  });

  it('should filter budgets when the selected period changes', () => {
    component.selectPeriod('2026-10');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(component.visibleBudgets()).toEqual([budgets[4]]);
    expect(element.textContent).toContain('Outubro de 2026');
    expect(element.textContent).not.toContain('120% do limite utilizado');
  });

  it('should render loading, error and retry states', () => {
    const pendingBudgets = new Subject<Budget[]>();
    budgetApi.findAll.mockReturnValueOnce(pendingBudgets);

    component.loadData();
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[aria-label="Carregando orçamentos"]'),
    ).not.toBeNull();

    pendingBudgets.next(budgets);
    pendingBudgets.complete();
    fixture.detectChanges();
    expect(component.state()).toBe('success');

    budgetApi.findAll.mockReturnValueOnce(throwError(() => new Error('Falha ao buscar')));
    component.loadData();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar os orçamentos');

    component.loadData();
    fixture.detectChanges();
    expect(component.state()).toBe('success');
  });

  it('should create and update budgets, keeping the selected period in sync', () => {
    component.startCreate();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-budget-form')).not.toBeNull();

    component.createBudget(newBudget);

    expect(budgetApi.create).toHaveBeenCalledWith(newBudget);
    expect(component.selectedMonth()).toBe(10);
    expect(toast.show).toHaveBeenCalledWith(expect.objectContaining({ title: 'Orçamento criado' }));

    component.startEdit(budgets[0]);
    const change: UpdateBudgetRequest = { amountLimit: 1100 };
    component.updateBudget(change);

    expect(budgetApi.update).toHaveBeenCalledWith(budgets[0].id, change);
    expect(toast.show).toHaveBeenCalledWith(
      expect.objectContaining({ title: 'Orçamento atualizado' }),
    );
  });

  it('should require confirmation before permanently deleting a budget', () => {
    component.confirmDelete(budgets[0]);

    expect(dialog.confirm).toHaveBeenCalledWith(
      expect.objectContaining({
        title: 'Excluir orçamento?',
        danger: true,
        confirmLabel: 'Excluir orçamento',
      }),
    );
    expect(budgetApi.delete).not.toHaveBeenCalled();

    dialog.confirm.mockReturnValueOnce(of(true));
    component.confirmDelete(budgets[0]);

    expect(budgetApi.delete).toHaveBeenCalledWith(budgets[0].id);
    expect(toast.show).toHaveBeenCalledWith(
      expect.objectContaining({ title: 'Orçamento excluído' }),
    );
  });
});
