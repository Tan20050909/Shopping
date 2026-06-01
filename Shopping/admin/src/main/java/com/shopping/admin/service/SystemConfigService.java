package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.entity.SystemConfig;
import com.shopping.admin.exception.BusinessException;
import com.shopping.admin.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SystemConfigService extends ServiceImpl<SystemConfigMapper, SystemConfig> {

    public List<SystemConfig> listByGroup(String configGroup) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        if (configGroup != null && !configGroup.isBlank()) {
            wrapper.eq(SystemConfig::getConfigGroup, configGroup);
        }
        wrapper.orderByAsc(SystemConfig::getSortNo);
        return list(wrapper);
    }

    public Map<String, String> getConfigMap(String configGroup) {
        List<SystemConfig> configs = listByGroup(configGroup);
        return configs.stream().collect(Collectors.toMap(
                SystemConfig::getConfigKey,
                c -> c.getConfigValue() != null ? c.getConfigValue() : "",
                (v1, v2) -> v1
        ));
    }

    public void updateConfigValue(Long configId, String configValue) {
        SystemConfig config = getById(configId);
        if (config == null) {
            throw new BusinessException("配置项不存在");
        }
        config.setConfigValue(configValue);
        updateById(config);
    }

    public String getConfigValue(String configKey) {
        SystemConfig config = getOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey));
        return config != null ? config.getConfigValue() : null;
    }
}
