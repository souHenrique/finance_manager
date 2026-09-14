import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-empty-state',
  styleUrl: './empty-state.scss',
  templateUrl: './empty-state.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyState {
  readonly title = input.required<string>();
  readonly description = input<string>();
}
