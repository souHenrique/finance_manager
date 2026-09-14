import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TextareaDirective } from './textarea';

@Component({
  imports: [ReactiveFormsModule, TextareaDirective],
  template: `
    <label for="notes">Observações</label>

    <textarea
      appTextarea
      id="notes"
      rows="4"
      maxlength="500"
      [formControl]="control"
      [attr.aria-invalid]="invalid()"
      aria-describedby="notes-hint"
    ></textarea>

    <p id="notes-hint">Máximo de 500 caracteres</p>
  `,
})
class TextareaTestHost {
  readonly control = new FormControl('Observação inicial', {
    nonNullable: true,
  });

  readonly invalid = signal(false);
}

describe('TextareaDirective', () => {
  let fixture: ComponentFixture<TextareaTestHost>;
  let component: TextareaTestHost;
  let textarea: HTMLTextAreaElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TextareaTestHost],
    }).compileComponents();

    fixture = TestBed.createComponent(TextareaTestHost);
    component = fixture.componentInstance;
    fixture.detectChanges();

    textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
  });

  it('should apply the design system classes', () => {
    expect(textarea.classList).toContain('ui-control');
    expect(textarea.classList).toContain('ui-control--textarea');
  });

  it('should receive the initial FormControl value', () => {
    expect(textarea.value).toBe('Observação inicial');
  });

  it('should update the textarea when FormControl changes', () => {
    component.control.setValue('Observação atualizada');
    fixture.detectChanges();

    expect(textarea.value).toBe('Observação atualizada');
  });

  it('should update FormControl when the user types', () => {
    textarea.value = 'Texto digitado pelo usuário';
    textarea.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.control.value).toBe('Texto digitado pelo usuário');
  });

  it('should reflect the disabled FormControl state', () => {
    component.control.disable();
    fixture.detectChanges();

    expect(textarea.disabled).toBe(true);
  });

  it('should preserve native textarea attributes', () => {
    expect(textarea.id).toBe('notes');
    expect(textarea.rows).toBe(4);
    expect(textarea.maxLength).toBe(500);
    expect(textarea.getAttribute('aria-describedby')).toBe('notes-hint');
  });

  it('should reflect the invalid accessibility state', () => {
    component.invalid.set(true);
    fixture.detectChanges();

    expect(textarea.getAttribute('aria-invalid')).toBe('true');
  });
});
