package com.shopping.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AfterSaleDetail {
    private AfterSale afterSale;
    private Order order;
    /** 订单项列表（含商品图片、名称） */
    private List<OrderItem> orderItems;
    private Logistics buyerLogistics;
    private List<LogisticsTrace> buyerLogisticsTraces;
    private Logistics merchantLogistics;
    private List<LogisticsTrace> merchantLogisticsTraces;
    /** 关联纠纷信息（从 tb_dispute 查询） */
    private Map<String, Object> dispute;
}
