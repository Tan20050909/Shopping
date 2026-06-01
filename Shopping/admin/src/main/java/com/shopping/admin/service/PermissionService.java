package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.entity.Permission;
import com.shopping.admin.mapper.PermissionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService extends ServiceImpl<PermissionMapper, Permission> {

    public List<Permission> listByModule(String module) {
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        if (module != null) wrapper.eq(Permission::getModule, module);
        wrapper.orderByAsc(Permission::getSortNo);
        return list(wrapper);
    }

    public List<Permission> getAllPermissions() {
        return list(new LambdaQueryWrapper<Permission>()
                .eq(Permission::getStatus, 1)
                .orderByAsc(Permission::getModule, Permission::getSortNo));
    }
}
