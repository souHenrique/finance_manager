import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { InputDirective } from './input';

@Component({
  imports: [ReactiveFormsModule, InputDirective],
  template: `
    <label for="description">Descrição</label>

    <input
      appInput
      id="description"
      type="text"
      [formControl]="control"
      [attr.aria-invalid]="invalid"
      aria-describedby="description-hint"
    />

    <p id="description-hint">Informe uma descrição</p>
  `,
})
class InputTestHost {
  readonly control = new FormControl('Descrição inicial', {
    nonNullable: true,
  });

  invalid = false;
}

describe('InputDirective', () => {
  let fixture: ComponentFixture<InputTestHost>;
  let component: InputTestHost;
  let input: HTMLInputElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InputTestHost],
    }).compileComponents();

    fixture = TestBed.createComponent(InputTestHost);
    component = fixture.componentInstance;
    fixture.detectChanges();

    input = fixture.nativeElement.querySelector(
      'input',
    ) as HTMLInputElement;
  });

  it('should apply the design system classes', () => {
    expect(input.classList).toContain('ui-control');
    expect(input.classList).toContain('ui-control--input');
  });

  it('should receive the initial FormControl value', () => {
    expect(input.value).toBe('Descrição inicial');
  });

  it('should update the input when FormControl changes', () => {
    component.control.setValue('Nova descrição');
    fixture.detectChanges();

    expect(input.value).toBe('Nova descrição');
  });

  it('should update FormControl when the user types', () => {
    input.value = 'Descrição digitada';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.control.value).toBe('Descrição digitada');
  });

  it('should reflect the disabled FormControl state', () => {
    component.control.disable();
    fixture.detectChanges();

    expect(input.disabled).toBe(true);
  });

  it('should preserve accessibility attributes', () => {
    component.invalid = true;
    fixture.detectChanges();

    expect(input.getAttribute('aria-invalid')).toBe('true');
    expect(input.getAttribute('aria-describedby')).toBe(
      'description-hint',
    );
  });

  it('should preserve native input attributes', () => {
    expect(input.id).toBe('description');
    expect(input.type).toBe('text');
  });
});
