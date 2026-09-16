import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReportCategoryChartComponent } from './report-category-chart';

describe('ReportCategoryChartComponent', () => {
  let fixture: ComponentFixture<ReportCategoryChartComponent>;
  let component: ReportCategoryChartComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportCategoryChartComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportCategoryChartComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('title', 'Distribuição por categoria');
    fixture.componentRef.setInput('tableCaption', 'Tabela alternativa ao gráfico de categorias.');
  });

  it('should render the visual chart and its equivalent accessible table', () => {
    fixture.componentRef.setInput('entries', [
      { label: 'Salário', amount: 5000, type: 'Receita' },
      { label: 'Mercado', amount: 800, type: 'Despesa' },
    ]);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('[role="img"]')).not.toBeNull();
    expect(element.querySelector('table')).not.toBeNull();
    expect(element.textContent).toContain('Receita: Salário');
    expect(element.textContent).toContain('Despesa: Mercado');
    expect(component.barWidth(5000)).toBe(100);
    expect(component.barWidth(800)).toBe(16);
  });

  it('should describe the absence of category data without rendering a chart', () => {
    fixture.componentRef.setInput('entries', []);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'Não há movimentações por categoria neste período.',
    );
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
  });
});
