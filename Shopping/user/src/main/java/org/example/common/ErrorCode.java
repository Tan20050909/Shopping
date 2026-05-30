package org.example.common;

public final class ErrorCode {
    public static final String SUCCESS = "SUCCESS";
    public static final String PARAM_INVALID = "PARAM_INVALID";
    public static final String USER_CONTEXT_MISSING = "USER_CONTEXT_MISSING";
    public static final String DATA_NOT_FOUND = "DATA_NOT_FOUND";
    public static final String GOODS_OFF_SHELF = "GOODS_OFF_SHELF";
    public static final String SKU_STOCK_NOT_ENOUGH = "SKU_STOCK_NOT_ENOUGH";
    public static final String ADDRESS_NOT_BELONG_USER = "ADDRESS_NOT_BELONG_USER";
    public static final String CART_EMPTY = "CART_EMPTY";
    public static final String MULTI_MERCHANT_ORDER_NOT_SUPPORTED = "MULTI_MERCHANT_ORDER_NOT_SUPPORTED";
    public static final String COUPON_NOT_AVAILABLE = "COUPON_NOT_AVAILABLE";
    public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    public static final String ORDER_STATUS_INVALID = "ORDER_STATUS_INVALID";
    public static final String PAYMENT_STATUS_INVALID = "PAYMENT_STATUS_INVALID";
    public static final String SYSTEM_ERROR = "SYSTEM_ERROR";

    private ErrorCode() {
    }
}
