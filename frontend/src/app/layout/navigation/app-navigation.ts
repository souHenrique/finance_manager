export interface NavigationItem {
  readonly label: string;
  readonly route: string;
  readonly symbol: string;
  readonly exact?: boolean;
}

export const APP_NAVIGATION: readonly NavigationItem[] = [
  {
    label: 'Dashboard',
    route: '/dashboard',
    symbol: '⌂',
    exact: true,
  },
  {
    label: 'Transações',
    route: '/transactions',
    symbol: '↕',
  },
  {
    label: 'Nova transferência',
    route: '/transfers/new',
    symbol: '⇄',
    exact: true,
  },
  {
    label: 'Contas',
    route: '/accounts',
    symbol: '▣',
  },
  {
    label: 'Categorias',
    route: '/categories',
    symbol: '◈',
  },
  {
    label: 'Cartões',
    route: '/credit-cards',
    symbol: '▤',
  },
  {
    label: 'Faturas',
    route: '/invoices',
    symbol: '▧',
  },
  {
    label: 'Orçamentos',
    route: '/budgets',
    symbol: '◎',
  },
  {
    label: 'Relatórios',
    route: '/reports',
    symbol: '▥',
  },
  {
    label: 'Perfil',
    route: '/profile',
    symbol: '●',
  },
];
