import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Shell } from './shell';

describe('Shell', () => {
  let fixture: ComponentFixture<Shell>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Shell],
    }).compileComponents();

    fixture = TestBed.createComponent(Shell);
    await fixture.whenStable();
  });

  it('should render the application title and routed content area', () => {
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Finance Manager');
    expect(element.querySelector('main router-outlet')).not.toBeNull();
  });
});
