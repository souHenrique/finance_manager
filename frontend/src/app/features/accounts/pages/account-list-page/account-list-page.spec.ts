import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { AccountApiService } from '../../data-access/account-api.service';
import { Account } from '../../models/account.models';
import { AccountListPage } from './account-list-page';

describe('AccountListPage', () => {
  let fixture: ComponentFixture<AccountListPage>;
  let accountApi: { findAll: ReturnType<typeof vi.fn> };
  let router: Router;

  const accounts: Account[] = [
    {
      id: '1fcd7be3-8458-469d-b07d-7514c365b5de',
      name: 'Conta Walter',
      type: 'CHECKING',
      institution: 'Banco Albuquerque',
      initialBalance: 1200,
      currentBalance: 1380.5,
      status: 'ACTIVE',
      version: 1,
      createdAt: '2026-09-15T10:00:00Z',
      updatedAt: '2026-09-15T10:00:00Z',
    },
    {
      id: '6a8f0aae-a55e-435d-a176-f34dc9428987',
      name: 'Reserva Jesse',
      type: 'SAVINGS',
      institution: null,
      initialBalance: 800,
      currentBalance: 950,
      status: 'INACTIVE',
      version: 1,
      createdAt: '2026-09-15T10:00:00Z',
      updatedAt: '2026-09-15T10:00:00Z',
    },
  ];

  beforeEach(async () => {
    accountApi = { findAll: vi.fn().mockReturnValue(of(accounts)) };

    await TestBed.configureTestingModule({
      imports: [AccountListPage],
      providers: [provideRouter([]), { provide: AccountApiService, useValue: accountApi }],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  function createPage(): void {
    fixture = TestBed.createComponent(AccountListPage);
    fixture.detectChanges();
  }

  it('should show skeletons while accounts are loading', () => {
    const response = new Subject<Account[]>();
    accountApi.findAll.mockReturnValue(response.asObservable());

    createPage();

    expect(fixture.nativeElement.querySelectorAll('app-skeleton')).toHaveLength(3);

    response.next(accounts);
    response.complete();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-skeleton')).toBeNull();
  });

  it('should render loaded accounts and their statuses', () => {
    createPage();

    const content = fixture.nativeElement.textContent as string;

    expect(accountApi.findAll).toHaveBeenCalledOnce();
    expect(content).toContain('Conta Walter');
    expect(content).toContain('Reserva Jesse');
    expect(content).toContain('Ativa');
    expect(content).toContain('Inativa');
    expect(content).toContain('Instituição não informada');
  });

  it('should display an error state and retry loading', () => {
    accountApi.findAll
      .mockReturnValueOnce(throwError(() => new Error('network')))
      .mockReturnValueOnce(of(accounts));

    createPage();

    const retryButton = fixture.nativeElement.querySelector(
      '.error-state button',
    ) as HTMLButtonElement;
    retryButton.click();
    fixture.detectChanges();

    expect(accountApi.findAll).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Conta Walter');
  });

  it('should show an empty state and navigate to creation', () => {
    accountApi.findAll.mockReturnValue(of([]));

    createPage();

    expect(fixture.nativeElement.textContent).toContain('Nenhuma conta cadastrada');

    const createButton = fixture.nativeElement.querySelector(
      'app-empty-state app-button button',
    ) as HTMLButtonElement;
    createButton.click();

    expect(router.navigate).toHaveBeenCalledWith(['/accounts/new']);
  });
});
