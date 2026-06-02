package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.ChatMessage;
import com.shopping.admin.service.ChatMessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) Long fromId,
                          @RequestParam(required = false) Long toId) {
        return Result.success(chatMessageService.listMessages(current, size, fromId, toId));
    }

    @PostMapping
    public Result<Void> send(@RequestBody ChatMessage message, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        message.setFromId(adminId != null ? adminId : 1L);
        message.setFromType(3);
        message.setIsRead(0);
        // session_id 字段 NOT NULL 无默认值，admin 无会话概念，填入 0 占位
        message.setSessionId(0L);
        chatMessageService.save(message);
        return Result.success();
    }

}
