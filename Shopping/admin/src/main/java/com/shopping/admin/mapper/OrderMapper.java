package com.shopping.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopping.admin.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
