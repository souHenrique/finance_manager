import { AccountType } from './account.models';

export interface AccountTypeOption {
  value: AccountType;
  label: string;
}

export const ACCOUNT_TYPE_OPTIONS: AccountTypeOption[] = [
  {
    value: 'CHECKING',
    label: 'Conta corrente',
  },
  {
    value: 'SAVINGS',
    label: 'Poupança',
  },
  {
    value: 'WALLET',
    label: 'Carteira',
  },
  {
    value: 'DIGITAL_ACCOUNT',
    label: 'Conta digital',
  },
  {
    value: 'OTHER',
    label: 'Outra',
  },
];
