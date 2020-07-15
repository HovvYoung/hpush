package com.hovvyoung.hpushServer.server.session;

/**
 * create by 尼恩 @ 疯狂创客圈
 **/
public interface ServerSession {
    void writeAndFlush(Object pkg);

    String getSessionId();

    boolean isValid();
}

