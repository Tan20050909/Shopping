package com.shopping.admin.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.admin.service.AdminService;
import com.shopping.admin.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final AdminService adminService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            writeError(response, 401, "未登录或token已过期");
            return false;
        }
        token = token.substring(7);
        try {
            if (jwtUtil.isTokenExpired(token)) {
                writeError(response, 401, "token已过期");
                return false;
            }
            Long adminId = jwtUtil.getAdminIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            request.setAttribute("adminId", adminId);
            request.setAttribute("username", username);

            String requiredPermission = resolvePermission(request);
            if (requiredPermission != null && !adminService.hasPermission(adminId, requiredPermission)) {
                writeError(response, 403, "当前身份无权操作该功能");
                return false;
            }
            return true;
        } catch (Exception e) {
            if (e instanceof com.shopping.admin.exception.BusinessException) {
                writeError(response, 403, e.getMessage());
            } else {
                writeError(response, 401, "无效token");
            }
            return false;
        }

    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of("code", code, "message", message)));
    }

    private String resolvePermission(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Public endpoints - no permission required
        if (uri.startsWith("/api/admin/info") || uri.startsWith("/api/notification") || uri.startsWith("/api/chat-bot")) {
            return null;
        }
        // Banner active list is public (for C-end display)
        if (uri.equals("/api/banner/active")) {
            return null;
        }
        if (uri.startsWith("/api/admin")) return "ADMIN_MGMT";
        if (uri.startsWith("/api/role") || uri.startsWith("/api/permission")) return "SYSTEM_ROLE";
        if (uri.startsWith("/api/config")) return "SYSTEM_CONFIG";
        if (uri.startsWith("/api/log")) return "LOG_VIEW";
        if (uri.startsWith("/api/dashboard")) return "DASHBOARD_VIEW";
        if (uri.startsWith("/api/report")) return "DATA_MGMT";
        if (uri.startsWith("/api/message-template")) return "MESSAGE_MGMT";
        if (uri.startsWith("/api/user")) return "USER_MGMT";
        if (uri.startsWith("/api/merchant")) return "MERCHANT_MGMT";
        if (uri.startsWith("/api/goods") || uri.startsWith("/api/category")) return "GOODS_MGMT";
        if (uri.startsWith("/api/coupon")) return "MARKETING_COUPON";
        if (uri.startsWith("/api/activity") || uri.startsWith("/api/group-buy")) return "MARKETING_ACTIVITY";
        if (uri.startsWith("/api/banner")) return "CONTENT_BANNER";
        if (uri.startsWith("/api/review")) return "REVIEW_MGMT";
        if (uri.startsWith("/api/chat")) return "CHAT_MGMT";
        if (uri.startsWith("/api/after-sale")) return "POST".equalsIgnoreCase(method) ? "AFTER_SALE_HANDLE" : "AFTER_SALE_MGMT";
        if (uri.startsWith("/api/dispute")) return "GET".equalsIgnoreCase(method) ? "DISPUTE_MGMT" : "DISPUTE_HANDLE";
        if (uri.startsWith("/api/abnormal")) return "ORDER_ABNORMAL";
        if (uri.startsWith("/api/order")) {
            if (uri.endsWith("/remind-ship")) return "ORDER_REMIND_SHIP";
            if (uri.endsWith("/refund")) return "ORDER_REFUND";
            if (uri.endsWith("/cancel") || uri.endsWith("/note") || uri.endsWith("/ship")) return "ORDER_INTERVENE";
            return "ORDER_VIEW";
        }
        return null;
    }
}
