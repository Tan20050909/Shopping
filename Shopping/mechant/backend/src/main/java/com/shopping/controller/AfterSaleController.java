package com.shopping.controller;

import com.shopping.entity.AfterSale;
import com.shopping.entity.AfterSaleDetail;
import com.shopping.entity.Logistics;
import com.shopping.service.AfterSaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/after-sale")
public class AfterSaleController {

    @Autowired
    private AfterSaleService afterSaleService;

    @GetMapping("/list")
    public List<AfterSale> list(@RequestParam Long merchantId, @RequestParam(required = false) Integer status) {
        return afterSaleService.listByMerchantId(merchantId, status);
    }

    @GetMapping("/{id}")
    public AfterSale detail(@PathVariable Long id) {
        return afterSaleService.getById(id);
    }

    @GetMapping("/{id}/detail")
    public AfterSaleDetail detailView(@PathVariable Long id) {
        AfterSaleDetail detail = afterSaleService.detail(id);
        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "售后单不存在");
        }
        return detail;
    }

    @PutMapping("/{id}/handle")
    public boolean handle(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String evidence
    ) {
        boolean ok = afterSaleService.handle(id, status, remark, evidence);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "处理失败");
        }
        return true;
    }

    @PutMapping("/{id}/complete")
    public boolean complete(@PathVariable Long id) {
        boolean ok = afterSaleService.complete(id);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "售后状态更新失败");
        }
        return true;
    }

    @PostMapping("/{id}/buyer-logistics")
    public Logistics uploadBuyerLogistics(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String expressCompany = body.getOrDefault("expressCompany", "");
        String expressNo = body.getOrDefault("expressNo", "");
        if (expressCompany == null || expressCompany.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "快递公司不能为空");
        }
        if (expressNo == null || expressNo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "快递单号不能为空");
        }
        Logistics logistics = afterSaleService.uploadBuyerLogistics(id, expressCompany, expressNo);
        if (logistics == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "售后单或订单信息不完整");
        }
        return logistics;
    }

    @PostMapping("/{id}/merchant-ship")
    public Logistics merchantAfterSaleShip(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String expressCompany = body.getOrDefault("expressCompany", "");
        String expressNo = body.getOrDefault("expressNo", "");
        if (expressCompany == null || expressCompany.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "快递公司不能为空");
        }
        if (expressNo == null || expressNo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "快递单号不能为空");
        }
        Logistics logistics = afterSaleService.merchantAfterSaleShip(id, expressCompany, expressNo);
        if (logistics == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "售后单或订单信息不完整");
        }
        return logistics;
    }
}
