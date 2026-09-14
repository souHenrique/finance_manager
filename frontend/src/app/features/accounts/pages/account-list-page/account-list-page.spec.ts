import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AccountListPage } from './account-list-page';

describe('AccountListPage', () => {
  let fixture: ComponentFixture<AccountListPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountListPage],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountListPage);
    fixture.detectChanges();
  });

  it('should render the accounts heading and description', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Contas');
    expect(element.querySelector('p')?.textContent).toContain(
      'Consulte e gerencie suas contas financeiras.',
    );
  });
});
