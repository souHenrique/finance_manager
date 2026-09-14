import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-form-field',
  styleUrl: './form-field.scss',
  templateUrl: './form-field.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormField {
  readonly label = input.required<string>();
  readonly controlId = input.required<string>();
  readonly hint = input<string>();
  readonly error = input<string>();
  readonly required = input(false);
}
