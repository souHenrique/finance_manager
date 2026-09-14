import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { SelectDirective } from './select';

@Component({
  imports: [ReactiveFormsModule, SelectDirective],
  template: `
    <label for="category">Categoria</label>

    <select
      appSelect
      id="category"
      [formControl]="control"
      aria-describedby="category-hint"
    >
      <option value="">Selecione</option>
      <option value="food">Alimentação</option>
      <option value="transport">Transporte</option>
    </select>

    <p id="category-hint">Selecione uma categoria</p>
  `,
})
class SelectTestHost {
  readonly control = new FormControl('', {
    nonNullable: true,
  });
}

describe('SelectDirective', () => {
  let fixture: ComponentFixture<SelectTestHost>;
  let component: SelectTestHost;
  let select: HTMLSelectElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SelectTestHost],
    }).compileComponents();

    fixture = TestBed.createComponent(SelectTestHost);
    component = fixture.componentInstance;
    fixture.detectChanges();

    select = fixture.nativeElement.querySelector(
      'select',
    ) as HTMLSelectElement;
  });

  it('should apply the design system classes', () => {
    expect(select.classList).toContain('ui-control');
    expect(select.classList).toContain('ui-control--select');
  });

  it('should render the native options', () => {
    expect(select.options).toHaveLength(3);
    expect(select.options[1]?.textContent).toBe('Alimentação');
    expect(select.options[2]?.textContent).toBe('Transporte');
  });

  it('should update the select when FormControl changes', () => {
    component.control.setValue('transport');
    fixture.detectChanges();

    expect(select.value).toBe('transport');
  });

  it('should update FormControl when the user selects an option', () => {
    select.value = 'food';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(component.control.value).toBe('food');
  });

  it('should reflect the disabled FormControl state', () => {
    component.control.disable();
    fixture.detectChanges();

    expect(select.disabled).toBe(true);
  });

  it('should preserve accessibility attributes', () => {
    expect(select.id).toBe('category');
    expect(select.getAttribute('aria-describedby')).toBe(
      'category-hint',
    );
  });
});
