import React from 'react';
import { OrdersContext } from './OrdersContext';
import { useOrdersQuery } from '../hooks/useOrdersQuery';

export function OrdersProvider() {
  const orders = useOrdersQuery();
  return <OrdersContext.Provider value={orders}><section /></OrdersContext.Provider>;
}
