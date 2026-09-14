import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RegisterPage } from './register-page';

describe('RegisterPage', () => {
  let fixture: ComponentFixture<RegisterPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterPage],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterPage);
    fixture.detectChanges();
  });

  it('should render the registration heading and description', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Criar conta');
    expect(element.querySelector('p')?.textContent).toContain(
      'Cadastre-se para começar a organizar suas finanças.',
    );
  });
});
