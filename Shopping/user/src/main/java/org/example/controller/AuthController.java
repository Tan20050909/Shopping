package org.example.controller;

import jakarta.validation.Valid;
import org.example.common.Result;
import org.example.dto.LoginRequest;
import org.example.service.ShoppingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final ShoppingService shoppingService;

    public AuthController(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(shoppingService.login(request));
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String phone = body == null ? null : body.get("phone");
        String password = body == null ? null : body.get("password");
        return Result.ok(shoppingService.register(phone, password));
    }
}
