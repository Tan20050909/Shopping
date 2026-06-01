package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.entity.MessageTemplate;
import com.shopping.admin.mapper.MessageTemplateMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageTemplateService extends ServiceImpl<MessageTemplateMapper, MessageTemplate> {

    public List<MessageTemplate> listAll() {
        return list(new LambdaQueryWrapper<MessageTemplate>()
                .eq(MessageTemplate::getStatus, 1)
                .orderByAsc(MessageTemplate::getTemplateId));
    }
}
