import { Category, CategoryType } from './category.models';
import { CategoryTreeNode } from './category-tree.models';

export function buildCategoryTree(
  categories: Category[],
  type: CategoryType,
): CategoryTreeNode[] {
  const categoriesOfType = categories.filter((category) => category.type === type);

  const nodes = new Map<string, CategoryTreeNode>(
    categoriesOfType.map((category) => [
      category.id,
      {
        ...category,
        children: [],
      },
    ]),
  );

  const roots: CategoryTreeNode[] = [];

  for (const node of nodes.values()) {
    if (!node.parentCategoryId) {
      roots.push(node);
      continue;
    }

    const parent = nodes.get(node.parentCategoryId);

    if (parent) {
      parent.children.push(node);
    } else {
      roots.push(node);
    }
  }

  return sortTreeByName(roots);
}

function sortTreeByName(nodes: CategoryTreeNode[]): CategoryTreeNode[] {
  return nodes
    .sort((first, second) => first.name.localeCompare(second.name, 'pt-BR'))
    .map((node) => ({
      ...node,
      children: sortTreeByName(node.children),
    }));
}
