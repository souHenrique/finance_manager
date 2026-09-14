import { Directive } from '@angular/core';

@Directive({
  selector: 'select[appSelect]',
  host: {
    class: 'ui-control ui-control--select',
  },
})
export class SelectDirective {}
