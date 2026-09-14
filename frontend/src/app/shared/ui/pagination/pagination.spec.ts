import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Pagination } from './pagination';

describe('Pagination', () => {
  let fixture: ComponentFixture<Pagination>;
  let component: Pagination;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Pagination],
    }).compileComponents();

    fixture = TestBed.createComponent(Pagination);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('currentPage', 1);
    fixture.componentRef.setInput('totalPages', 3);
    fixture.detectChanges();
  });

  function buttons(): NodeListOf<HTMLButtonElement> {
    return fixture.nativeElement.querySelectorAll('button');
  }

  it('should render the current page summary', () => {
    expect(fixture.nativeElement.textContent).toContain(
      'Página 1 de 3',
    );
  });

  it('should disable previous on the first page', () => {
    expect(buttons()[0]?.disabled).toBe(true);
    expect(buttons()[1]?.disabled).toBe(false);
  });

  it('should disable next on the last page', () => {
    fixture.componentRef.setInput('currentPage', 3);
    fixture.detectChanges();

    expect(buttons()[0]?.disabled).toBe(false);
    expect(buttons()[1]?.disabled).toBe(true);
  });

  it('should emit the next page', () => {
    const pageChange = vi.fn();

    component.pageChange.subscribe(pageChange);

    buttons()[1]?.click();

    expect(pageChange).toHaveBeenCalledOnce();
    expect(pageChange).toHaveBeenCalledWith(2);
  });

  it('should emit the previous page', () => {
    const pageChange = vi.fn();

    fixture.componentRef.setInput('currentPage', 2);
    fixture.detectChanges();
    component.pageChange.subscribe(pageChange);

    buttons()[0]?.click();

    expect(pageChange).toHaveBeenCalledWith(1);
  });

  it('should disable navigation when the component is disabled', () => {
    const pageChange = vi.fn();

    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();
    component.pageChange.subscribe(pageChange);

    expect(buttons()[0]?.disabled).toBe(true);
    expect(buttons()[1]?.disabled).toBe(true);

    buttons()[1]?.click();

    expect(pageChange).not.toHaveBeenCalled();
  });

  it('should expose an accessible navigation label', () => {
    const navigation = fixture.nativeElement.querySelector(
      'nav',
    ) as HTMLElement;

    expect(navigation.getAttribute('aria-label')).toBe('Paginação');
  });
});
