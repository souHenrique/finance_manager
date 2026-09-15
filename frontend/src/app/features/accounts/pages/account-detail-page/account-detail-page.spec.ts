import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AppDialogService } from '../../../../core/feedback/dialog/dialog.service';
import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { AccountApiService } from '../../data-access/account-api.service';
import { Account } from '../../models/account.models';
import { AccountDetailPage } from './account-detail-page';

describe('AccountDetailPage', () => {
  let fixture: ComponentFixture<AccountDetailPage>;
  let component: AccountDetailPage;
  let accountApi: {
    findById: ReturnType<typeof vi.fn>;
    updateStatus: ReturnType<typeof vi.fn>;
  };
  let dialog: { confirm: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let toast: { show: ReturnType<typeof vi.fn> };

  const account: Account = {
    id: '6df2a8d8-c0c4-4a4c-92e0-e9e40350d3ba',
    name: 'Conta Skyler',
    type: 'CHECKING',
    institution: 'Banco Schrader',
    initialBalance: 1500,
    currentBalance: 1740.5,
    status: 'ACTIVE',
    version: 4,
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };

  beforeEach(async () => {
    accountApi = {
      findById: vi.fn().mockReturnValue(of(account)),
      updateStatus: vi.fn().mockReturnValue(of({ ...account, status: 'INACTIVE' })),
    };
    dialog = { confirm: vi.fn().mockReturnValue(of(true)) };
    router = { navigate: vi.fn().mockResolvedValue(true) };
    toast = { show: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [AccountDetailPage],
      providers: [
        { provide: AccountApiService, useValue: accountApi },
        { provide: AppDialogService, useValue: dialog },
        { provide: Router, useValue: router },
        { provide: ToastService, useValue: toast },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: account.id }),
            },
          },
        },
      ],
    }).compileComponents();
  });

  function createPage(): void {
    fixture = TestBed.createComponent(AccountDetailPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('should load and display account data from the route id', () => {
    createPage();

    const content = fixture.nativeElement.textContent as string;

    expect(accountApi.findById).toHaveBeenCalledWith(account.id);
    expect(content).toContain('Conta Skyler');
    expect(content).toContain('Conta corrente');
    expect(content).toContain('Ativa');
  });

  it('should show an error state when loading fails', () => {
    accountApi.findById.mockReturnValue(throwError(() => new Error('network')));

    createPage();

    expect(fixture.nativeElement.querySelector('app-error-state')).not.toBeNull();
  });

  it('should not update status when confirmation is cancelled', () => {
    dialog.confirm.mockReturnValue(of(false));

    createPage();
    component.confirmStatusChange();

    expect(dialog.confirm).toHaveBeenCalledOnce();
    expect(accountApi.updateStatus).not.toHaveBeenCalled();
    expect(toast.show).not.toHaveBeenCalled();
  });

  it('should inactivate an active account, show feedback, and reload it', () => {
    createPage();
    component.confirmStatusChange();

    expect(accountApi.updateStatus).toHaveBeenCalledWith(account.id, {
      status: 'INACTIVE',
    });
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Conta inativada',
      message: 'A conta foi inativada com sucesso.',
    });
    expect(accountApi.findById).toHaveBeenCalledTimes(2);
  });

  it('should activate an inactive account and reload it', () => {
    const inactiveAccount: Account = { ...account, status: 'INACTIVE' };
    accountApi.findById.mockReturnValue(of(inactiveAccount));
    accountApi.updateStatus.mockReturnValue(of({ ...inactiveAccount, status: 'ACTIVE' }));

    createPage();
    component.confirmStatusChange();

    expect(accountApi.updateStatus).toHaveBeenCalledWith(account.id, {
      status: 'ACTIVE',
    });
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Conta ativada',
      message: 'A conta está disponível novamente.',
    });
    expect(accountApi.findById).toHaveBeenCalledTimes(2);
  });

  it('should navigate to edit for the loaded account', () => {
    createPage();
    component.goToEdit();

    expect(router.navigate).toHaveBeenCalledWith(['/accounts', account.id, 'edit']);
  });
});
