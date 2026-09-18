import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from '../../features/auth/services/auth.service';
import { Header } from './header';

describe('Header', () => {
  let fixture: ComponentFixture<Header>;
  let auth: { logout: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    auth = { logout: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    }).compileComponents();

    fixture = TestBed.createComponent(Header);
    fixture.detectChanges();
  });

  it('should render navigation and accessibility controls', () => {
    const element = fixture.nativeElement as HTMLElement;
    const menuButton = element.querySelector('.header__menu-button') as HTMLButtonElement;

    expect(element.querySelector('.header__brand')?.textContent).toContain('Finance Manager');
    expect(element.querySelector('.header__profile')?.textContent).toContain('Perfil');
    expect(element.querySelector('.header__skip-link')).not.toBeNull();
    expect(menuButton.getAttribute('aria-controls')).toBe('app-sidebar');
    expect(menuButton.getAttribute('aria-expanded')).toBe('false');
  });

  it('should emit menuToggle when the menu button is clicked', () => {
    const emitSpy = vi.spyOn(fixture.componentInstance.menuToggle, 'emit');
    const menuButton = fixture.nativeElement.querySelector(
      '.header__menu-button',
    ) as HTMLButtonElement;

    menuButton.click();

    expect(emitSpy).toHaveBeenCalledOnce();
  });

  it('should emit skipToContent without changing the navigation target', () => {
    const emitSpy = vi.spyOn(fixture.componentInstance.skipToContent, 'emit');
    const skipLink = fixture.nativeElement.querySelector('.header__skip-link') as HTMLAnchorElement;
    const event = new MouseEvent('click', { cancelable: true });

    skipLink.dispatchEvent(event);

    expect(event.defaultPrevented).toBe(true);
    expect(emitSpy).toHaveBeenCalledOnce();
  });

  it('should expose the open menu state', () => {
    fixture.componentRef.setInput('menuOpen', true);
    fixture.detectChanges();

    const menuButton = fixture.nativeElement.querySelector(
      '.header__menu-button',
    ) as HTMLButtonElement;

    expect(menuButton.getAttribute('aria-expanded')).toBe('true');
  });

  it('should logout when the user clicks the logout button', () => {
    const logoutButton = fixture.nativeElement.querySelector(
      '.header__logout',
    ) as HTMLButtonElement;

    logoutButton.click();

    expect(auth.logout).toHaveBeenCalledOnce();
  });
});
