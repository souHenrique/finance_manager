export interface CreateTransferRequest {
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  date: string;
  description: string;
}
