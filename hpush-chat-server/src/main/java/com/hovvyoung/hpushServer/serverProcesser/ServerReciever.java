package com.hovvyoung.hpushServer.serverProcesser;

import com.hovvyoung.hpushCommon.bean.msg.ProtoMsg;
import com.hovvyoung.hpushServer.server.session.LocalSession;


/**
 * 操作类
 */
public interface ServerReciever
{

    ProtoMsg.HeadType op();

    Boolean action(LocalSession ch, ProtoMsg.Message proto);

}
