import { Component, Inject } from '@angular/core';
import { SharedCardComponent } from '../shared/shared-card.component';
import { ORDER_API } from './orders.tokens';
import { OrdersApi } from './orders.api';
import { OrderStatusPipe } from './order-status.pipe';
import { TrackClickDirective } from './track-click.directive';

@Component({
  selector: 'app-order-list',
  template: `<shared-card appTrackClick>{{ status | orderStatus }}</shared-card>`
})
export class OrderListComponent {
  status: string;
  constructor(@Inject(ORDER_API) private api: OrdersApi) {}
}
