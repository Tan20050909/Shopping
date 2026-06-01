package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.Goods;
import com.shopping.entity.GoodsPriceLog;
import com.shopping.entity.GoodsSku;
import com.shopping.entity.GoodsStockLog;
import com.shopping.entity.MerchantStockSetting;
import com.shopping.mapper.GoodsPriceLogMapper;
import com.shopping.mapper.GoodsMapper;
import com.shopping.mapper.GoodsSkuMapper;
import com.shopping.mapper.GoodsStockLogMapper;
import com.shopping.mapper.MerchantStockSettingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class GoodsSkuService extends ServiceImpl<GoodsSkuMapper, GoodsSku> {

    @Autowired
    private GoodsPriceLogMapper priceLogMapper;

    @Autowired
    private GoodsStockLogMapper stockLogMapper;

    @Autowired
    private MerchantStockSettingMapper stockSettingMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    public List<GoodsSku> listByGoodsId(Long goodsId) {
        return list(new LambdaQueryWrapper<GoodsSku>().eq(GoodsSku::getGoodsId, goodsId));
    }

    @Transactional
    public boolean updateStock(Long id, Integer stock) {
        if (id == null || stock == null || stock < 0) return false;

        GoodsSku exist = getById(id);
        if (exist == null) return false;
        Integer oldStock = exist.getStock() == null ? 0 : exist.getStock();
        int newStock = stock;

        GoodsSku patch = new GoodsSku();
        patch.setId(id);
        patch.setStock(newStock);
        patch.setUpdateTime(LocalDateTime.now());
        boolean ok = updateById(patch);
        if (!ok) return false;

        try {
            Long goodsId = exist.getGoodsId();
            Long merchantId = null;
            if (goodsId != null) {
                Goods g = goodsMapper.selectById(goodsId);
                merchantId = g == null ? null : g.getMerchantId();
            }
            if (merchantId != null && goodsId != null) {
                GoodsStockLog log = new GoodsStockLog();
                log.setMerchantId(merchantId);
                log.setGoodsId(goodsId);
                log.setSkuId(id);
                log.setOldStock(oldStock);
                log.setNewStock(newStock);
                log.setChangeStock(newStock - oldStock);
                log.setCreateTime(LocalDateTime.now());
                stockLogMapper.insert(log);
            }
        } catch (Exception e) {
            return true;
        }
        return true;
    }

    @Transactional
    public boolean updatePrice(Long id, java.math.BigDecimal newPrice) {
        GoodsSku sku = getById(id);
        if (sku != null) {
            GoodsPriceLog log = new GoodsPriceLog();
            log.setSkuId(id);
            log.setOldPrice(sku.getPrice());
            log.setNewPrice(newPrice);
            log.setCreateTime(LocalDateTime.now());
            priceLogMapper.insert(log);

            sku.setPrice(newPrice);
            sku.setUpdateTime(LocalDateTime.now());
            return updateById(sku);
        }
        return false;
    }

    public List<GoodsPriceLog> listPriceLog(Long skuId) {
        return priceLogMapper.selectList(new LambdaQueryWrapper<GoodsPriceLog>()
                .eq(GoodsPriceLog::getSkuId, skuId)
                .orderByDesc(GoodsPriceLog::getCreateTime));
    }

    public Integer getWarningStock(Long merchantId) {
        if (merchantId == null) return 0;
        try {
            MerchantStockSetting s = stockSettingMapper.selectOne(new LambdaQueryWrapper<MerchantStockSetting>()
                    .eq(MerchantStockSetting::getMerchantId, merchantId));
            Integer v = s == null ? null : s.getWarningStock();
            return v == null || v < 0 ? 0 : v;
        } catch (Exception e) {
            return 0;
        }
    }

    @Transactional
    public boolean setWarningStock(Long merchantId, Integer warningStock) {
        if (merchantId == null || warningStock == null || warningStock < 0) return false;
        try {
            MerchantStockSetting existing = stockSettingMapper.selectOne(new LambdaQueryWrapper<MerchantStockSetting>()
                    .eq(MerchantStockSetting::getMerchantId, merchantId));
            LocalDateTime now = LocalDateTime.now();
            if (existing == null) {
                MerchantStockSetting s = new MerchantStockSetting();
                s.setMerchantId(merchantId);
                s.setWarningStock(warningStock);
                s.setUpdateTime(now);
                return stockSettingMapper.insert(s) > 0;
            }
            MerchantStockSetting patch = new MerchantStockSetting();
            patch.setId(existing.getId());
            patch.setWarningStock(warningStock);
            patch.setUpdateTime(now);
            return stockSettingMapper.updateById(patch) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public List<GoodsStockLog> listStockLog(Long merchantId, Long goodsId, Long skuId, Integer limit) {
        if (merchantId == null) return List.of();
        int pageSize = limit == null ? 100 : Math.max(1, Math.min(limit, 500));
        try {
            LambdaQueryWrapper<GoodsStockLog> qw = new LambdaQueryWrapper<GoodsStockLog>()
                    .eq(GoodsStockLog::getMerchantId, merchantId)
                    .orderByDesc(GoodsStockLog::getCreateTime)
                    .last("LIMIT " + pageSize);
            if (goodsId != null) qw.eq(GoodsStockLog::getGoodsId, goodsId);
            if (skuId != null) qw.eq(GoodsStockLog::getSkuId, skuId);
            return stockLogMapper.selectList(qw);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Transactional
    public boolean replaceByGoodsId(Long goodsId, List<GoodsSku> skus) {
        remove(new LambdaQueryWrapper<GoodsSku>().eq(GoodsSku::getGoodsId, goodsId));
        if (skus == null || skus.isEmpty()) {
            return true;
        }
        for (GoodsSku sku : skus) {
            if (sku == null) continue;
            sku.setId(null);
            sku.setGoodsId(goodsId);
            if (sku.getSpec() == null || sku.getSpec().isBlank()) {
                sku.setSpec("默认");
            }
            if (sku.getSpecParams() == null) {
                sku.setSpecParams("");
            }
            if (sku.getPrice() == null) {
                sku.setPrice(BigDecimal.ZERO);
            }
            if (sku.getStock() == null) {
                sku.setStock(0);
            }
            if (sku.getLockStock() == null) {
                sku.setLockStock(0);
            }
            if (sku.getSkuCode() == null || sku.getSkuCode().isBlank()) {
                sku.setSkuCode("SKU" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            }
            if (sku.getStatus() == null) {
                sku.setStatus(1);
            }
        }
        return saveBatch(skus);
    }
}
