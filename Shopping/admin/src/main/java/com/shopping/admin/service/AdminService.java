package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.Admin;
import com.shopping.admin.entity.AdminRoleRelation;
import com.shopping.admin.entity.Permission;
import com.shopping.admin.entity.Role;
import com.shopping.admin.entity.RolePermission;
import com.shopping.admin.exception.BusinessException;
import com.shopping.admin.mapper.AdminMapper;
import com.shopping.admin.mapper.AdminRoleRelationMapper;
import com.shopping.admin.mapper.PermissionMapper;
import com.shopping.admin.mapper.RoleMapper;
import com.shopping.admin.mapper.RolePermissionMapper;
import com.shopping.admin.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService extends ServiceImpl<AdminMapper, Admin> {

    private final AdminRoleRelationMapper adminRoleRelationMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // Login rate limiting: username -> {failCount, lastFailTime}
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = TimeUnit.MINUTES.toMillis(15);
    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    private static class LoginAttempt {
        int failCount;
        long lastFailTime;
        LoginAttempt(int count, long time) { this.failCount = count; this.lastFailTime = time; }
    }

    public Map<String, Object> login(String username, String password) {
        // Check login rate limit
        LoginAttempt attempt = loginAttempts.get(username);
        if (attempt != null && attempt.failCount >= MAX_LOGIN_ATTEMPTS) {
            long elapsed = System.currentTimeMillis() - attempt.lastFailTime;
            if (elapsed < LOCK_DURATION_MS) {
                long remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(LOCK_DURATION_MS - elapsed) + 1;
                throw new BusinessException(429, "登录失败次数过多，请" + remainingMinutes + "分钟后再试");
            } else {
                loginAttempts.remove(username);
            }
        }

        Admin admin = getOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, username));
        if (admin == null) {
            recordLoginFailure(username);
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (admin.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            recordLoginFailure(username);
            throw new BusinessException(401, "用户名或密码错误");
        }
        // Login success - clear failure count
        loginAttempts.remove(username);
        Map<String, Object> result = buildAdminInfo(admin.getAdminId());
        result.put("token", jwtUtil.generateToken(admin.getAdminId(), admin.getUsername()));
        return result;
    }

    private void recordLoginFailure(String username) {
        loginAttempts.compute(username, (k, v) ->
                v == null ? new LoginAttempt(1, System.currentTimeMillis())
                        : new LoginAttempt(v.failCount + 1, System.currentTimeMillis()));
    }

    public Map<String, Object> buildAdminInfo(Long adminId) {
        Admin admin = getById(adminId);
        if (admin == null) {
            throw new BusinessException(404, "管理员不存在");
        }
        List<Role> roles = getRoles(adminId);
        List<String> roleCodes = roles.stream().map(Role::getRoleCode).distinct().toList();
        List<String> permissions = new ArrayList<>(getPermissionCodes(adminId));
        boolean superAdmin = isSuperAdmin(adminId, roleCodes);

        Map<String, Object> result = new HashMap<>();
        result.put("adminId", admin.getAdminId());
        result.put("username", admin.getUsername());
        result.put("realName", admin.getRealName());
        result.put("avatar", null); // tb_platform_admin 无 avatar 字段
        result.put("roles", roles);
        result.put("roleIds", roles.stream().map(Role::getRoleId).toList());
        result.put("roleCodes", roleCodes);
        result.put("permissions", permissions);
        result.put("permissionCodes", permissions);
        result.put("menus", buildMenus(permissions, superAdmin));
        result.put("superAdmin", superAdmin);
        return result;
    }

    public PageResult<Admin> listAdmins(long current, long size, String keyword) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Admin::getUsername, keyword)
                    .or().like(Admin::getRealName, keyword)
                    .or().like(Admin::getPhone, keyword);
        }
        wrapper.orderByDesc(Admin::getCreateTime);
        Page<Admin> page = page(new Page<>(current, size), wrapper);
        page.getRecords().forEach(a -> a.setPassword(null));
        return new PageResult<>(page);
    }

    public void addAdmin(Admin admin) {
        long count = count(new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, admin.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        if (admin.getPassword() == null || admin.getPassword().isBlank()) {
            throw new BusinessException(400, "新增员工必须设置初始密码");
        }
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        save(admin);
    }

    public void updateAdmin(Admin admin) {
        Admin existing = getById(admin.getAdminId());
        if (existing == null) {
            throw new BusinessException("管理员不存在");
        }
        if (admin.getPassword() != null && !admin.getPassword().isBlank()) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        } else {
            admin.setPassword(existing.getPassword());
        }
        updateById(admin);
    }

    public void changePassword(Long adminId, String oldPassword, String newPassword) {
        Admin admin = getById(adminId);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
            throw new BusinessException(400, "原密码不正确");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException(400, "新密码长度不能少于6位");
        }
        admin.setPassword(passwordEncoder.encode(newPassword));
        updateById(admin);
    }

    public void deleteAdmin(Long adminId) {
        if (adminId != null && adminId == 1L) {
            throw new BusinessException(400, "超级管理员不能删除");
        }
        removeById(adminId);
        adminRoleRelationMapper.delete(new LambdaQueryWrapper<AdminRoleRelation>()
                .eq(AdminRoleRelation::getAdminId, adminId));
    }

    @Transactional
    public void assignRoles(Long adminId, List<Long> roleIds) {
        Admin admin = getById(adminId);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        List<Long> safeRoleIds = roleIds == null ? List.of() : roleIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (adminId == 1L && safeRoleIds.stream().noneMatch(id -> id == 1L)) {
            throw new BusinessException(400, "超级管理员必须保留超级管理员身份");
        }
        adminRoleRelationMapper.delete(new LambdaQueryWrapper<AdminRoleRelation>()
                .eq(AdminRoleRelation::getAdminId, adminId));
        for (Long roleId : safeRoleIds) {
            AdminRoleRelation relation = new AdminRoleRelation();
            relation.setAdminId(adminId);
            relation.setRoleId(roleId);
            adminRoleRelationMapper.insert(relation);
        }
    }

    public List<Long> getRoleIds(Long adminId) {
        return adminRoleRelationMapper.selectList(new LambdaQueryWrapper<AdminRoleRelation>()
                        .eq(AdminRoleRelation::getAdminId, adminId))
                .stream()
                .map(AdminRoleRelation::getRoleId)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<Role> getRoles(Long adminId) {
        List<Long> roleIds = getRoleIds(adminId);
        if (roleIds.isEmpty() && adminId != null && adminId == 1L) {
            Role superRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, "SUPER_ADMIN"));
            return superRole == null ? List.of() : List.of(superRole);
        }
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .in(Role::getRoleId, roleIds)
                .eq(Role::getStatus, 1));
    }

    public Set<String> getPermissionCodes(Long adminId) {
        List<Role> roles = getRoles(adminId);
        List<String> roleCodes = roles.stream().map(Role::getRoleCode).distinct().toList();
        if (isSuperAdmin(adminId, roleCodes)) {
            Set<String> all = permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                            .eq(Permission::getStatus, 1))
                    .stream()
                    .map(Permission::getPermissionCode)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            all.add("*");
            return all;
        }
        List<Long> roleIds = roles.stream().map(Role::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return Set.of("DASHBOARD_VIEW", "NOTIFICATION_MGMT");
        }
        List<Long> permissionIds = rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermission>()
                        .in(RolePermission::getRoleId, roleIds))
                .stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .toList();
        Set<String> codes = new LinkedHashSet<>();
        if (!permissionIds.isEmpty()) {
            codes.addAll(permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                            .in(Permission::getPermissionId, permissionIds)
                            .eq(Permission::getStatus, 1))
                    .stream()
                    .map(Permission::getPermissionCode)
                    .toList());
        }
        codes.addAll(defaultPermissionCodesForRoles(roleCodes));
        return codes;
    }

    public boolean hasPermission(Long adminId, String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return true;
        }
        Set<String> codes = getPermissionCodes(adminId);
        return codes.contains("*") || codes.contains(permissionCode);
    }

    private boolean isSuperAdmin(Long adminId, List<String> roleCodes) {
        return (adminId != null && adminId == 1L) || roleCodes.contains("SUPER_ADMIN");
    }

    private Set<String> defaultPermissionCodesForRoles(List<String> roleCodes) {
        Set<String> codes = new LinkedHashSet<>();
        codes.add("DASHBOARD_VIEW");
        codes.add("NOTIFICATION_MGMT");
        for (String roleCode : roleCodes) {
            switch (roleCode) {
                case "OPERATOR", "OPS_MANAGER" -> codes.addAll(List.of(
                        "MERCHANT_MGMT", "MERCHANT_VIEW", "MERCHANT_AUDIT", "MERCHANT_FREEZE", "MERCHANT_CREDIT",
                        "GOODS_MGMT", "GOODS_VIEW", "GOODS_AUDIT", "GOODS_OFFLINE", "GOODS_CATEGORY",
                        "ORDER_MGMT", "ORDER_VIEW", "ORDER_REMIND_SHIP", "MARKETING_MGMT", "MARKETING_COUPON", "MARKETING_ACTIVITY",
                        "CONTENT_MGMT", "CONTENT_BANNER", "REVIEW_MGMT", "DATA_MGMT", "DATA_VIEW"));
                case "SERVICE", "CS_SUPERVISOR" -> codes.addAll(List.of(
                        "USER_MGMT", "USER_VIEW", "ORDER_MGMT", "ORDER_VIEW", "ORDER_REMIND_SHIP", "ORDER_REFUND",
                        "AFTER_SALE_MGMT", "AFTER_SALE_HANDLE", "DISPUTE_MGMT", "DISPUTE_HANDLE", "CHAT_MGMT"));
                case "AUDITOR", "DATA_ANALYST" -> codes.addAll(List.of(
                        "DATA_MGMT", "DATA_VIEW", "REPORT_VIEW", "LOG_VIEW", "USER_MGMT", "USER_VIEW",
                        "MERCHANT_MGMT", "MERCHANT_VIEW", "GOODS_MGMT", "GOODS_VIEW", "ORDER_MGMT", "ORDER_VIEW", "DISPUTE_MGMT", "AFTER_SALE_MGMT"));
                case "RISK_OFFICER" -> codes.addAll(List.of(
                        "USER_MGMT", "USER_VIEW", "USER_RISK", "MERCHANT_MGMT", "MERCHANT_VIEW", "MERCHANT_FREEZE", "MERCHANT_CREDIT",
                        "GOODS_MGMT", "GOODS_VIEW", "GOODS_RISK", "ORDER_MGMT", "ORDER_VIEW", "ORDER_ABNORMAL", "DISPUTE_MGMT", "DISPUTE_HANDLE", "AFTER_SALE_MGMT"));
                case "FINANCE_OFFICER" -> codes.addAll(List.of("ORDER_MGMT", "ORDER_VIEW", "ORDER_REFUND", "AFTER_SALE_MGMT", "AFTER_SALE_HANDLE", "FINANCE_MGMT", "FINANCE_VIEW", "FINANCE_RECONCILE", "DATA_MGMT", "DATA_VIEW", "REPORT_VIEW"));
                case "CONTENT_REVIEWER" -> codes.addAll(List.of("GOODS_MGMT", "GOODS_VIEW", "GOODS_AUDIT", "MERCHANT_MGMT", "MERCHANT_VIEW", "CONTENT_MGMT", "CONTENT_BANNER", "REVIEW_MGMT"));
                default -> { }
            }
        }
        return codes;
    }

    private List<Map<String, Object>> buildMenus(List<String> permissions, boolean superAdmin) {
        Set<String> codes = new LinkedHashSet<>(permissions);
        List<Map<String, Object>> menus = new ArrayList<>();
        addMenu(menus, codes, superAdmin, "首页", "/dashboard", "DASHBOARD_VIEW");
        addMenu(menus, codes, superAdmin, "商品", "/goods", "GOODS_MGMT");
        addMenu(menus, codes, superAdmin, "订单", "/order", "ORDER_MGMT");
        addMenu(menus, codes, superAdmin, "商户", "/merchant", "MERCHANT_MGMT");
        addMenu(menus, codes, superAdmin, "用户", "/user", "USER_MGMT");
        addMenu(menus, codes, superAdmin, "售后", "/after-sale", "AFTER_SALE_MGMT");
        addMenu(menus, codes, superAdmin, "纠纷", "/dispute", "DISPUTE_MGMT");
        addMenu(menus, codes, superAdmin, "异常", "/abnormal", "ORDER_ABNORMAL");
        addMenu(menus, codes, superAdmin, "客服", "/chat", "CHAT_MGMT");
        addMenu(menus, codes, superAdmin, "营销", "/coupon", "MARKETING_MGMT");
        addMenu(menus, codes, superAdmin, "轮播", "/banner", "CONTENT_BANNER");
        addMenu(menus, codes, superAdmin, "评论", "/review", "REVIEW_MGMT");
        addMenu(menus, codes, superAdmin, "数据", "/report", "DATA_MGMT");
        addMenu(menus, codes, superAdmin, "日志", "/log", "LOG_VIEW");
        addMenu(menus, codes, superAdmin, "系统", "/admin", "ADMIN_MGMT");
        return menus;
    }

    private void addMenu(List<Map<String, Object>> menus, Set<String> codes, boolean superAdmin,
                         String label, String path, String permission) {
        if (superAdmin || codes.contains(permission) || codes.contains("*")) {
            Map<String, Object> menu = new LinkedHashMap<>();
            menu.put("label", label);
            menu.put("path", path);
            menu.put("permission", permission);
            menus.add(menu);
        }
    }
}
