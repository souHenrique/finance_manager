import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Alert } from './alert';

describe('Alert', () => {
  let fixture: ComponentFixture<Alert>;
  let component: Alert;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Alert],
    }).compileComponents();

    fixture = TestBed.createComponent(Alert);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('message', 'Não foi possível salvar');
    fixture.detectChanges();
  });

  it('should render the required message', () => {
    expect(fixture.nativeElement.textContent).toContain('Não foi possível salvar');
  });

  it('should render the optional title', () => {
    fixture.componentRef.setInput('title', 'Erro');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('strong')?.textContent).toContain('Erro');
  });

  it('should use status role for informational alerts', () => {
    const alert = fixture.nativeElement.querySelector('.alert') as HTMLElement;

    expect(alert.getAttribute('role')).toBe('status');
  });

  it('should use alert role for danger alerts', () => {
    fixture.componentRef.setInput('tone', 'danger');
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('.alert') as HTMLElement;

    expect(alert.getAttribute('role')).toBe('alert');
    expect(alert.classList).toContain('alert--danger');
  });

  it('should hide the close button by default', () => {
    expect(fixture.nativeElement.querySelector('button[aria-label="Fechar aviso"]')).toBeNull();
  });

  it('should emit dismissed when close is clicked', () => {
    const dismissed = vi.fn();

    component.dismissed.subscribe(dismissed);
    fixture.componentRef.setInput('dismissible', true);
    fixture.detectChanges();

    const closeButton = fixture.nativeElement.querySelector(
      'button[aria-label="Fechar aviso"]',
    ) as HTMLButtonElement;

    closeButton.click();

    expect(dismissed).toHaveBeenCalledOnce();
  });
});
