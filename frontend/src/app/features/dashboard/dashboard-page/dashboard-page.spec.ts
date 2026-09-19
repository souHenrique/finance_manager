import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { CategoryApiService } from '../../categories/data-access/category-api.service';
import { Category } from '../../categories/models/category.models';
import { DashboardApiService } from '../data-access/dashboard-api.service';
import { Dashboard } from '../models/dashboard.models';
import { DashboardPage } from './dashboard-page';

describe('DashboardPage', () => {
  let fixture: ComponentFixture<DashboardPage>;
  let component: DashboardPage;
  let dashboardApi: { get: ReturnType<typeof vi.fn> };
  let categoryApi: { findAll: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };

  const categories: Category[] = [
    {
      id: '57b1879c-a98e-4718-b66d-47f970ab6709',
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

  const dashboard: Dashboard = {
    referenceDate: '2026-09-16',
    year: 2026,
    month: 9,
    periodStart: '2026-09-01',
    periodEnd: '2026-09-30',
    monthlyBalance: { basis: 'CASH_AND_INVOICE', amount: 2750 },
    monthlyInflows: { basis: 'CASH', amount: 5000 },
    totalOutflows: { basis: 'CASH', amount: 18000 },
    monthlyOutflows: { basis: 'CASH', amount: 1500 },
    creditCardPurchaseOutflows: { basis: 'COMPETENCE', amount: 750 },
    competenceExpenses: { basis: 'COMPETENCE', amount: 1800 },
    openInvoices: { basis: 'COMPETENCE', amount: 850 },
    budget: {
      basis: 'COMPETENCE',
      totalLimit: 3000,
      totalSpent: 2100,
      usagePercentage: 70,
      items: [
        {
          budgetId: 'c487c4cf-d948-4ba8-a85f-e36bb798c928',
          categoryId: categories[0].id,
          amountLimit: 1500,
          spentAmount: 1200,
          usagePercentage: 80,
          alertStatus: 'ALERT',
        },
        {
          budgetId: 'cb4b2101-4b17-4f67-9c8a-2c42bd4348f5',
          categoryId: categories[1].id,
          amountLimit: 1000,
          spentAmount: 1200,
          usagePercentage: 120,
          alertStatus: 'LIMIT_REACHED',
        },
      ],
    },
    consolidatedBalance: { basis: 'CASH', amount: 7000 },
  };

  beforeEach(async () => {
    dashboardApi = { get: vi.fn().mockReturnValue(of(dashboard)) };
    categoryApi = { findAll: vi.fn().mockReturnValue(of(categories)) };
    router = { navigate: vi.fn().mockResolvedValue(true) };

    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        { provide: DashboardApiService, useValue: dashboardApi },
        { provide: CategoryApiService, useValue: categoryApi },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render only the daily-decision indicators without exposing accounting bases', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(dashboardApi.get).toHaveBeenCalledOnce();
    expect(element.querySelector('h1')?.textContent).toContain('Dashboard financeiro');
    expect(element.textContent).toContain('Setembro de 2026');

    for (const title of [
      'Saldo',
      'Entradas mensais',
      'Saídas mensais',
      'Faturas abertas',
      'Orçamento',
      'Saldo consolidado',
    ]) {
      expect(element.textContent).toContain(title);
    }

    expect(element.textContent).not.toContain('CASH + FATURA');
    expect(element.textContent).not.toContain('DATA DA DESPESA');
    expect(element.textContent).not.toContain('Regime de caixa');
    expect(element.textContent).toContain(
      'Total acumulado de entradas efetivadas no mês de referência.',
    );
  });

  it('should leave analytical indicators to reports and place the consolidated balance last', () => {
    const element = fixture.nativeElement as HTMLElement;
    const titles = Array.from(
      element.querySelectorAll<HTMLElement>('.dashboard__indicators app-card h2'),
    ).map((title) => title.textContent?.trim());

    expect(titles).toEqual(['Saldo', 'Entradas mensais', 'Saídas mensais', 'Faturas abertas']);
    expect(element.querySelectorAll('.dashboard__indicator-card')).toHaveLength(4);

    expect(element.textContent).not.toContain('Saídas totais');
    expect(element.textContent).not.toContain('Saídas de compras no crédito');
    expect(element.textContent).not.toContain('Despesas por competência');

    const sections = Array.from(element.querySelectorAll<HTMLElement>('.dashboard > section'));
    expect(sections.at(-1)?.textContent).toContain('Saldo consolidado');
  });

  it('should navigate to reports for the detailed analysis', () => {
    const reportsButton = Array.from<HTMLButtonElement>(
      fixture.nativeElement.querySelectorAll('button'),
    ).find((button) => button.textContent?.includes('Ver relatórios')) as HTMLButtonElement;

    reportsButton.click();

    expect(router.navigate).toHaveBeenCalledWith(['/reports']);
  });

  it('should render the budget summary and explicit alert labels for each category', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Limite total');
    expect(element.textContent).toContain('Percentual consumido');
    expect(element.textContent).toContain('Alimentação');
    expect(element.textContent).toContain('Alerta de consumo');
    expect(element.textContent).toContain('Transporte');
    expect(element.textContent).toContain('Limite excedido');
    expect(element.textContent).toContain('120% do limite utilizado');
    expect(element.querySelectorAll('progress')).toHaveLength(1);
  });

  it('should render loading, error and retry states', () => {
    const pendingDashboard = new Subject<Dashboard>();
    dashboardApi.get.mockReturnValueOnce(pendingDashboard);

    component.loadDashboard();
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[aria-label="Carregando indicadores do dashboard"]'),
    ).not.toBeNull();

    pendingDashboard.next(dashboard);
    pendingDashboard.complete();
    fixture.detectChanges();
    expect(component.state()).toBe('success');

    dashboardApi.get.mockReturnValueOnce(throwError(() => new Error('Falha ao buscar')));
    component.loadDashboard();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar o dashboard');

    component.loadDashboard();
    fixture.detectChanges();
    expect(component.state()).toBe('success');
  });

  it('should keep dashboard data available when category labels cannot be loaded', () => {
    categoryApi.findAll.mockReturnValueOnce(
      throwError(() => new Error('Falha ao buscar categorias')),
    );

    component.loadDashboard();
    fixture.detectChanges();

    expect(component.state()).toBe('success');
    expect(fixture.nativeElement.textContent).toContain('Categoria indisponível');
  });
});
