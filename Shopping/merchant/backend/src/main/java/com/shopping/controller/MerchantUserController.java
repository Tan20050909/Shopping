package com.shopping.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shopping.entity.Merchant;
import com.shopping.entity.MerchantUser;
import com.shopping.service.MerchantUserService;
import com.shopping.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant-user")
public class MerchantUserController {
    
    @Autowired
    private MerchantUserService merchantUserService;
    
    @Autowired
    private MerchantService merchantService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> params) {
        String account = params.get("phone");
        if (account == null || account.isBlank()) {
            account = params.get("username");
        }
        String password = params.get("password");
        
        Map<String, Object> result = new HashMap<>();
        
        /*
        // Temporarily disabled as tb_merchant_user table does not exist
        MerchantUser user = merchantUserService.login(username, password);
        if (user != null) {
            result.put("success", true);
            result.put("data", user);
            result.put("message", "登录成功");
            return result;
        }
        */
        
        final String accountFinal = account;
        Merchant merchant = merchantService.getOne(new LambdaQueryWrapper<Merchant>()
                .and(wrapper -> wrapper.eq(Merchant::getPhone, accountFinal).or().eq(Merchant::getMerchantName, accountFinal)));
        if (merchant != null && verifyPassword(merchant.getPassword(), password)) {
            Map<String, Object> merchantData = new HashMap<>();
            merchantData.put("id", merchant.getMerchantId());
            merchantData.put("merchantId", merchant.getMerchantId());
            merchantData.put("username", merchant.getMerchantName());
            merchantData.put("shopName", merchant.getMerchantName());
            merchantData.put("merchantName", merchant.getMerchantName());

            result.put("success", true);
            result.put("data", merchantData);
            result.put("message", "登录成功");
            return result;
        }
        
        result.put("success", false);
        result.put("message", "手机号或密码错误");
        return result;
    }

    private boolean verifyPassword(String stored, String raw) {
        if (stored == null || stored.isBlank() || raw == null) {
            return false;
        }
        String v = stored.trim();
        if (v.startsWith("$2a$") || v.startsWith("$2b$") || v.startsWith("$2y$")) {
            return passwordEncoder.matches(raw, v);
        }
        return v.equals(raw);
    }
    
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        String phone = params.get("phone");

        Map<String, Object> result = new HashMap<>();

        if (username == null || username.isBlank()) {
            result.put("success", false);
            result.put("message", "用户名不能为空");
            return result;
        }
        if (password == null || password.isBlank()) {
            result.put("success", false);
            result.put("message", "密码不能为空");
            return result;
        }
        if (phone == null || phone.isBlank()) {
            result.put("success", false);
            result.put("message", "手机号不能为空");
            return result;
        }

        Merchant existed = merchantService.getOne(new LambdaQueryWrapper<Merchant>()
                .and(w -> w.eq(Merchant::getPhone, phone).or().eq(Merchant::getMerchantName, username)));
        if (existed != null) {
            result.put("success", false);
            result.put("message", "手机号或店铺名已存在");
            return result;
        }

        Merchant created = new Merchant();
        created.setMerchantName(username.trim());
        created.setLegalPerson("");
        created.setIdCard("");
        created.setBusinessLicense("");
        created.setIndustryLicense("");
        created.setPhone(phone.trim());
        created.setEmail("");
        created.setAddress("");
        created.setPassword(passwordEncoder.encode(password.trim()));
        created.setMerchantType(1);
        created.setShopLogo("");
        created.setShopIntro("");
        created.setStatus(0);
        created.setAuditStatus(0);
        created.setAuditRemark("");
        created.setShopScore(0.0);
        created.setIsDeleted(0);
        created.setRegisterTime(java.time.LocalDateTime.now());
        created.setUpdateTime(java.time.LocalDateTime.now());
        merchantService.save(created);

        Map<String, Object> merchantData = new HashMap<>();
        merchantData.put("id", created.getMerchantId());
        merchantData.put("merchantId", created.getMerchantId());
        merchantData.put("username", created.getMerchantName());
        merchantData.put("shopName", created.getMerchantName());
        merchantData.put("merchantName", created.getMerchantName());

        result.put("success", true);
        result.put("data", merchantData);
        result.put("message", "注册成功");
        return result;
    }
    
    @GetMapping("/info/{id}")
    public MerchantUser getInfo(@PathVariable Long id) {
        return merchantUserService.getById(id);
    }
}
