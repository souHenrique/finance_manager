import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InvoiceListPage } from './invoice-list-page';

describe('InvoiceListPage', () => {
  let fixture: ComponentFixture<InvoiceListPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InvoiceListPage],
    }).compileComponents();

    fixture = TestBed.createComponent(InvoiceListPage);
    fixture.detectChanges();
  });

  it('should render the invoices heading and description', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Faturas');
    expect(element.querySelector('p')?.textContent).toContain(
      'Acompanhe as faturas dos seus cartões de crédito.',
    );
  });
});
