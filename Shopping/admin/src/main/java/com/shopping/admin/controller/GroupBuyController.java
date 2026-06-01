package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.GroupBuy;
import com.shopping.admin.service.GroupBuyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/group-buy")
@RequiredArgsConstructor
public class GroupBuyController {

    private final GroupBuyService groupBuyService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) Integer groupStatus) {
        return Result.success(groupBuyService.listGroupBuys(current, size, groupStatus));
    }

    @PostMapping
    public Result<Void> add(@RequestBody GroupBuy groupBuy) {
        groupBuyService.save(groupBuy);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody GroupBuy groupBuy) {
        groupBuyService.updateById(groupBuy);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        groupBuyService.updateGroupBuyStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        groupBuyService.removeById(id);
        return Result.success();
    }
}
