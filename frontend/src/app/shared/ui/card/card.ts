import { ChangeDetectionStrategy, Component, input } from '@angular/core';
@Component({
  imports: [],
  selector: 'app-card',
  styleUrl: './card.scss',
  templateUrl: './card.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Card {
  readonly title = input<string>();
  readonly subtitle = input<string>();
}
