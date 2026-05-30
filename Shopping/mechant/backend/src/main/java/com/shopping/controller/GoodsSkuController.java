package com.shopping.controller;

import com.shopping.entity.GoodsPriceLog;
import com.shopping.entity.GoodsSku;
import com.shopping.entity.GoodsStockLog;
import com.shopping.service.GoodsSkuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/goods-sku")
public class GoodsSkuController {

    @Autowired
    private GoodsSkuService skuService;

    @GetMapping("/list")
    public List<GoodsSku> list(@RequestParam Long goodsId) {
        return skuService.listByGoodsId(goodsId);
    }

    @PutMapping("/{id}/stock")
    public boolean updateStock(@PathVariable Long id, @RequestParam Integer stock) {
        return skuService.updateStock(id, stock);
    }

    @PutMapping("/{id}/price")
    public boolean updatePrice(@PathVariable Long id, @RequestParam java.math.BigDecimal price) {
        return skuService.updatePrice(id, price);
    }

    @GetMapping("/{id}/price-log")
    public List<GoodsPriceLog> listPriceLog(@PathVariable Long id) {
        return skuService.listPriceLog(id);
    }

    @GetMapping("/stock-warning")
    public Integer getStockWarning(@RequestParam Long merchantId) {
        return skuService.getWarningStock(merchantId);
    }

    @PutMapping("/stock-warning")
    public boolean setStockWarning(@RequestParam Long merchantId, @RequestParam Integer warningStock) {
        return skuService.setWarningStock(merchantId, warningStock);
    }

    @GetMapping("/stock-log/list")
    public List<GoodsStockLog> listStockLog(
            @RequestParam Long merchantId,
            @RequestParam(required = false) Long goodsId,
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Integer limit
    ) {
        return skuService.listStockLog(merchantId, goodsId, skuId, limit);
    }

    @PutMapping("/goods/{goodsId}")
    public boolean replaceByGoodsId(@PathVariable Long goodsId, @RequestBody List<GoodsSku> skus) {
        return skuService.replaceByGoodsId(goodsId, skus);
    }
}
