package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.*;
import com.shopping.admin.exception.BusinessException;
import com.shopping.admin.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsService extends ServiceImpl<GoodsMapper, Goods> {

    private final GoodsAuditLogMapper goodsAuditLogMapper;
    private final GoodsSkuMapper goodsSkuMapper;
    private final GoodsPicMapper goodsPicMapper;
    private final CategoryMapper categoryMapper;
    private final MerchantMapper merchantMapper;
    private final OperationLogService operationLogService;
    private final NotificationService notificationService;

    public PageResult<Goods> listGoods(long current, long size, String keyword, Integer auditStatus, Integer status) {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Goods::getGoodsName, keyword)
                    .or().like(Goods::getGoodsIntro, keyword)
                    .or().like(Goods::getKeywords, keyword));
        }
        if (auditStatus != null) {
            wrapper.eq(Goods::getAuditStatus, auditStatus);
        }
        if (status != null) {
            wrapper.eq(Goods::getStatus, status);
        }
        wrapper.orderByDesc(Goods::getCreateTime);
        Page<Goods> page = page(new Page<>(current, size), wrapper);
        List<Goods> records = page.getRecords();

        if (records != null && !records.isEmpty()) {
            fillExtraFields(records);
        }

        return new PageResult<>(page);
    }

    @Override
    public Goods getById(Serializable id) {
        Goods goods = super.getById(id);
        if (goods != null) {
            fillExtraFields(Collections.singletonList(goods));
        }
        return goods;
    }

    private void fillExtraFields(List<Goods> goodsList) {
        Set<Long> goodsIds = goodsList.stream().map(Goods::getGoodsId).collect(Collectors.toSet());
        Set<Long> cateIds = goodsList.stream().map(Goods::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> merchantIds = goodsList.stream().map(Goods::getMerchantId).filter(Objects::nonNull).collect(Collectors.toSet());

        // 批量查询 SKU 聚合价格和库存（表不存在时降级处理）
        Map<Long, BigDecimal> minPriceMap = new HashMap<>();
        Map<Long, Integer> totalStockMap = new HashMap<>();
        if (!goodsIds.isEmpty()) {
            try {
                List<GoodsSku> skuList = goodsSkuMapper.selectList(
                        new LambdaQueryWrapper<GoodsSku>()
                                .in(GoodsSku::getGoodsId, goodsIds)
                                .eq(GoodsSku::getStatus, 1)
                                .eq(GoodsSku::getIsDeleted, 0));
                Map<Long, List<GoodsSku>> skuGroupMap = skuList.stream()
                        .collect(Collectors.groupingBy(GoodsSku::getGoodsId));
                for (Map.Entry<Long, List<GoodsSku>> entry : skuGroupMap.entrySet()) {
                    Long goodsId = entry.getKey();
                    List<GoodsSku> skus = entry.getValue();
                    BigDecimal minPrice = skus.stream()
                            .map(GoodsSku::getPrice)
                            .filter(Objects::nonNull)
                            .min(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);
                    int totalStock = skus.stream()
                            .mapToInt(s -> Math.max(s.getStock() - s.getLockStock(), 0))
                            .sum();
                    minPriceMap.put(goodsId, minPrice);
                    totalStockMap.put(goodsId, totalStock);
                }
            } catch (Exception e) {
                // tb_goods_sku 表不存在时降级处理，价格和库存显示为 0
                log.warn("查询 tb_goods_sku 失败（表可能不存在），价格和库存将显示为 0: {}", e.getMessage());
            }
        }

        // 批量查询分类名
        Map<Long, String> categoryNameMap = new HashMap<>();
        if (!cateIds.isEmpty()) {
            List<Category> categories = categoryMapper.selectBatchIds(cateIds);
            for (Category c : categories) {
                categoryNameMap.put(c.getCategoryId(), c.getCategoryName());
            }
        }

        // 批量查询商家名
        Map<Long, String> merchantNameMap = new HashMap<>();
        if (!merchantIds.isEmpty()) {
            List<Merchant> merchants = merchantMapper.selectBatchIds(merchantIds);
            for (Merchant m : merchants) {
                merchantNameMap.put(m.getMerchantId(), m.getMerchantName());
            }
        }

        // 批量查询商品图集（表不存在时降级处理）
        Map<Long, String> imagesMap = new HashMap<>();
        if (!goodsIds.isEmpty()) {
            try {
                List<GoodsPic> picList = goodsPicMapper.selectList(
                        new LambdaQueryWrapper<GoodsPic>()
                                .in(GoodsPic::getGoodsId, goodsIds)
                                .eq(GoodsPic::getIsDeleted, 0)
                                .orderByAsc(GoodsPic::getPicSort));
                Map<Long, List<GoodsPic>> picGroupMap = picList.stream()
                        .collect(Collectors.groupingBy(GoodsPic::getGoodsId));
                for (Map.Entry<Long, List<GoodsPic>> entry : picGroupMap.entrySet()) {
                    String images = entry.getValue().stream()
                            .map(GoodsPic::getPicUrl)
                            .collect(Collectors.joining(","));
                    imagesMap.put(entry.getKey(), images);
                }
            } catch (Exception e) {
                // tb_goods_pic 表不存在时降级处理，图集不显示
                log.warn("查询 tb_goods_pic 失败（表可能不存在），商品图集将不显示: {}", e.getMessage());
            }
        }

        // 填充字段
        for (Goods goods : goodsList) {
            goods.setPrice(minPriceMap.get(goods.getGoodsId()));
            goods.setStock(totalStockMap.getOrDefault(goods.getGoodsId(), 0));
            goods.setSales(goods.getSellCount());
            goods.setCategoryName(categoryNameMap.getOrDefault(goods.getCategoryId(), "-"));
            goods.setMerchantName(merchantNameMap.getOrDefault(goods.getMerchantId(), "-"));
            goods.setMainImage(goods.getGoodsPic());
            goods.setGoodsDesc(goods.getGoodsIntro());
            goods.setImages(imagesMap.get(goods.getGoodsId()));
        }
    }

    public void auditGoods(Long goodsId, Integer auditStatus, String auditRemark, Long adminId) {
        Goods goods = super.getById(goodsId);
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
        Goods goods = super.getById(goodsId);
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
