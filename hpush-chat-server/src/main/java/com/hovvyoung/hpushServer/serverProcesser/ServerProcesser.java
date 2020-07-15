package com.hovvyoung.hpushServer.serverProcesser;


import com.hovvyoung.hpushServer.server.session.LocalSession;
import com.hovvyoung.hpushCommon.bean.msg.ProtoMsg;

/**
 * 操作类
 */
public interface ServerProcesser {

    ProtoMsg.HeadType type();

    boolean action(LocalSession ch, ProtoMsg.Message proto);

}
