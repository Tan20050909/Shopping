package com.shopping.entity;

import lombok.Data;

import java.util.List;

@Data
public class AfterSaleDetail {
    private AfterSale afterSale;
    private Order order;
    private Logistics buyerLogistics;
    private List<LogisticsTrace> buyerLogisticsTraces;
    private Logistics merchantLogistics;
    private List<LogisticsTrace> merchantLogisticsTraces;
}
