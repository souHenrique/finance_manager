import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CategoryTreeNode } from '../../models/category-tree.models';
import { CategoryTreeComponent } from './category-tree';

describe('CategoryTreeComponent', () => {
  let fixture: ComponentFixture<CategoryTreeComponent>;
  let component: CategoryTreeComponent;

  const child: CategoryTreeNode = {
    id: 'ac36fcee-4401-45ec-80d1-bb9c9464b833',
    name: 'Jesse Pinkman',
    type: 'EXPENSE',
    parentCategoryId: 'a1499b0c-2cca-4eb1-81c1-b82c6c599bd1',
    status: 'INACTIVE',
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
    children: [],
  };

  const parent: CategoryTreeNode = {
    id: 'a1499b0c-2cca-4eb1-81c1-b82c6c599bd1',
    name: 'Walter White',
    type: 'EXPENSE',
    parentCategoryId: null,
    status: 'ACTIVE',
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
    children: [child],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoryTreeComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoryTreeComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('nodes', [parent]);
    fixture.detectChanges();
  });

  it('should render parent and child categories', () => {
    const content = fixture.nativeElement.textContent as string;

    expect(content).toContain('Walter White');
    expect(content).toContain('Jesse Pinkman');
    expect(content).toContain('Categoria principal');
    expect(content).toContain('Subcategoria');
    expect(fixture.nativeElement.querySelectorAll('[role="treeitem"]')).toHaveLength(2);
    expect(fixture.nativeElement.querySelector('[aria-level="1"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[aria-level="2"]')).not.toBeNull();
  });

  it('should identify inactive categories with text', () => {
    expect(fixture.nativeElement.textContent).toContain('Inativa');
  });

  it('should emit the parent when creating a child category', () => {
    const createChild = vi.fn();
    component.createChild.subscribe(createChild);

    const buttons = fixture.nativeElement.querySelectorAll(
      'app-button button',
    ) as NodeListOf<HTMLButtonElement>;

    buttons[0].click();

    expect(createChild).toHaveBeenCalledWith(parent);
  });

  it('should emit the parent when editing it', () => {
    const edit = vi.fn();
    component.edit.subscribe(edit);

    const buttons = fixture.nativeElement.querySelectorAll(
      'app-button button',
    ) as NodeListOf<HTMLButtonElement>;

    buttons[1].click();

    expect(edit).toHaveBeenCalledWith(parent);
  });

  it('should propagate child actions through the recursive tree', () => {
    const edit = vi.fn();
    component.edit.subscribe(edit);

    const buttons = fixture.nativeElement.querySelectorAll(
      'app-button button',
    ) as NodeListOf<HTMLButtonElement>;

    buttons[3].click();

    expect(edit).toHaveBeenCalledWith(child);
  });
});
