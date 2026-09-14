import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Skeleton } from './skeleton';

describe('Skeleton', () => {
  let fixture: ComponentFixture<Skeleton>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Skeleton],
    }).compileComponents();

    fixture = TestBed.createComponent(Skeleton);
    fixture.detectChanges();
  });

  it('should be hidden from assistive technology', () => {
    const skeleton = fixture.nativeElement.querySelector(
      '.skeleton',
    ) as HTMLElement;

    expect(skeleton.getAttribute('aria-hidden')).toBe('true');
  });

  it('should use the default dimensions', () => {
    const skeleton = fixture.nativeElement.querySelector(
      '.skeleton',
    ) as HTMLElement;

    expect(skeleton.style.width).toBe('100%');
    expect(skeleton.style.height).toBe('1rem');
  });

  it('should apply custom dimensions and radius', () => {
    fixture.componentRef.setInput('width', '12rem');
    fixture.componentRef.setInput('height', '2rem');
    fixture.componentRef.setInput('radius', '0.5rem');
    fixture.detectChanges();

    const skeleton = fixture.nativeElement.querySelector(
      '.skeleton',
    ) as HTMLElement;

    expect(skeleton.style.width).toBe('12rem');
    expect(skeleton.style.height).toBe('2rem');
    expect(skeleton.style.borderRadius).toBe('0.5rem');
  });
});
