package com.hovvyoung.hpushServer.serverProcesser;

import com.hovvyoung.hpushServer.server.session.ServerSession;
import com.hovvyoung.hpushServer.server.session.SessionManger;
import com.hovvyoung.hpushCommon.bean.msg.ProtoMsg;
import com.hovvyoung.hpushCommon.util.Print;
import com.hovvyoung.hpushServer.server.session.LocalSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service("ChatRedirectProcesser")
public class ChatRedirectProcesser extends AbstractServerProcesser {
    @Override
    public ProtoMsg.HeadType op() {
        return ProtoMsg.HeadType.MESSAGE_REQUEST;
    }

    @Override
    public Boolean action(LocalSession fromSession, ProtoMsg.Message proto) {
        // 聊天处理
        ProtoMsg.MessageRequest msg = proto.getMessageRequest();
        Print.tcfo("chatMsg | from="
                + msg.getFrom()
                + " , to=" + msg.getTo()
                + " , content=" + msg.getContent());
        // 获取接收方的chatID
        String to = msg.getTo();
        // int platform = msg.getPlatform();
        List<ServerSession> toSessions = SessionManger.inst().getSessionsByUserId(to);
        if (toSessions == null || toSessions.size() == 0) {
            //接收方离线
            Print.tcfo("[" + to + "] 不在线，发送失败!");
        } else {

            toSessions.forEach((session)->{
                // 将IM消息发送到接收方
                session.writeAndFlush(proto);
            });
        }
        return null;
    }

}
