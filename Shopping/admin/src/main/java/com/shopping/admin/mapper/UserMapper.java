package com.shopping.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopping.admin.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
