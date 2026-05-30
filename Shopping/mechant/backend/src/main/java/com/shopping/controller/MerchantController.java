package com.shopping.controller;

import com.shopping.entity.Merchant;
import com.shopping.entity.MerchantAuditLog;
import com.shopping.service.MerchantAuditLogService;
import com.shopping.service.MerchantService;
import com.shopping.service.ShopFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private ShopFollowService shopFollowService;

    @Autowired
    private MerchantAuditLogService auditLogService;

    @PostMapping("/apply")
    public Merchant apply(@RequestBody Merchant merchant) {
        return merchantService.apply(merchant);
    }

    @GetMapping("/info")
    public Merchant info(@RequestParam Long merchantId) {
        Merchant m = merchantService.getById(merchantId);
        if (m == null) return null;
        m.setFollowerCount(shopFollowService.countByMerchantId(merchantId));
        return m;
    }

    @GetMapping("/audit")
    public Map<String, Object> audit(@RequestParam Long merchantId) {
        Map<String, Object> res = new HashMap<>();
        res.put("merchant", merchantService.getById(merchantId));
        res.put("logs", auditLogService.listByMerchantId(merchantId));
        return res;
    }

    @PutMapping("/profile")
    public boolean updateProfile(@RequestBody Merchant merchant) {
        Merchant exist = merchantService.getById(merchant.getMerchantId());
        if (exist == null) return false;
        Merchant patch = new Merchant();
        patch.setMerchantId(exist.getMerchantId());
        patch.setPhone(merchant.getPhone());
        patch.setEmail(merchant.getEmail());
        patch.setAddress(merchant.getAddress());
        patch.setLegalPerson(merchant.getLegalPerson());
        patch.setIdCard(merchant.getIdCard());
        patch.setUpdateTime(LocalDateTime.now());
        return merchantService.updateById(patch);
    }

    @PutMapping("/logo")
    public boolean updateLogo(@RequestBody Merchant merchant) {
        Merchant exist = merchantService.getById(merchant.getMerchantId());
        if (exist == null) return false;
        Merchant patch = new Merchant();
        patch.setMerchantId(exist.getMerchantId());
        patch.setShopLogo(merchant.getShopLogo());
        patch.setUpdateTime(LocalDateTime.now());
        return merchantService.updateById(patch);
    }

    @PutMapping("/materials")
    public boolean updateMaterials(@RequestBody Merchant merchant) {
        Merchant exist = merchantService.getById(merchant.getMerchantId());
        if (exist == null) return false;
        Integer before = exist.getAuditStatus();

        Merchant patch = new Merchant();
        patch.setMerchantId(exist.getMerchantId());
        patch.setMerchantName(merchant.getMerchantName());
        patch.setBusinessLicense(merchant.getBusinessLicense());
        patch.setIndustryLicense(merchant.getIndustryLicense());
        patch.setShopLogo(merchant.getShopLogo());
        patch.setShopIntro(merchant.getShopIntro());
        patch.setAuditStatus(0);
        patch.setAuditRemark(null);
        patch.setAuditTime(null);
        patch.setUpdateTime(LocalDateTime.now());

        boolean ok = merchantService.updateById(patch);
        if (!ok) return false;

        MerchantAuditLog log = new MerchantAuditLog();
        log.setMerchantId(exist.getMerchantId());
        log.setBeforeStatus(before);
        log.setAfterStatus(0);
        log.setAuditAdminId(0L);
        log.setAuditRemark("商家更新资料，提交审核");
        log.setCreateTime(LocalDateTime.now());
        auditLogService.save(log);
        return true;
    }

    @GetMapping("/list")
    public List<Merchant> list(@RequestParam Long merchantId) {
        return merchantService.listByMerchantId(merchantId);
    }

    @GetMapping("/{id}/public")
    public Merchant publicInfo(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        Merchant m = merchantService.getById(id);
        if (m == null) return null;
        m.setFollowerCount(shopFollowService.countByMerchantId(id));
        if (userId != null) {
            m.setFollowed(shopFollowService.isFollowed(userId, id));
        }
        return m;
    }

    @PostMapping("/{id}/follow")
    public Map<String, Object> toggleFollow(@PathVariable Long id, @RequestParam Long userId) {
        boolean followed = shopFollowService.toggle(userId, id);
        long count = shopFollowService.countByMerchantId(id);
        Map<String, Object> res = new HashMap<>();
        res.put("followed", followed);
        res.put("followerCount", count);
        return res;
    }

    @GetMapping("/{id}/follow/count")
    public Map<String, Object> followerCount(@PathVariable Long id) {
        long count = shopFollowService.countByMerchantId(id);
        Map<String, Object> res = new HashMap<>();
        res.put("followerCount", count);
        return res;
    }

    @GetMapping("/{id}/follow/status")
    public Map<String, Object> followStatus(@PathVariable Long id, @RequestParam Long userId) {
        boolean followed = shopFollowService.isFollowed(userId, id);
        Map<String, Object> res = new HashMap<>();
        res.put("followed", followed);
        return res;
    }
}
