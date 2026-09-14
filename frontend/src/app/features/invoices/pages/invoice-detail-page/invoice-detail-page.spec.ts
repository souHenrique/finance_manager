import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InvoiceDetailPage } from './invoice-detail-page';

describe('InvoiceDetailPage', () => {
  let fixture: ComponentFixture<InvoiceDetailPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InvoiceDetailPage],
    }).compileComponents();

    fixture = TestBed.createComponent(InvoiceDetailPage);
    fixture.detectChanges();
  });

  it('should render the invoice details heading and description', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Detalhes da fatura');
    expect(element.querySelector('p')?.textContent).toContain(
      'Consulte os lançamentos e o status da fatura.',
    );
  });
});
