import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import type { FeedbackTone } from '../types/feedback-tone';

@Component({
  imports: [],
  selector: 'app-alert',
  styleUrl: './alert.scss',
  templateUrl: './alert.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Alert {
  readonly tone = input<FeedbackTone>('info');
  readonly title = input<string>();
  readonly message = input.required<string>();
  readonly dismissible = input(false);

  readonly dismissed = output<void>();
}
