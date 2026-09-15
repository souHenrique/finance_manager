export type AccountType = 'CHECKING' | 'SAVINGS' | 'WALLET' | 'DIGITAL_ACCOUNT' | 'OTHER';

export type AccountStatus = 'ACTIVE' | 'INACTIVE';

export interface Account {
  id: string;
  name: string;
  type: AccountType;
  institution: string | null;
  initialBalance: number;
  currentBalance: number;
  status: AccountStatus;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAccountRequest {
  name: string;
  type: AccountType;
  institution: string | null;
  initialBalance: number;
}

export interface UpdateAccountRequest {
  name?: string;
  type?: AccountType;
  institution?: string;
}

export interface UpdateAccountStatusRequest {
  status: AccountStatus;
}
