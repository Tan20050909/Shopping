package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.MessageTemplate;
import com.shopping.admin.service.MessageTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/message-template")
@RequiredArgsConstructor
public class MessageTemplateController {

    private final MessageTemplateService messageTemplateService;

    @GetMapping("/list")
    public Result<List<MessageTemplate>> list() {
        return Result.success(messageTemplateService.listAll());
    }

    @PostMapping
    public Result<Void> add(@RequestBody MessageTemplate template) {
        messageTemplateService.save(template);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody MessageTemplate template) {
        messageTemplateService.updateById(template);
        return Result.success();
    }
}
