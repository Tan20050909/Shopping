package com.shopping.controller;

import com.shopping.entity.GoodsComment;
import com.shopping.entity.GoodsCommentAppeal;
import com.shopping.service.GoodsCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/goods-comment")
public class GoodsCommentController {

    @Autowired
    private GoodsCommentService commentService;

    @GetMapping("/list")
    public List<GoodsComment> list(@RequestParam Long merchantId, @RequestParam(required = false) Integer includeInvalid) {
        return commentService.listByMerchantId(merchantId, includeInvalid);
    }

    @PutMapping("/{id}/reply")
    public boolean reply(@PathVariable Long id, @RequestParam String reply) {
        return commentService.reply(id, reply);
    }

    @DeleteMapping("/{id}/reply")
    public boolean deleteReply(@PathVariable Long id, @RequestParam(required = false) Integer index) {
        return commentService.deleteReply(id, index);
    }

    @PutMapping("/{id}/top")
    public boolean setTop(@PathVariable Long id, @RequestParam Integer isTop) {
        return commentService.setTop(id, isTop);
    }

    @PostMapping("/{id}/appeal")
    public boolean appeal(@PathVariable Long id, @RequestParam Long merchantId, @RequestBody Map<String, String> body) {
        String reason = body == null ? null : body.getOrDefault("reason", "");
        String evidence = body == null ? null : body.getOrDefault("evidence", "");
        boolean ok = commentService.submitAppeal(id, merchantId, reason, evidence);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "申诉提交失败");
        }
        return true;
    }

    @GetMapping("/appeal/list")
    public List<GoodsCommentAppeal> listAppeals(@RequestParam Long merchantId, @RequestParam(required = false) Integer status) {
        return commentService.listAppeals(merchantId, status);
    }

    @PutMapping("/appeal/{appealId}/handle")
    public boolean handleAppeal(
            @PathVariable Long appealId,
            @RequestParam Integer status,
            @RequestParam(required = false) Long handlerId,
            @RequestParam(required = false) String remark
    ) {
        boolean ok = commentService.handleAppeal(appealId, status, handlerId, remark);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "处理失败");
        }
        return true;
    }
}
