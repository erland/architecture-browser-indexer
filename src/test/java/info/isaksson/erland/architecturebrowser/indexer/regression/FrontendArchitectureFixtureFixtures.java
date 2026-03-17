package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.List;

final class FrontendArchitectureFixtureFixtures extends AbstractFrontendArchitectureFixtureTestSupport {
    private FrontendArchitectureFixtureFixtures() {}

    static List<TsFixtureFile> angularFiles() {
        return List.of(
            tsFile("src/app/orders/order.dto.ts","typescript","angular","""
                export interface OrderDto {
                  id: string;
                  status: string;
                }
                """, List.of(), List.of(interfaceDeclaration("""
                    export interface OrderDto {
                      id: string;
                      status: string;
                    }
                    """, "OrderDto", List.of(), List.of(propertySignature("id: string;", "id"), propertySignature("status: string;", "status")), List.of()))),
            tsFile("src/app/orders/orders.tokens.ts","typescript","angular","""
                export const ORDER_API = 'ORDER_API';
                export const ORDERS_CONFIG = 'ORDERS_CONFIG';
                """, List.of(), List.of()),
            tsFile("src/app/orders/orders.api.ts","typescript","angular","""
                import type { OrderDto } from './order.dto';

                export class OrdersApi {
                  fetchOrders(): OrderDto { throw new Error('noop'); }
                }
                """, List.of("import type { OrderDto } from './order.dto';"), List.of(classDeclaration("""
                    export class OrdersApi {
                      fetchOrders(): OrderDto { throw new Error('noop'); }
                    }
                    """, "OrdersApi", List.of(), null, List.of(), List.of(methodDefinition("fetchOrders(): OrderDto { throw new Error('noop'); }", "fetchOrders", "OrderDto"))))),
            tsFile("src/app/shared/shared-card.component.ts","typescript","angular","""
                import { Component } from '@angular/core';

                @Component({
                  selector: 'shared-card',
                  standalone: true,
                  template: `<section class="card"><ng-content /></section>`
                })
                export class SharedCardComponent {}
                """, List.of("import { Component } from '@angular/core';"), List.of(classDeclaration("""
                    @Component({
                      selector: 'shared-card',
                      standalone: true,
                      template: `<section class="card"><ng-content /></section>`
                    })
                    export class SharedCardComponent {}
                    """, "SharedCardComponent", List.of("""
                        @Component({
                          selector: 'shared-card',
                          standalone: true,
                          template: `<section class="card"><ng-content /></section>`
                        })
                        """.strip()), null, List.of(), List.of()))),
            tsFile("src/app/orders/order-status.pipe.ts","typescript","angular","""
                import { Pipe } from '@angular/core';

                @Pipe({ name: 'orderStatus' })
                export class OrderStatusPipe {}
                """, List.of("import { Pipe } from '@angular/core';"), List.of(classDeclaration("@Pipe({ name: 'orderStatus' })\nexport class OrderStatusPipe {}", "OrderStatusPipe", List.of("@Pipe({ name: 'orderStatus' })"), null, List.of(), List.of()))),
            tsFile("src/app/orders/track-click.directive.ts","typescript","angular","""
                import { Directive } from '@angular/core';

                @Directive({ selector: '[appTrackClick]' })
                export class TrackClickDirective {}
                """, List.of("import { Directive } from '@angular/core';"), List.of(classDeclaration("@Directive({ selector: '[appTrackClick]' })\nexport class TrackClickDirective {}", "TrackClickDirective", List.of("@Directive({ selector: '[appTrackClick]' })"), null, List.of(), List.of()))),
            tsFile("src/app/orders/order-list.component.ts","typescript","angular","""
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
                """, List.of(
                    "import { Component, Inject } from '@angular/core';",
                    "import { SharedCardComponent } from '../shared/shared-card.component';",
                    "import { ORDER_API } from './orders.tokens';",
                    "import { OrdersApi } from './orders.api';",
                    "import { OrderStatusPipe } from './order-status.pipe';",
                    "import { TrackClickDirective } from './track-click.directive';"
                ), List.of(classDeclaration("""
                    @Component({
                      selector: 'app-order-list',
                      template: `<shared-card appTrackClick>{{ status | orderStatus }}</shared-card>`
                    })
                    export class OrderListComponent {
                      status: string;
                      constructor(@Inject(ORDER_API) private api: OrdersApi) {}
                    }
                    """, "OrderListComponent", List.of("""
                        @Component({
                          selector: 'app-order-list',
                          template: `<shared-card appTrackClick>{{ status | orderStatus }}</shared-card>`
                        })
                        """.strip()), null, List.of(), List.of(publicField("status: string;", "status"), methodDefinition("constructor(@Inject(ORDER_API) private api: OrdersApi) {}", "constructor", null))))),
            tsFile("src/app/orders/orders.routes.ts","typescript","angular","""
                import { OrderListComponent } from './order-list.component';

                export const ORDERS_ROUTES = [{ path: 'orders', component: OrderListComponent }];
                """, List.of("import { OrderListComponent } from './order-list.component';"), List.of()),
            tsFile("src/app/orders/orders.module.ts","typescript","angular","""
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
                """, List.of(
                    "import { NgModule } from '@angular/core';",
                    "import { OrderListComponent } from './order-list.component';",
                    "import { OrdersApi } from './orders.api';",
                    "import { ORDER_API } from './orders.tokens';",
                    "import { SharedCardComponent } from '../shared/shared-card.component';",
                    "import { OrderStatusPipe } from './order-status.pipe';",
                    "import { TrackClickDirective } from './track-click.directive';"
                ), List.of(classDeclaration("""
                    @NgModule({
                      declarations: [OrderListComponent, OrderStatusPipe, TrackClickDirective],
                      imports: [SharedCardComponent],
                      providers: [{ provide: ORDER_API, useClass: OrdersApi }],
                      exports: [OrderListComponent]
                    })
                    export class OrdersModule {}
                    """, "OrdersModule", List.of("""
                        @NgModule({
                          declarations: [OrderListComponent, OrderStatusPipe, TrackClickDirective],
                          imports: [SharedCardComponent],
                          providers: [{ provide: ORDER_API, useClass: OrdersApi }],
                          exports: [OrderListComponent]
                        })
                        """.strip()), null, List.of(), List.of())))
        );
    }

    static List<TsFixtureFile> reactFiles() {
        return List.of(
            tsFile("src/domain/OrderDto.ts","typescript","react","""
                export interface OrderDto {
                  id: string;
                  total: number;
                }
                """, List.of(), List.of(interfaceDeclaration("""
                    export interface OrderDto {
                      id: string;
                      total: number;
                    }
                    """, "OrderDto", List.of(), List.of(propertySignature("id: string;", "id"), propertySignature("total: number;", "total")), List.of()))),
            tsFile("src/hooks/useOrdersQuery.ts","typescript","react","""
                import { useEffect, useState } from 'react';
                import type { OrderDto } from '../domain/OrderDto';

                export function useOrdersQuery(): OrderDto[] {
                  const [orders, setOrders] = useState<OrderDto[]>([]);
                  useEffect(() => { setOrders([]); }, []);
                  return orders;
                }
                """, List.of("import { useEffect, useState } from 'react';", "import type { OrderDto } from '../domain/OrderDto';"), List.of(functionDeclaration("""
                    export function useOrdersQuery(): OrderDto[] {
                      const [orders, setOrders] = useState<OrderDto[]>([]);
                      useEffect(() => { setOrders([]); }, []);
                      return orders;
                    }
                    """, "useOrdersQuery"))),
            tsFile("src/context/OrdersContext.tsx","typescript","react","""
                import React, { createContext } from 'react';
                import type { OrderDto } from '../domain/OrderDto';

                export const OrdersContext = createContext<OrderDto[] | null>(null);
                """, List.of("import React, { createContext } from 'react';", "import type { OrderDto } from '../domain/OrderDto';"), List.of()),
            tsFile("src/components/OrdersTable.tsx","typescript","react","""
                import React from 'react';
                import type { OrderDto } from '../domain/OrderDto';

                export function OrdersTable(props: { orders: OrderDto[] }) { return <table />; }
                """, List.of("import React from 'react';", "import type { OrderDto } from '../domain/OrderDto';"), List.of(functionDeclaration("export function OrdersTable(props: { orders: OrderDto[] }) { return <table />; }", "OrdersTable"))),
            tsFile("src/context/OrdersProvider.tsx","typescript","react","""
                import React from 'react';
                import { OrdersContext } from './OrdersContext';
                import { useOrdersQuery } from '../hooks/useOrdersQuery';

                export function OrdersProvider() {
                  const orders = useOrdersQuery();
                  return <OrdersContext.Provider value={orders}><section /></OrdersContext.Provider>;
                }
                """, List.of("import React from 'react';", "import { OrdersContext } from './OrdersContext';", "import { useOrdersQuery } from '../hooks/useOrdersQuery';"), List.of(functionDeclaration("""
                    export function OrdersProvider() {
                      const orders = useOrdersQuery();
                      return <OrdersContext.Provider value={orders}><section /></OrdersContext.Provider>;
                    }
                    """, "OrdersProvider"))),
            tsFile("src/pages/OrdersPage.tsx","typescript","react","""
                import React, { useContext } from 'react';
                import { OrdersContext } from '../context/OrdersContext';
                import { OrdersTable } from '../components/OrdersTable';

                export function OrdersPage() {
                  const orders = useContext(OrdersContext) ?? [];
                  return <main><OrdersTable orders={orders} /></main>;
                }
                """, List.of("import React, { useContext } from 'react';", "import { OrdersContext } from '../context/OrdersContext';", "import { OrdersTable } from '../components/OrdersTable';"), List.of(functionDeclaration("""
                    export function OrdersPage() {
                      const orders = useContext(OrdersContext) ?? [];
                      return <main><OrdersTable orders={orders} /></main>;
                    }
                    """, "OrdersPage"))),
            tsFile("src/routes/AppRoutes.tsx","typescript","react","""
                import React from 'react';
                import { Routes, Route } from 'react-router-dom';
                import { OrdersPage } from '../pages/OrdersPage';

                export function AppRoutes() {
                  return <Routes><Route path="orders" element={<OrdersPage />} /></Routes>;
                }
                """, List.of("import React from 'react';", "import { Routes, Route } from 'react-router-dom';", "import { OrdersPage } from '../pages/OrdersPage';"), List.of(functionDeclaration("""
                    export function AppRoutes() {
                      return <Routes><Route path="orders" element={<OrdersPage />} /></Routes>;
                    }
                    """, "AppRoutes")))
        );
    }
}
