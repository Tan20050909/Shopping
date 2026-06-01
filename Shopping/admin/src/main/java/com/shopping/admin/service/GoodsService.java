package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.Goods;
import com.shopping.admin.entity.GoodsAuditLog;
import com.shopping.admin.exception.BusinessException;
import com.shopping.admin.mapper.GoodsAuditLogMapper;
import com.shopping.admin.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoodsService extends ServiceImpl<GoodsMapper, Goods> {

    private final GoodsAuditLogMapper goodsAuditLogMapper;
    private final OperationLogService operationLogService;
    private final NotificationService notificationService;

    public PageResult<Goods> listGoods(long current, long size, String keyword, Integer auditStatus, Integer status) {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Goods::getGoodsName, keyword)
                    .or().like(Goods::getGoodsDesc, keyword);
        }
        if (auditStatus != null) {
            wrapper.eq(Goods::getAuditStatus, auditStatus);
        }
        if (status != null) {
            wrapper.eq(Goods::getStatus, status);
        }
        wrapper.orderByDesc(Goods::getCreateTime);
        Page<Goods> page = page(new Page<>(current, size), wrapper);
        return new PageResult<>(page);
    }

    public void auditGoods(Long goodsId, Integer auditStatus, String auditRemark, Long adminId) {
        Goods goods = getById(goodsId);
        if (goods == null) {
            throw new BusinessException("商品不存在");
        }
        goods.setAuditStatus(auditStatus);
        goods.setAuditRemark(auditRemark);
        if (auditStatus == 1) {
            goods.setStatus(1);
        } else if (auditStatus == 2) {
            goods.setStatus(0);
        }
        updateById(goods);

        GoodsAuditLog log = new GoodsAuditLog();
        log.setGoodsId(goodsId);
        log.setMerchantId(goods.getMerchantId());
        log.setAuditStatus(auditStatus);
        log.setAuditAdminId(adminId);
        log.setAuditRemark(auditRemark);
        goodsAuditLogMapper.insert(log);

        String resultText = auditStatus == 1 ? "审核通过" : auditStatus == 2 ? "审核拒绝" : "待审核";
        operationLogService.saveBizLog(adminId, "商品管理", "AUDIT",
                resultText + "商品：" + goods.getGoodsName(), "/api/goods/audit", "POST",
                "goodsId=" + goodsId + ",auditStatus=" + auditStatus);
        notificationService.createBusinessNotification(1L, auditStatus == 1 ? 3 : 4, "商品审核结果",
                "商品" + "\"" + goods.getGoodsName() + "\"" + "已" + resultText, "goods", goodsId);
    }

    public void updateGoodsStatus(Long goodsId, Integer status, String reason) {
        Goods goods = getById(goodsId);
        if (goods == null) {
            throw new BusinessException("商品不存在");
        }
        if (status == 3 && (reason == null || reason.isBlank())) {
            throw new BusinessException("平台下架原因不能为空");
        }
        goods.setStatus(status);
        if (status == 3) {
            goods.setAuditRemark(reason);
        }
        updateById(goods);
        String desc = status == 1 ? "上架商品" : status == 3 ? "平台下架商品" : "下架商品";
        operationLogService.saveBizLog(null, "商品管理", "STATUS",
                desc + "：" + goods.getGoodsName() + (reason != null ? "，原因：" + reason : ""),
                "/api/goods/" + goodsId + "/status", "PUT", "status=" + status);
    }
}
