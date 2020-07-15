package com.hovvyoung.hpushClient.clientMsgProcesser;


import com.hovvyoung.hpushClient.client.ClientSession;
import com.hovvyoung.hpushCommon.bean.msg.ProtoMsg;

/**
 * 操作类
 */
public interface Proc {

    ProtoMsg.HeadType op();

    void action(ClientSession ch, ProtoMsg.Message proto) throws Exception;

}
