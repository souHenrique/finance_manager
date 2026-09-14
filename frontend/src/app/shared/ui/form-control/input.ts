import { Directive } from '@angular/core';

@Directive({
  selector: 'input[appInput]',
  host: {
    class: 'ui-control ui-control--input',
  },
})
export class InputDirective {}
