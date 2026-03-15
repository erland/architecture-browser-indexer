package com.example.orders.api;

import com.example.orders.domain.OrderEntity;
import com.example.orders.dto.OrderRequest;
import com.example.orders.service.OrderService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    OrderService orderService;

    @GET
    public List<OrderEntity> listOrders() {
        return orderService.listOrders();
    }

    @POST
    public OrderEntity createOrder(OrderRequest request) {
        return orderService.createOrder(request);
    }
}
