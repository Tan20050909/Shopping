package com.shopping.entity;

import lombok.Data;
import java.util.List;

@Data
public class OrderDetail {
    private Order order;
    private List<OrderItem> items;
    private Logistics logistics;
    private List<LogisticsTrace> logisticsTraces;
}
