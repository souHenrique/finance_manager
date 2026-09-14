import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EmptyState } from './empty-state';

@Component({
  imports: [EmptyState],
  template: `
    <app-empty-state
      title="Nenhuma transação encontrada"
      description="Crie uma transação para começar."
    >
      <span empty-state-icon>○</span>

      <button empty-state-actions type="button">
        Criar transação
      </button>
    </app-empty-state>
  `,
})
class EmptyStateTestHost {}

describe('EmptyState', () => {
  let fixture: ComponentFixture<EmptyStateTestHost>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmptyStateTestHost],
    }).compileComponents();

    fixture = TestBed.createComponent(EmptyStateTestHost);
    fixture.detectChanges();
  });

  it('should render title and description', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h2')?.textContent).toContain(
      'Nenhuma transação encontrada',
    );

    expect(element.querySelector('p')?.textContent).toContain(
      'Crie uma transação para começar.',
    );
  });

  it('should project the decorative icon', () => {
    const icon = fixture.nativeElement.querySelector(
      '[empty-state-icon]',
    ) as HTMLElement;

    expect(icon).not.toBeNull();
    expect(icon.textContent).toContain('○');

    const iconContainer = fixture.nativeElement.querySelector(
      '.empty-state__icon',
    ) as HTMLElement;

    expect(iconContainer.getAttribute('aria-hidden')).toBe('true');
  });

  it('should project the action', () => {
    const action = fixture.nativeElement.querySelector(
      '[empty-state-actions]',
    ) as HTMLButtonElement;

    expect(action).not.toBeNull();
    expect(action.textContent).toContain('Criar transação');
  });
});
