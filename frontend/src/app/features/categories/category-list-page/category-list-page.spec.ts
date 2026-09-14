import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CategoryListPage } from './category-list-page';

describe('CategoryListPage', () => {
  let fixture: ComponentFixture<CategoryListPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoryListPage],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoryListPage);
    fixture.detectChanges();
  });

  it('should render the categories heading and description', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Categorias');
    expect(element.querySelector('p')?.textContent).toContain(
      'Organize suas receitas e despesas por categoria.',
    );
  });
});
