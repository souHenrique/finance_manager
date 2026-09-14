import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { Button } from '../button/button';

@Component({
  selector: 'app-error-state',
  imports: [Button],
  styleUrl: './error-state.scss',
  templateUrl: './error-state.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorState {
  readonly title = input('Não foi possível carregar os dados');
  readonly message = input.required<string>();
  readonly retryable = input(true);

  readonly retry = output<void>();
}
