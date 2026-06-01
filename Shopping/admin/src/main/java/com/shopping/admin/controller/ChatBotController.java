package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.ChatBotLog;
import com.shopping.admin.exception.BusinessException;
import com.shopping.admin.service.ChatBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat-bot")
@RequiredArgsConstructor
public class ChatBotController {

    private final ChatBotService chatBotService;

    @PostMapping("/ask")
    public Result<Map<String, Object>> ask(@RequestBody AskRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            throw new BusinessException(400, "问题不能为空");
        }
        Long adminId = request.adminId();
        Map<String, Object> result = chatBotService.ask(request.question(), adminId);
        return Result.success(result);
    }

    @PostMapping("/feedback")
    public Result<Void> feedback(@RequestBody FeedbackRequest request) {
        if (request.logId() == null) {
            throw new BusinessException(400, "logId不能为空");
        }
        if (request.helpful() == null) {
            throw new BusinessException(400, "helpful不能为空");
        }
        chatBotService.feedback(request.logId(), request.helpful());
        return Result.success();
    }

    @GetMapping("/quick-questions")
    public Result<List<Map<String, String>>> getQuickQuestions() {
        return Result.success(chatBotService.getQuickQuestions());
    }

    @GetMapping("/history")
    public Result<PageResult<ChatBotLog>> getHistory(
            @RequestParam(required = false) Long adminId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return Result.success(chatBotService.getHistory(adminId, current, size));
    }

    public record AskRequest(String question, Long adminId) {}
    public record FeedbackRequest(Long logId, Integer helpful) {}
}
