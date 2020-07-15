package com.hovvyoung.hpushServer.serverHandler;

import com.hovvyoung.hpushCommon.bean.msg.ProtoMsg;
import com.hovvyoung.hpushCommon.concurrent.FutureTaskScheduler;
import com.hovvyoung.hpushServer.server.session.LocalSession;
import com.hovvyoung.hpushServer.server.session.SessionManger;
import com.hovvyoung.hpushServer.serverProcesser.ChatRedirectProcesser;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service("ChatRedirectHandler")
@ChannelHandler.Sharable
public class ChatRedirectHandler extends ChannelInboundHandlerAdapter {

    @Autowired
    ChatRedirectProcesser redirectProcesser;

    @Autowired
    SessionManger sessionManger;

    /**
     * 收到消息
     */
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        //判断消息实例
        if (null == msg || !(msg instanceof ProtoMsg.Message)) {
            super.channelRead(ctx, msg);
            return;
        }

        //判断消息类型
        ProtoMsg.Message pkg = (ProtoMsg.Message) msg;
        ProtoMsg.HeadType headType = ((ProtoMsg.Message) msg).getType();
        if (!headType.equals(redirectProcesser.op())) {
            super.channelRead(ctx, msg);
            return;
        }
        //异步处理转发的逻辑
        FutureTaskScheduler.add(() ->
        {
            //判断是否登录
            LocalSession session = LocalSession.getSession(ctx);
            if (null != session && session.isLogin()) {

                redirectProcesser.action(session, pkg);
                return;
            }

        });
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx)
            throws Exception {
        LocalSession session = ctx.channel().attr(LocalSession.SESSION_KEY).get();

        if (null != session && session.isValid()) {
            session.close();
            sessionManger.removeLocalSession(session.getSessionId());
        }
    }
}
