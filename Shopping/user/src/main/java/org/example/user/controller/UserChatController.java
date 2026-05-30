package org.example.user.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.example.common.Result;
import org.example.context.UserContext;
import org.example.dto.ChatMessageRequest;
import org.example.dto.ChatSessionRequest;
import org.example.service.ShoppingService;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserChatController {
    private final ShoppingService shoppingService;

    public UserChatController(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @PostMapping("/chat/sessions")
    public Result<Map<String, Object>> startSession(@Valid @RequestBody ChatSessionRequest request) {
        return Result.ok(shoppingService.startChatSession(UserContext.requireCurrentUserId(), request));
    }

    @GetMapping("/chat/sessions")
    public Result<List<Map<String, Object>>> sessions() {
        return Result.ok(shoppingService.chatSessions(UserContext.requireCurrentUserId()));
    }

    @GetMapping("/chat/sessions/{sessionId}")
    public Result<Map<String, Object>> sessionDetail(@PathVariable long sessionId) {
        return Result.ok(shoppingService.chatSessionDetail(UserContext.requireCurrentUserId(), sessionId));
    }

    @PostMapping("/chat/sessions/{sessionId}/messages")
    public Result<Map<String, Object>> sendMessage(@PathVariable long sessionId,
                                                   @Valid @RequestBody ChatMessageRequest request) {
        return Result.ok(shoppingService.sendChatMessage(UserContext.requireCurrentUserId(), sessionId, request));
    }

    @PutMapping("/chat/messages/{messageId}/revoke")
    public Result<Void> revokeMessage(@PathVariable long messageId) {
        shoppingService.revokeChatMessage(UserContext.requireCurrentUserId(), messageId);
        return Result.ok();
    }

    @PostMapping("/chat/uploads/image")
    public Result<Map<String, Object>> uploadChatImage(@RequestParam("file") MultipartFile file,
                                                       HttpServletRequest request) {
        return Result.ok(shoppingService.uploadChatImage(file, request));
    }

    @PostMapping("/chat/uploads/file")
    public Result<Map<String, Object>> uploadChatFile(@RequestParam("file") MultipartFile file,
                                                      HttpServletRequest request) {
        return Result.ok(shoppingService.uploadChatFile(file, request));
    }
}
