package com.hovvyoung.hpushWebgate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 *  webgate模块主要是用来接收用户端的 请求服务端节点，注册用户，（登录请求），查看用户信息等
 * */

@SpringBootApplication
@EntityScan(basePackages ="com.hovvyoung.hpushWebgate" )
public class WebgateBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(WebgateBootstrap.class, args);
    }
}
