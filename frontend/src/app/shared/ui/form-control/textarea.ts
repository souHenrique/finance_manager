import { Directive } from '@angular/core';

@Directive({
  selector: 'textarea[appTextarea]',
  host: {
    class: 'ui-control ui-control--textarea',
  },
})
export class TextareaDirective {}
