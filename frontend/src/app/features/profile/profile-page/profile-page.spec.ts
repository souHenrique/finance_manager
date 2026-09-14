import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProfilePage } from './profile-page';

describe('ProfilePage', () => {
  let fixture: ComponentFixture<ProfilePage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfilePage],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfilePage);
    fixture.detectChanges();
  });

  it('should render the profile heading and description', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Perfil');
    expect(element.querySelector('p')?.textContent).toContain(
      'Consulte e atualize suas informações pessoais.',
    );
  });
});
