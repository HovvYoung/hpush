package com.hovvyoung.hpushClient.clientHandler;


import com.hovvyoung.hpushClient.client.ClientSession;
import com.hovvyoung.hpushClient.client.CommandController;
import com.hovvyoung.hpushCommon.ProtoInstant;
import com.hovvyoung.hpushCommon.bean.msg.ProtoMsg;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@ChannelHandler.Sharable
@Service("LoginResponceHandler")
public class LoginResponceHandler extends ChannelInboundHandlerAdapter {

    @Autowired
    CommandController commandController;
    @Autowired
    HeartBeatClientHandler heartBeatClientHandler;
    /**
     * 业务逻辑处理
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        //判断消息实例
        if (null == msg || !(msg instanceof ProtoMsg.Message)) {
            super.channelRead(ctx, msg);
            return;
        }

        //判断类型
        ProtoMsg.Message pkg = (ProtoMsg.Message) msg;
        ProtoMsg.HeadType headType = ((ProtoMsg.Message) msg).getType();
        if (!headType.equals(ProtoMsg.HeadType.LOGIN_RESPONSE)) {
            super.channelRead(ctx, msg);
            return;
        }


        //判断返回是否成功.服务端会回送LoginResponse message
        ProtoMsg.LoginResponse info = pkg.getLoginResponse();

        ProtoInstant.ResultCodeEnum result =
                ProtoInstant.ResultCodeEnum.values()[info.getCode()];

        if (!result.equals(ProtoInstant.ResultCodeEnum.SUCCESS)) {
            log.info(result.getDesc());
            log.info("step3：登录Netty 服务节点失败");
        } else {
            // 保存sessionId
            ClientSession session = ctx.channel().attr(ClientSession.SESSION_KEY).get();
            session.setSessionId(pkg.getSessionId());
            session.setLogin(true);

            log.info("step3：登录Netty 服务节点成功");
            // 这是读取到回复信息的notify
            commandController.notifyCommandThread();

            //去掉登录用的handler
            ctx.channel().pipeline().addAfter("loginResponseHandler","heartBeatClientHandler",heartBeatClientHandler);
            ctx.channel().pipeline().remove("loginResponseHandler");

        }


    }


}
