import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NotFoundPage } from './not-found-page';

describe('NotFoundPage', () => {
  let fixture: ComponentFixture<NotFoundPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotFoundPage],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(NotFoundPage);
    fixture.detectChanges();
  });

  it('should explain the 404 error and provide a dashboard link', () => {
    const element = fixture.nativeElement as HTMLElement;
    const dashboardLink = element.querySelector('a') as HTMLAnchorElement;

    expect(element.textContent).toContain('Erro 404');
    expect(element.querySelector('h1')?.textContent).toContain('Página não encontrada');
    expect(dashboardLink.textContent).toContain('Voltar ao Dashboard');
    expect(dashboardLink.getAttribute('href')).toBe('/dashboard');
  });
});
