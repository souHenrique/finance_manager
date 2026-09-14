import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Header } from './header';

describe('Header', () => {
  let fixture: ComponentFixture<Header>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [provideRouter([])],
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

  it('should expose the open menu state', () => {
    fixture.componentRef.setInput('menuOpen', true);
    fixture.detectChanges();

    const menuButton = fixture.nativeElement.querySelector(
      '.header__menu-button',
    ) as HTMLButtonElement;

    expect(menuButton.getAttribute('aria-expanded')).toBe('true');
  });
});
