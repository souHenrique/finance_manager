import {
  ChangeDetectionStrategy,
  Component,
  input,
} from '@angular/core';

@Component({
  imports: [],
  selector: 'app-skeleton',
  styleUrl: './skeleton.scss',
  templateUrl: './skeleton.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Skeleton {
  readonly width = input('100%');
  readonly height = input('1rem');
  readonly radius = input('var(--radius-md)');
}
