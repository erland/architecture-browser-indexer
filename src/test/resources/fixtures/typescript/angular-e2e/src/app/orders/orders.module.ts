import { NgModule } from '@angular/core';
import { OrderListComponent } from './order-list.component';
import { OrdersApi } from './orders.api';
import { ORDER_API } from './orders.tokens';
import { SharedCardComponent } from '../shared/shared-card.component';
import { OrderStatusPipe } from './order-status.pipe';
import { TrackClickDirective } from './track-click.directive';

@NgModule({
  declarations: [OrderListComponent, OrderStatusPipe, TrackClickDirective],
  imports: [SharedCardComponent],
  providers: [{ provide: ORDER_API, useClass: OrdersApi }],
  exports: [OrderListComponent]
})
export class OrdersModule {}
