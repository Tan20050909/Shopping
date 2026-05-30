package org.example.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.common.BizException;
import org.example.common.ErrorCode;
import org.example.context.MerchantContext;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MerchantContextInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String rawMerchantId = request.getHeader("X-Merchant-Id");
        if (rawMerchantId == null || rawMerchantId.isBlank()) {
            throw new BizException(ErrorCode.USER_CONTEXT_MISSING, "缺少 X-Merchant-Id 请求头");
        }
        try {
            MerchantContext.setCurrentMerchantId(Long.parseLong(rawMerchantId.trim()));
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.PARAM_INVALID, "X-Merchant-Id 格式不正确");
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        MerchantContext.clear();
    }
}
