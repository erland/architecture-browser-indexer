import React, { createContext } from 'react';
import type { OrderDto } from '../domain/OrderDto';

export const OrdersContext = createContext<OrderDto[] | null>(null);
