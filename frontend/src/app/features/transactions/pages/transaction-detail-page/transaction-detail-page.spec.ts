import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TransactionDetailPage } from './transaction-detail-page';

describe('TransactionDetailPage', () => {
  let fixture: ComponentFixture<TransactionDetailPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransactionDetailPage],
    }).compileComponents();

    fixture = TestBed.createComponent(TransactionDetailPage);
    fixture.detectChanges();
  });

  it('should render the transaction details heading and description', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Detalhes da transação');
    expect(element.querySelector('p')?.textContent).toContain(
      'Consulte as informações da transação selecionada.',
    );
  });
});
