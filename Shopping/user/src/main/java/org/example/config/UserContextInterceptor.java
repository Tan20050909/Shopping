package org.example.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.common.ErrorCode;
import org.example.context.UserContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserContextInterceptor implements HandlerInterceptor {
    private static final long DEFAULT_DEV_USER_ID = 1001L;
    private final Environment environment;

    public UserContextInterceptor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String rawUserId = request.getHeader("X-User-Id");
        Long userId = null;
        if (rawUserId != null && !rawUserId.isBlank()) {
            try {
                userId = Long.parseLong(rawUserId.trim());
            } catch (NumberFormatException ignored) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":\"" + ErrorCode.PARAM_INVALID + "\",\"message\":\"X-User-Id 格式不正确\",\"data\":null}");
                return false;
            }
        }
        if (userId == null && isDevEnvironment()) {
            userId = DEFAULT_DEV_USER_ID;
        }
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"" + ErrorCode.USER_CONTEXT_MISSING + "\",\"message\":\"缺少 X-User-Id 请求头\",\"data\":null}");
            return false;
        }
        UserContext.setCurrentUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean isDevEnvironment() {
        return environment.acceptsProfiles(Profiles.of("dev")) || environment.getActiveProfiles().length == 0;
    }
}
