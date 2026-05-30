package org.example.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.context.UserContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        Long userId = tokenService.resolve(request.getHeader("Authorization"));
        if (userId == null) {
            userId = tokenService.resolve(request.getHeader("X-Token"));
        }
        if (userId == null && isPublicEndpoint(request)) {
            return true;
        }
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"请先登录\",\"data\":null}");
            return false;
        }
        UserContext.setCurrentUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (path.equals("/hello") || path.startsWith("/api/auth/")) {
            return true;
        }
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        return path.equals("/api/user/home")
                || path.equals("/api/user/home/banners")
                || path.equals("/api/user/home/sections")
                || path.equals("/api/user/categories")
                || path.equals("/api/user/products")
                || path.matches("/api/user/products/\\d+")
                || path.matches("/api/user/products/\\d+/reviews")
                || path.matches("/api/user/shops/\\d+")
                || path.matches("/api/user/shops/\\d+/products")
                || path.equals("/api/user/recommendations")
                || path.equals("/api/user/rankings")
                || path.equals("/api/user/live-rooms")
                || path.matches("/api/user/live-rooms/\\d+")
                || path.matches("/api/user/live-rooms/\\d+/products")
                || path.matches("/api/user/live-rooms/\\d+/comments");
    }
}
