import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Spinner } from './spinner';

describe('Spinner', () => {
  let fixture: ComponentFixture<Spinner>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Spinner],
    }).compileComponents();

    fixture = TestBed.createComponent(Spinner);
    fixture.detectChanges();
  });

  it('should be announced as loading by default', () => {
    const spinner = fixture.nativeElement.querySelector(
      '.spinner',
    ) as HTMLElement;

    expect(spinner.getAttribute('role')).toBe('status');
    expect(spinner.getAttribute('aria-label')).toBe('Carregando');
    expect(spinner.getAttribute('aria-hidden')).toBeNull();
  });

  it('should be hidden from assistive technology when decorative', () => {
    fixture.componentRef.setInput('decorative', true);
    fixture.detectChanges();

    const spinner = fixture.nativeElement.querySelector(
      '.spinner',
    ) as HTMLElement;

    expect(spinner.getAttribute('role')).toBeNull();
    expect(spinner.getAttribute('aria-label')).toBeNull();
    expect(spinner.getAttribute('aria-hidden')).toBe('true');
  });

  it('should apply the configured size', () => {
    fixture.componentRef.setInput('size', 'lg');
    fixture.detectChanges();

    const spinner = fixture.nativeElement.querySelector(
      '.spinner',
    ) as HTMLElement;

    expect(spinner.classList).toContain('spinner--lg');
  });

  it('should use the configured accessible label', () => {
    fixture.componentRef.setInput(
      'label',
      'Carregando transações',
    );
    fixture.detectChanges();

    const spinner = fixture.nativeElement.querySelector(
      '.spinner',
    ) as HTMLElement;

    expect(spinner.getAttribute('aria-label')).toBe(
      'Carregando transações',
    );
  });
});
