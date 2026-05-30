package org.example.user.controller;

import jakarta.validation.Valid;
import org.example.common.Result;
import org.example.context.UserContext;
import org.example.dto.LiveCommentRequest;
import org.example.service.ShoppingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserDiscoveryController {
    private final ShoppingService shoppingService;

    public UserDiscoveryController(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @GetMapping("/recommendations")
    public Result<List<Map<String, Object>>> recommendations(@RequestParam(required = false) Integer scene,
                                                             @RequestParam(required = false) Long merchantId) {
        Long userId = UserContext.getCurrentUserId();
        if (userId != null) {
            return Result.ok(shoppingService.recommendations(userId, scene));
        }
        if (merchantId != null) {
            return Result.ok(shoppingService.merchantRecommendations(merchantId));
        }
        return Result.ok(shoppingService.publicRecommendations());
    }

    @GetMapping("/rankings")
    public Result<List<Map<String, Object>>> rankings(@RequestParam(defaultValue = "sales") String type,
                                                      @RequestParam(required = false) Integer categoryId,
                                                      @RequestParam(required = false) Long merchantId) {
        return Result.ok(shoppingService.ranks(mapRankType(type), categoryId, merchantId));
    }

    @GetMapping("/live-rooms")
    public Result<List<Map<String, Object>>> liveRooms(@RequestParam(required = false) Long merchantId) {
        return Result.ok(shoppingService.lives(merchantId));
    }

    @GetMapping("/live-rooms/{liveId}")
    public Result<Map<String, Object>> liveRoom(@PathVariable long liveId) {
        return Result.ok(shoppingService.liveDetail(liveId));
    }

    @GetMapping("/live-rooms/{liveId}/products")
    public Result<List<Map<String, Object>>> liveRoomProducts(@PathVariable long liveId) {
        return Result.ok(shoppingService.liveGoods(liveId));
    }

    @GetMapping("/live-rooms/{liveId}/comments")
    public Result<List<Map<String, Object>>> liveRoomComments(@PathVariable long liveId) {
        return Result.ok(shoppingService.liveComments(liveId));
    }

    @PostMapping("/live-rooms/{liveId}/comments")
    public Result<Void> createLiveComment(@PathVariable long liveId, @Valid @RequestBody LiveCommentRequest request) {
        shoppingService.addLiveComment(UserContext.requireCurrentUserId(), liveId, request);
        return Result.ok();
    }

    private Integer mapRankType(String type) {
        return switch (type == null ? "sales" : type) {
            case "favorite" -> 2;
            case "rating" -> 3;
            default -> 1;
        };
    }
}
