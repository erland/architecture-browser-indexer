package info.isaksson.erland.architecturebrowser.indexer.regression;

import java.util.List;

final class TypeScriptArchitectureFixtureFixtures {
    private TypeScriptArchitectureFixtureFixtures() {}

    static List<AbstractTypeScriptArchitectureFixtureTestSupport.TsFixtureFile> layeredReactFixture() {
        return List.of(
            AbstractTypeScriptArchitectureFixtureTestSupport.tsFile(
                "src/api/contracts/OrderDto.ts", "typescript", "react", """
                export interface OrderDto {
                  id: string;
                  total: number;
                }
                """, List.of(), List.of(
                    AbstractTypeScriptArchitectureFixtureTestSupport.interfaceDeclaration("""
                        export interface OrderDto {
                          id: string;
                          total: number;
                        }
                        """, "OrderDto", List.of(), List.of(
                        AbstractTypeScriptArchitectureFixtureTestSupport.propertySignature("id: string;", "id"),
                        AbstractTypeScriptArchitectureFixtureTestSupport.propertySignature("total: number;", "total")
                    ), List.of())
                )
            ),
            AbstractTypeScriptArchitectureFixtureTestSupport.tsFile(
                "src/api/client/OrderApi.ts", "typescript", "react", """
                import type { OrderDto } from '../contracts/OrderDto';

                export interface OrderApi {
                  fetchOrder(id: string): OrderDto;
                }
                """, List.of("import type { OrderDto } from '../contracts/OrderDto';"), List.of(
                    AbstractTypeScriptArchitectureFixtureTestSupport.interfaceDeclaration("""
                        export interface OrderApi {
                          fetchOrder(id: string): OrderDto;
                        }
                        """, "OrderApi", List.of(), List.of(), List.of(
                        AbstractTypeScriptArchitectureFixtureTestSupport.methodSignature("fetchOrder(id: string): OrderDto;", "fetchOrder", "OrderDto")
                    ))
                )
            ),
            AbstractTypeScriptArchitectureFixtureTestSupport.tsFile(
                "src/services/core/OrderService.ts", "typescript", "react", """
                import type { OrderDto } from '../../api/contracts/OrderDto';
                import type { OrderApi } from '../../api/client/OrderApi';

                export class OrderService {
                  currentOrder: OrderDto;
                  constructor(api: OrderApi) {}
                  loadOrder(id: string): OrderDto { return this.currentOrder; }
                }
                """, List.of(
                    "import type { OrderDto } from '../../api/contracts/OrderDto';",
                    "import type { OrderApi } from '../../api/client/OrderApi';"
                ), List.of(
                    AbstractTypeScriptArchitectureFixtureTestSupport.classDeclaration("""
                        export class OrderService {
                          currentOrder: OrderDto;
                          constructor(api: OrderApi) {}
                          loadOrder(id: string): OrderDto { return this.currentOrder; }
                        }
                        """, "OrderService", List.of(), null, List.of(), List.of(
                        AbstractTypeScriptArchitectureFixtureTestSupport.publicField("currentOrder: OrderDto;", "currentOrder"),
                        AbstractTypeScriptArchitectureFixtureTestSupport.methodDefinition("constructor(api: OrderApi) {}", "constructor", null),
                        AbstractTypeScriptArchitectureFixtureTestSupport.methodDefinition("loadOrder(id: string): OrderDto { return this.currentOrder; }", "loadOrder", "OrderDto")
                    ))
                )
            ),
            AbstractTypeScriptArchitectureFixtureTestSupport.tsFile(
                "src/state/orders/OrdersStore.ts", "typescript", "react", """
                import type { OrderDto } from '../../api/contracts/OrderDto';
                import { OrderService } from '../../services/core/OrderService';

                export class OrdersStore {
                  service: OrderService;
                  current(): OrderDto { return this.service.loadOrder('1'); }
                }
                """, List.of(
                    "import type { OrderDto } from '../../api/contracts/OrderDto';",
                    "import { OrderService } from '../../services/core/OrderService';"
                ), List.of(
                    AbstractTypeScriptArchitectureFixtureTestSupport.classDeclaration("""
                        export class OrdersStore {
                          service: OrderService;
                          current(): OrderDto { return this.service.loadOrder('1'); }
                        }
                        """, "OrdersStore", List.of(), null, List.of(), List.of(
                        AbstractTypeScriptArchitectureFixtureTestSupport.publicField("service: OrderService;", "service"),
                        AbstractTypeScriptArchitectureFixtureTestSupport.methodDefinition("current(): OrderDto { return this.service.loadOrder('1'); }", "current", "OrderDto")
                    ))
                )
            ),
            AbstractTypeScriptArchitectureFixtureTestSupport.tsFile(
                "src/app/pages/OrdersPage.tsx", "typescript", "react", """
                import React from 'react';
                import { OrdersStore } from '../../state/orders/OrdersStore';

                export function OrdersPage(store: OrdersStore) { return <main />; }
                """, List.of(
                    "import React from 'react';",
                    "import { OrdersStore } from '../../state/orders/OrdersStore';"
                ), List.of(
                    AbstractTypeScriptArchitectureFixtureTestSupport.functionDeclaration("export function OrdersPage(store: OrdersStore) { return <main />; }", "OrdersPage")
                )
            )
        );
    }

    static List<AbstractTypeScriptArchitectureFixtureTestSupport.TsFixtureFile> angularFixture() {
        return List.of(
            AbstractTypeScriptArchitectureFixtureTestSupport.tsFile(
                "src/app/orders/order.dto.ts", "typescript", "angular", """
                export interface OrderDto {
                  id: string;
                }
                """, List.of(), List.of(
                    AbstractTypeScriptArchitectureFixtureTestSupport.interfaceDeclaration("""
                        export interface OrderDto {
                          id: string;
                        }
                        """, "OrderDto", List.of(), List.of(
                        AbstractTypeScriptArchitectureFixtureTestSupport.propertySignature("id: string;", "id")
                    ), List.of())
                )
            ),
            AbstractTypeScriptArchitectureFixtureTestSupport.tsFile(
                "src/app/orders/order.service.ts", "typescript", "angular", """
                import { Injectable } from '@angular/core';
                import type { OrderDto } from './order.dto';

                @Injectable()
                export class OrderService {
                  current: OrderDto;
                }
                """, List.of(
                    "import { Injectable } from '@angular/core';",
                    "import type { OrderDto } from './order.dto';"
                ), List.of(
                    AbstractTypeScriptArchitectureFixtureTestSupport.classDeclaration("""
                        @Injectable()
                        export class OrderService {
                          current: OrderDto;
                        }
                        """, "OrderService", List.of("@Injectable()"), null, List.of(), List.of(
                        AbstractTypeScriptArchitectureFixtureTestSupport.publicField("current: OrderDto;", "current")
                    ))
                )
            ),
            AbstractTypeScriptArchitectureFixtureTestSupport.tsFile(
                "src/app/orders/order-list.component.ts", "typescript", "angular", """
                import { Component } from '@angular/core';
                import { OrderService } from './order.service';

                @Component({ selector: 'app-order-list' })
                export class OrderListComponent {
                  service: OrderService;
                }
                """, List.of(
                    "import { Component } from '@angular/core';",
                    "import { OrderService } from './order.service';"
                ), List.of(
                    AbstractTypeScriptArchitectureFixtureTestSupport.classDeclaration("""
                        @Component({ selector: 'app-order-list' })
                        export class OrderListComponent {
                          service: OrderService;
                        }
                        """, "OrderListComponent", List.of("@Component({ selector: 'app-order-list' })"), null, List.of(), List.of(
                        AbstractTypeScriptArchitectureFixtureTestSupport.publicField("service: OrderService;", "service")
                    ))
                )
            ),
            AbstractTypeScriptArchitectureFixtureTestSupport.tsFile(
                "src/app/orders/orders.module.ts", "typescript", "angular", """
                import { NgModule } from '@angular/core';
                import { OrderListComponent } from './order-list.component';

                @NgModule({ declarations: [OrderListComponent] })
                export class OrdersModule {}
                """, List.of(
                    "import { NgModule } from '@angular/core';",
                    "import { OrderListComponent } from './order-list.component';"
                ), List.of(
                    AbstractTypeScriptArchitectureFixtureTestSupport.classDeclaration("""
                        @NgModule({ declarations: [OrderListComponent] })
                        export class OrdersModule {}
                        """, "OrdersModule", List.of("@NgModule({ declarations: [OrderListComponent] })"), null, List.of(), List.of())
                )
            )
        );
    }

    static List<AbstractTypeScriptArchitectureFixtureTestSupport.TsFixtureFile> frameworkRelationshipsFixture() {
        return List.of(
            AbstractTypeScriptArchitectureFixtureTestSupport.tsFile(
                "src/app/context/AuthContext.tsx", "typescript", "react", """
                import React, { createContext } from 'react';
                export const AuthContext = createContext(null);
                export function AuthProvider() { return <AuthContext.Provider value={{}}><section /></AuthContext.Provider>; }
                """, List.of("import React, { createContext } from 'react';"), List.of(
                    AbstractTypeScriptArchitectureFixtureTestSupport.functionDeclaration("export function AuthProvider() { return <AuthContext.Provider value={{}}><section /></AuthContext.Provider>; }", "AuthProvider")
                )
            ),
            AbstractTypeScriptArchitectureFixtureTestSupport.tsFile(
                "src/app/hooks/useOrdersQuery.ts", "typescript", "react", """
                export function useOrdersQuery() { return null; }
                """, List.of(), List.of(
                    AbstractTypeScriptArchitectureFixtureTestSupport.functionDeclaration("export function useOrdersQuery() { return null; }", "useOrdersQuery")
                )
            ),
            AbstractTypeScriptArchitectureFixtureTestSupport.tsFile(
                "src/app/pages/OrdersPage.tsx", "typescript", "react", """
                import React, { useContext } from 'react';
                import { Route } from 'react-router-dom';
                import { AuthContext } from '../context/AuthContext';
                import { useOrdersQuery } from '../hooks/useOrdersQuery';

                export function OrdersPage() {
                  const auth = useContext(AuthContext);
                  useOrdersQuery();
                  return <main><Route path="orders" element={<OrdersPage />} /></main>;
                }
                """, List.of(
                    "import React, { useContext } from 'react';",
                    "import { Route } from 'react-router-dom';",
                    "import { AuthContext } from '../context/AuthContext';",
                    "import { useOrdersQuery } from '../hooks/useOrdersQuery';"
                ), List.of(
                    AbstractTypeScriptArchitectureFixtureTestSupport.functionDeclaration("""
                    export function OrdersPage() {
                      const auth = useContext(AuthContext);
                      useOrdersQuery();
                      return <main><Route path="orders" element={<OrdersPage />} /></main>;
                    }
                    """, "OrdersPage")
                )
            ),
            AbstractTypeScriptArchitectureFixtureTestSupport.tsFile(
                "src/app/orders/orders.component.ts", "typescript", "angular", """
                import { Component, Inject } from '@angular/core';
                import { ORDER_API } from './orders.tokens';

                @Component({ selector: 'app-orders' })
                export class OrdersComponent {
                  constructor(@Inject(ORDER_API) api: OrdersApi) {}
                }
                """, List.of(
                    "import { Component, Inject } from '@angular/core';",
                    "import { ORDER_API } from './orders.tokens';"
                ), List.of(
                    AbstractTypeScriptArchitectureFixtureTestSupport.classDeclaration("""
                    @Component({ selector: 'app-orders' })
                    export class OrdersComponent {
                      constructor(@Inject(ORDER_API) api: OrdersApi) {}
                    }
                    """, "OrdersComponent", List.of("@Component({ selector: 'app-orders' })"), null, List.of(), List.of(
                    AbstractTypeScriptArchitectureFixtureTestSupport.methodDefinition("constructor(@Inject(ORDER_API) api: OrdersApi) {}", "constructor", null)
                )))
            ),
            AbstractTypeScriptArchitectureFixtureTestSupport.tsFile(
                "src/app/orders/orders.module.ts", "typescript", "angular", """
                import { NgModule } from '@angular/core';
                import { OrdersComponent } from './orders.component';
                import { OrdersApi } from './orders.api';
                import { ORDER_API } from './orders.tokens';

                @NgModule({ declarations: [OrdersComponent], providers: [{ provide: ORDER_API, useClass: OrdersApi }] })
                export class OrdersModule {}
                """, List.of(
                    "import { NgModule } from '@angular/core';",
                    "import { OrdersComponent } from './orders.component';",
                    "import { OrdersApi } from './orders.api';",
                    "import { ORDER_API } from './orders.tokens';"
                ), List.of(
                    AbstractTypeScriptArchitectureFixtureTestSupport.classDeclaration("""
                    @NgModule({ declarations: [OrdersComponent], providers: [{ provide: ORDER_API, useClass: OrdersApi }] })
                    export class OrdersModule {}
                    """, "OrdersModule", List.of("@NgModule({ declarations: [OrdersComponent], providers: [{ provide: ORDER_API, useClass: OrdersApi }] })"), null, List.of(), List.of())
                )
            )
        );
    }
}
