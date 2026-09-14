import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BudgetListPage } from './budget-list-page';

describe('BudgetListPage', () => {
  let fixture: ComponentFixture<BudgetListPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BudgetListPage],
    }).compileComponents();

    fixture = TestBed.createComponent(BudgetListPage);
    fixture.detectChanges();
  });

  it('should render the budgets heading and description', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Orçamentos');
    expect(element.querySelector('p')?.textContent).toContain(
      'Defina e acompanhe limites mensais por categoria.',
    );
  });
});
