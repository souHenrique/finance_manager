import { Dialog } from '@angular/cdk/dialog';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ConfirmDialog, ConfirmDialogData } from './confirm-dialog';
import { AppDialogService } from './dialog.service';

describe('AppDialogService', () => {
  let service: AppDialogService;
  let open: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    open = vi.fn().mockReturnValue({
      closed: of(true),
    });

    TestBed.configureTestingModule({
      providers: [
        AppDialogService,
        {
          provide: Dialog,
          useValue: {
            open,
          },
        },
      ],
    });

    service = TestBed.inject(AppDialogService);
  });

  it('should open an accessible destructive confirmation dialog', () => {
    const options: Omit<ConfirmDialogData, 'id'> = {
      title: 'Excluir orçamento',
      message: 'Essa ação não poderá ser desfeita.',
      confirmLabel: 'Excluir',
      cancelLabel: 'Cancelar',
      danger: true,
    };

    service.confirm(options);

    expect(open).toHaveBeenCalledWith(
      ConfirmDialog,
      expect.objectContaining({
        data: {
          ...options,
          id: 'confirm-dialog-1',
        },
        role: 'alertdialog',
        ariaModal: true,
        ariaLabelledBy: 'confirm-dialog-1-title',
        ariaDescribedBy: 'confirm-dialog-1-description',
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        disableClose: false,
        panelClass: 'app-dialog-panel',
        backdropClass: 'app-dialog-backdrop',
        width: 'min(32rem, calc(100vw - 2rem))',
        maxHeight: 'calc(100vh - 2rem)',
      }),
    );
  });

  it('should use dialog role for a non-destructive confirmation', () => {
    service.confirm({
      title: 'Confirmar alteração',
      message: 'Deseja salvar as alterações?',
      confirmLabel: 'Salvar',
      cancelLabel: 'Cancelar',
      danger: false,
    });

    expect(open).toHaveBeenCalledWith(
      ConfirmDialog,
      expect.objectContaining({
        role: 'dialog',
      }),
    );
  });

  it('should generate a unique identifier for each dialog', () => {
    const options: Omit<ConfirmDialogData, 'id'> = {
      title: 'Confirmação',
      message: 'Deseja continuar?',
      confirmLabel: 'Continuar',
      cancelLabel: 'Cancelar',
    };

    service.confirm(options);
    service.confirm(options);

    expect(open).toHaveBeenNthCalledWith(
      1,
      ConfirmDialog,
      expect.objectContaining({
        data: expect.objectContaining({
          id: 'confirm-dialog-1',
        }),
      }),
    );

    expect(open).toHaveBeenNthCalledWith(
      2,
      ConfirmDialog,
      expect.objectContaining({
        data: expect.objectContaining({
          id: 'confirm-dialog-2',
        }),
      }),
    );
  });

  it('should return the dialog closed observable', () => {
    const closed = of(false);

    open.mockReturnValue({
      closed,
    });

    const result = service.confirm({
      title: 'Cancelar operação',
      message: 'Deseja cancelar?',
      confirmLabel: 'Confirmar',
      cancelLabel: 'Voltar',
    });

    expect(result).toBe(closed);
  });
});
