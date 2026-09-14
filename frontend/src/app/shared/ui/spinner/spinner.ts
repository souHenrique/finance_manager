import {
  ChangeDetectionStrategy,
  Component,
  input,
} from '@angular/core';

@Component({
  imports: [],
  selector: 'app-spinner',
  styleUrl: './spinner.scss',
  templateUrl: './spinner.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Spinner {
  readonly size = input<'sm' | 'md' | 'lg'>('md');
  readonly label = input('Carregando');
  readonly decorative = input(false);
}

