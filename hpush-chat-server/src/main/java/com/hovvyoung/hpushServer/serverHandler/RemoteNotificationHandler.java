package com.hovvyoung.hpushServer.serverHandler;

import com.google.gson.reflect.TypeToken;
import com.hovvyoung.hpushServer.server.session.RemoteSession;
import com.hovvyoung.hpushServer.server.session.SessionManger;
import com.hovvyoung.hpushCommon.bean.msg.Notification;
import com.hovvyoung.hpushCommon.bean.msg.ProtoMsg;
import com.hovvyoung.hpushCommon.constants.ServerConstants;
import com.hovvyoung.hpushCommon.entity.ImNode;
import com.hovvyoung.hpushCommon.util.JsonUtil;
import com.hovvyoung.hpushServer.server.session.LocalSession;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("RemoteNotificationHandler")
@ChannelHandler.Sharable
public class RemoteNotificationHandler
        extends ChannelInboundHandlerAdapter {

    /**
     * 收到消息
     */
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if (null == msg || !(msg instanceof ProtoMsg.Message)) {
            super.channelRead(ctx, msg);
            return;
        }

        ProtoMsg.Message pkg = (ProtoMsg.Message) msg;

        //取得请求类型,如果不是通知消息，直接跳过
        ProtoMsg.HeadType headType = pkg.getType();

        if (!headType.equals(ProtoMsg.HeadType.MESSAGE_NOTIFICATION)) {
            super.channelRead(ctx, msg);
            return;
        }

        //处理消息的内容
        ProtoMsg.MessageNotification notificationPkg = pkg.getNotification();
        String json = notificationPkg.getJson();

        Notification<RemoteSession> notification =
                JsonUtil.jsonToPojo(json, new TypeToken<Notification<RemoteSession>>() {
                }.getType());


        //节点的链接成功
        if (notification.getType() == Notification.CONNECT_FINISHED) {

            Notification<ImNode> nodInfo =
                    JsonUtil.jsonToPojo(json, new TypeToken<Notification<ImNode>>() {
                    }.getType());


            log.info("收到分布式节点连接成功通知, node={}", json);

            ctx.pipeline().remove("loginRequest");
            ctx.channel().attr(ServerConstants.CHANNEL_NAME).set(JsonUtil.pojoToJson(nodInfo));
        }

        //上线的通知
        if (notification.getType() == Notification.SESSION_ON) {
            log.info("收到用户上线通知, node={}", json);
            RemoteSession remoteSession = notification.getData();
            SessionManger.inst().addRemoteSession(remoteSession);
        }

        //下线的通知
        if (notification.getType() == Notification.SESSION_OFF) {
            log.info("收到用户下线通知, node={}", json);
            RemoteSession remoteSession = notification.getData();
            SessionManger.inst().removeRemoteSession(remoteSession.getSessionId());
        }


    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx)
            throws Exception {
        LocalSession session = LocalSession.getSession(ctx);

        if (null != session) {

            session.unbind();

        }
    }
}
