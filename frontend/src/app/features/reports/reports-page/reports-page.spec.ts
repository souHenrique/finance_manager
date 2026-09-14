import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReportsPage } from './reports-page';

describe('ReportsPage', () => {
  let fixture: ComponentFixture<ReportsPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportsPage],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportsPage);
    fixture.detectChanges();
  });

  it('should render the reports heading and description', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Relatórios');
    expect(element.querySelector('p')?.textContent).toContain(
      'Analise receitas, despesas e resultados por competência.',
    );
  });
});
