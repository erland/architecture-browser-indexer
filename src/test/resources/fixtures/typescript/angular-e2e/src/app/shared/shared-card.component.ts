import { Component } from '@angular/core';

@Component({
  selector: 'shared-card',
  standalone: true,
  template: `<section class="card"><ng-content /></section>`
})
export class SharedCardComponent {}
