import { Component, input, output } from '@angular/core';

import { Category } from '../../models/category.models';
import { CategoryTreeNode } from '../../models/category-tree.models';
import { Badge } from '../../../../shared/ui/badge/badge';
import { Button } from '../../../../shared/ui/button/button';

@Component({
  selector: 'app-category-tree',
  imports: [CategoryTreeComponent, Badge, Button],
  templateUrl: './category-tree.html',
  styleUrl: './category-tree.scss',
})
export class CategoryTreeComponent {
  readonly nodes = input.required<CategoryTreeNode[]>();

  readonly createChild = output<Category>();
  readonly edit = output<Category>();
}
