import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Button } from './button';

describe('Button', () => {
  let fixture: ComponentFixture<Button>;
  let component: Button;
  let nativeButton: HTMLButtonElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Button],
    }).compileComponents();

    fixture = TestBed.createComponent(Button);
    component = fixture.componentInstance;
    fixture.detectChanges();

    nativeButton = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
  });

  it('should use safe default values', () => {
    expect(nativeButton.type).toBe('button');
    expect(nativeButton.disabled).toBe(false);
    expect(nativeButton.classList).toContain('button--primary');
    expect(nativeButton.classList).toContain('button--md');
  });

  it('should apply the selected variant and size', () => {
    fixture.componentRef.setInput('variant', 'danger');
    fixture.componentRef.setInput('size', 'lg');
    fixture.detectChanges();

    expect(nativeButton.classList).toContain('button--danger');
    expect(nativeButton.classList).toContain('button--lg');
  });

  it('should emit pressed when clicked', () => {
    const pressed = vi.fn();

    component.pressed.subscribe(pressed);

    nativeButton.click();

    expect(pressed).toHaveBeenCalledOnce();
  });

  it('should not emit pressed when disabled', () => {
    const pressed = vi.fn();

    component.pressed.subscribe(pressed);
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    nativeButton.click();

    expect(nativeButton.disabled).toBe(true);
    expect(pressed).not.toHaveBeenCalled();
  });

  it('should disable the button while loading', () => {
    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();

    expect(nativeButton.disabled).toBe(true);
    expect(nativeButton.getAttribute('aria-busy')).toBe('true');
    expect(fixture.nativeElement.querySelector('app-spinner')).not.toBeNull();
  });

  it('should use submit type when configured', () => {
    fixture.componentRef.setInput('type', 'submit');
    fixture.detectChanges();

    expect(nativeButton.type).toBe('submit');
  });
});
