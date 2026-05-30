package org.example.context;

import org.example.common.BizException;
import org.example.common.ErrorCode;

public final class MerchantContext {
    private static final ThreadLocal<Long> MERCHANT_ID_HOLDER = new ThreadLocal<>();

    private MerchantContext() {
    }

    public static void setCurrentMerchantId(Long merchantId) {
        MERCHANT_ID_HOLDER.set(merchantId);
    }

    public static long requireCurrentMerchantId() {
        Long merchantId = MERCHANT_ID_HOLDER.get();
        if (merchantId == null) {
            throw new BizException(ErrorCode.USER_CONTEXT_MISSING, "缺少 X-Merchant-Id 请求头");
        }
        return merchantId;
    }

    public static void clear() {
        MERCHANT_ID_HOLDER.remove();
    }
}
