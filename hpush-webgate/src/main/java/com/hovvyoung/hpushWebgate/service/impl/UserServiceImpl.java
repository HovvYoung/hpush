package com.hovvyoung.hpushWebgate.service.impl;

import com.hovvyoung.hpushWebgate.entity.UserInfo;
import com.hovvyoung.hpushWebgate.entity.UserPO;
import com.hovvyoung.hpushWebgate.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Slf4j
@Service
//@FeignClient(value = "")
//通过feign远程调用方法
public class UserServiceImpl implements UserService {

//    @Resource
//    private UserMapper userMapper;

    @Override
    public UserPO login(UserInfo user) {

        return null;
    }

    @Override
    public UserPO register() {
        return null;
    }

    @Override
    public UserPO getById(String userid) {
        return null;
    }

    @CacheEvict(value = "CrazyIMKey:User:", key = "#userid")
    public int deleteById(String userid) {
        return 0;
    }

}
