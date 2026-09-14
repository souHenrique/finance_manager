import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AccountDetailPage } from './account-detail-page';

describe('AccountDetailPage', () => {
  let fixture: ComponentFixture<AccountDetailPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountDetailPage],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountDetailPage);
    fixture.detectChanges();
  });

  it('should render the account details heading and description', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Detalhes da conta');
    expect(element.querySelector('p')?.textContent).toContain(
      'Consulte as informações e movimentações da conta.',
    );
  });
});
