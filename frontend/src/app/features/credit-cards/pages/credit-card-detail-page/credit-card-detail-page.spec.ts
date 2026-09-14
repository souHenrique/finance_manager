import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CreditCardDetailPage } from './credit-card-detail-page';

describe('CreditCardDetailPage', () => {
  let fixture: ComponentFixture<CreditCardDetailPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreditCardDetailPage],
    }).compileComponents();

    fixture = TestBed.createComponent(CreditCardDetailPage);
    fixture.detectChanges();
  });

  it('should render the credit card details heading and description', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Detalhes do cartão');
    expect(element.querySelector('p')?.textContent).toContain(
      'Consulte os dados e as faturas do cartão.',
    );
  });
});
