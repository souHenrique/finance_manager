import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Badge } from './badge';

describe('Badge', () => {
  let fixture: ComponentFixture<Badge>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Badge],
    }).compileComponents();

    fixture = TestBed.createComponent(Badge);
    fixture.componentRef.setInput('label', 'Concluída');
    fixture.detectChanges();
  });

  it('should render the required label', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Concluída');
  });

  it('should use the neutral tone by default', () => {
    const badge = fixture.nativeElement.querySelector(
      '.badge',
    ) as HTMLElement;

    expect(badge.classList).toContain('badge--neutral');
  });

  it('should apply the configured tone', () => {
    fixture.componentRef.setInput('tone', 'success');
    fixture.detectChanges();

    const badge = fixture.nativeElement.querySelector(
      '.badge',
    ) as HTMLElement;

    expect(badge.classList).toContain('badge--success');
  });

  it('should render a visible symbol in addition to color', () => {
    fixture.componentRef.setInput('symbol', '✓');
    fixture.detectChanges();

    const symbol = fixture.nativeElement.querySelector(
      '[aria-hidden="true"]',
    ) as HTMLElement;

    expect(symbol.textContent?.trim()).toBe('✓');
  });
});
