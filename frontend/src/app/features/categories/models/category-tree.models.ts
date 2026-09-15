import { Category } from './category.models';

export interface CategoryTreeNode extends Category {
  children: CategoryTreeNode[];
}
