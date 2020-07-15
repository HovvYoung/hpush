package com.hovvyoung.hpushCommon.constants;

import io.netty.util.AttributeKey;

/**
 * create by 尼恩 @ 疯狂创客圈
 **/
public class ServerConstants {

    //工作节点的父路径
    public static final String MANAGE_PATH = "/im/nodes";

    //工作节点的路径前缀
    public static final String PATH_PREFIX = MANAGE_PATH + "/seq-";

    //统计用户数的znode
    public static final String COUNTER_PATH = "/im/OnlineCounter";

    public static final String WEB_URL = "http://localhost:8080";


    public static final AttributeKey<String> CHANNEL_NAME=
            AttributeKey.valueOf("CHANNEL_NAME");

}
