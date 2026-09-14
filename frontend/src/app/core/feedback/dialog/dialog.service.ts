import { Dialog } from '@angular/cdk/dialog';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConfirmDialog, ConfirmDialogData } from './confirm-dialog';

@Injectable({ providedIn: 'root' })
export class AppDialogService {
  private readonly dialog = inject(Dialog);
  private nextId = 0;

  confirm(options: Omit<ConfirmDialogData, 'id'>): Observable<boolean | undefined> {
    const id = `confirm-dialog-${++this.nextId}`;

    return this.dialog.open<boolean, ConfirmDialogData>(ConfirmDialog, {
      data: { ...options, id },
      role: options.danger ? 'alertdialog' : 'dialog',
      ariaModal: true,
      ariaLabelledBy: `${id}-title`,
      ariaDescribedBy: `${id}-description`,
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      disableClose: false,
      panelClass: 'app-dialog-panel',
      backdropClass: 'app-dialog-backdrop',
      width: 'min(32rem, calc(100vw - 2rem))',
      maxHeight: 'calc(100vh - 2rem)',
    }).closed;
  }
}
