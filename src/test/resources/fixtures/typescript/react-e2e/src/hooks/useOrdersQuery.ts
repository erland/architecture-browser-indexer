import { useEffect, useState } from 'react';
import type { OrderDto } from '../domain/OrderDto';

export function useOrdersQuery(): OrderDto[] {
  const [orders, setOrders] = useState<OrderDto[]>([]);
  useEffect(() => { setOrders([]); }, []);
  return orders;
}
