package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.service.GoodsReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class GoodsReviewController {

    private final GoodsReviewService goodsReviewService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) Long goodsId,
                          @RequestParam(required = false) Integer isHidden) {
        return Result.success(goodsReviewService.listReviews(current, size, goodsId, isHidden));
    }

    @PostMapping("/reply")
    public Result<Void> reply(@RequestBody ReplyRequest request) {
        goodsReviewService.replyReview(request.reviewId(), request.merchantReply(), request.adminId());
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        goodsReviewService.updateReviewStatus(id, request.isHidden());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        goodsReviewService.softDeleteReview(id);
        return Result.success();
    }

    public record ReplyRequest(Long reviewId, String merchantReply, Long adminId) {}

    public record StatusRequest(Integer isHidden) {}
}
