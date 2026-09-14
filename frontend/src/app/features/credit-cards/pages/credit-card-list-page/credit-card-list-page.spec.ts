import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CreditCardListPage } from './credit-card-list-page';

describe('CreditCardListPage', () => {
  let fixture: ComponentFixture<CreditCardListPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreditCardListPage],
    }).compileComponents();

    fixture = TestBed.createComponent(CreditCardListPage);
    fixture.detectChanges();
  });

  it('should render the credit cards heading and description', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Cartões de crédito');
    expect(element.querySelector('p')?.textContent).toContain(
      'Consulte e gerencie seus cartões de crédito.',
    );
  });
});
