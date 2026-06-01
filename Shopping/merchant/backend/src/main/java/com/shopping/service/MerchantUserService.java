package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.MerchantUser;
import com.shopping.mapper.MerchantUserMapper;
import org.springframework.stereotype.Service;

@Service
public class MerchantUserService extends ServiceImpl<MerchantUserMapper, MerchantUser> {
    
    public MerchantUser login(String username, String password) {
        return getOne(new LambdaQueryWrapper<MerchantUser>()
                .and(wrapper -> wrapper.eq(MerchantUser::getUsername, username).or().eq(MerchantUser::getPhone, username))
                .eq(MerchantUser::getPassword, password));
    }
    
    public MerchantUser register(String username, String password, String phone) {
        MerchantUser user = new MerchantUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setPhone(phone);
        user.setStatus(1);
        save(user);
        return user;
    }
    
    public MerchantUser getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<MerchantUser>()
                .eq(MerchantUser::getUsername, username));
    }
}
