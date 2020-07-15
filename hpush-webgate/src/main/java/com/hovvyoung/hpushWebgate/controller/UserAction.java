package com.hovvyoung.hpushWebgate.controller;

import com.hovvyoung.hpushWebgate.entity.UserInfo;
import com.hovvyoung.hpushCommon.entity.ImNode;
import com.hovvyoung.hpushCommon.entity.LoginBack;
import com.hovvyoung.hpushCommon.util.JsonUtil;
import com.hovvyoung.hpushWebgate.loadbalance.ImLoadBalance;
import com.hovvyoung.hpushWebgate.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


import javax.annotation.Resource;

/**
 * WEB GATE
 * Created by 尼恩 at 疯狂创客圈
 */

//@EnableAutoConfiguration
@RestController
@RequestMapping(value = "/user", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
// produces: 返回json数据的字符编码为utf-8.：
public class UserAction {
    @Resource
    private UserService userService;
    @Resource
    private ImLoadBalance imLoadBalance;

    /**
     * Web短连接登录
     *
     * @param username 用户名
     * @param password 命名
     * @return 登录结果
     */

    @RequestMapping(value = "/login/{username}/{password}",method = RequestMethod.GET)
    public String loginAction(
            @PathVariable("username") String username,
            @PathVariable("password") String password) {

        // 网关本身也是一个netty client, 和netty集群的所有节点保持着长连接
        // 1. 收到用户的登录请求
        // 2. 找到最佳的netty服务器，将登录请求发往netty
        // 3. 或者web验证服务器和netty服务器分离，登录后的session放在redis
        //  但是Netty的长连接是登录时建立的，需要改成接收到一个新客户信息时，去redis验证session/取回本地
        //  然后建立长连接再做消息转发

        UserInfo user = new UserInfo();
        user.setUsername(username);
        user.setPwd(password);

//        User loginUser = userService.login(user);

        return JsonUtil.pojoToJson(new LoginBack());
    }

    @RequestMapping(value = "/getImNode",method = RequestMethod.GET)
    public String getImNode() {
        LoginBack back = new LoginBack();
        /**
         * 取得最佳(连接数最少）的Netty服务器
         */
        ImNode bestWorker = imLoadBalance.getBestWorker();
        back.setImNode(bestWorker);
        return JsonUtil.pojoToJson(back);
    }


    /**
     * 从zookeeper中删除所有IM节点
     *
     * @return 删除结果
     */
    @RequestMapping(value = "/removeWorkers",method = RequestMethod.GET)
    public String removeWorkers(){
        imLoadBalance.removeWorkers();
        return "已经删除";
    }

}