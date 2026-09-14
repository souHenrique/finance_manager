import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TransactionCreatePage } from './transaction-create-page';

describe('TransactionCreatePage', () => {
  let fixture: ComponentFixture<TransactionCreatePage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransactionCreatePage],
    }).compileComponents();

    fixture = TestBed.createComponent(TransactionCreatePage);
    fixture.detectChanges();
  });

  it('should render the transaction creation heading and description', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Nova transação');
    expect(element.querySelector('p')?.textContent).toContain(
      'Cadastre uma nova movimentação financeira.',
    );
  });
});
