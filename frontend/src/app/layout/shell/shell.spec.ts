import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Shell } from './shell';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../features/auth/services/auth.service';

describe('Shell', () => {
  let fixture: ComponentFixture<Shell>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            logout: vi.fn(),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Shell);
    await fixture.whenStable();
  });

  it('should render header, sidebar and routed content area', () => {
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('app-header')).not.toBeNull();
    expect(element.querySelector('app-sidebar')).not.toBeNull();
    expect(element.querySelector('.header__brand')?.textContent).toContain('Nummo');
    expect(element.querySelector('main router-outlet')).not.toBeNull();
  });

  it('should focus the main content when the skip link is used', () => {
    fixture.detectChanges();

    const skipLink = fixture.nativeElement.querySelector('.header__skip-link') as HTMLAnchorElement;
    const mainContent = fixture.nativeElement.querySelector('#main-content') as HTMLElement;

    skipLink.click();

    expect(document.activeElement).toBe(mainContent);
  });
});
