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
    expect(element.querySelector('.header__brand')?.textContent).toContain('Finance Manager');
    expect(element.querySelector('main router-outlet')).not.toBeNull();
  });
});
