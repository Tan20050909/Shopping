package org.example.context;

import org.example.common.BizException;
import org.example.common.ErrorCode;

public final class UserContext {
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setCurrentUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long getCurrentUserId() {
        return USER_ID_HOLDER.get();
    }

    public static long requireCurrentUserId() {
        Long userId = USER_ID_HOLDER.get();
        if (userId == null) {
            throw new BizException(ErrorCode.USER_CONTEXT_MISSING, "缺少当前用户信息");
        }
        return userId;
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
