import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TransactionListPage } from './transaction-list-page';

describe('TransactionListPage', () => {
  let fixture: ComponentFixture<TransactionListPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransactionListPage],
    }).compileComponents();

    fixture = TestBed.createComponent(TransactionListPage);
    fixture.detectChanges();
  });

  it('should render the transactions heading and description', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Transações');
    expect(element.querySelector('p')?.textContent).toContain(
      'Consulte e gerencie suas movimentações financeiras.',
    );
  });
});
