import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Card } from './card';

@Component({
  imports: [Card],
  template: `
    <app-card
      title="Resumo financeiro"
      subtitle="Dados de setembro"
    >
      <p class="projected-content">Saldo: R$ 1.000,00</p>

      <button
        card-actions
        class="projected-action"
        type="button"
      >
        Ver detalhes
      </button>
    </app-card>
  `,
})
class CardTestHost {}

describe('Card', () => {
  let fixture: ComponentFixture<CardTestHost>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CardTestHost],
    }).compileComponents();

    fixture = TestBed.createComponent(CardTestHost);
    fixture.detectChanges();
  });

  it('should render title and subtitle', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h2')?.textContent).toContain(
      'Resumo financeiro',
    );

    expect(element.querySelector('.card__header p')?.textContent).toContain(
      'Dados de setembro',
    );
  });

  it('should project the card content', () => {
    expect(
      fixture.nativeElement.querySelector('.projected-content')
        ?.textContent,
    ).toContain('Saldo: R$ 1.000,00');
  });

  it('should project actions into the header', () => {
    const header = fixture.nativeElement.querySelector(
      '.card__header',
    ) as HTMLElement;

    expect(header.querySelector('.projected-action')).not.toBeNull();
  });
});
