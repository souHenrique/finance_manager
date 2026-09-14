import {
  ChangeDetectionStrategy,
  Component,
  input,
} from '@angular/core';
import type { FeedbackTone } from '../types/feedback-tone';

@Component({
  imports: [],
  selector: 'app-badge',
  styleUrl: './badge.scss',
  templateUrl: './badge.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Badge {
  readonly label = input.required<string>();
  readonly tone = input<FeedbackTone>('neutral');
  readonly symbol = input('•');
}
