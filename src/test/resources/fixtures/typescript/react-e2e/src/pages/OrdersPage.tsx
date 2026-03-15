import React, { useContext } from 'react';
import { OrdersContext } from '../context/OrdersContext';
import { OrdersTable } from '../components/OrdersTable';

export function OrdersPage() {
  const orders = useContext(OrdersContext) ?? [];
  return <main><OrdersTable orders={orders} /></main>;
}
