package com.hovvyoung.hpushServer.serverHandler;

import com.hovvyoung.hpushCommon.bean.msg.ProtoMsg;
import com.hovvyoung.hpushCommon.concurrent.CallbackTask;
import com.hovvyoung.hpushCommon.concurrent.CallbackTaskScheduler;
import com.hovvyoung.hpushServer.server.session.LocalSession;
import com.hovvyoung.hpushServer.server.session.SessionManger;
import com.hovvyoung.hpushServer.serverProcesser.LoginProcesser;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service("LoginRequestHandler")
@ChannelHandler.Sharable
public class LoginRequestHandler extends ChannelInboundHandlerAdapter {

    @Autowired
    LoginProcesser loginProcesser;

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

        //取得请求类型
        ProtoMsg.HeadType headType = pkg.getType();

        if (!headType.equals(loginProcesser.op())) {
            super.channelRead(ctx, msg);
            return;
        }

        // TODO 判断是否重复登录
//        LocalSession session= LocalSession.getSession(ctx);
//        if (null != session && session.isLogin()) {
//            log.error("用户已登录");
//            return;
//        };

        //异步任务，处理登录的逻辑
        LocalSession finalSession = new LocalSession(ctx.channel());
        CallbackTaskScheduler.add(new CallbackTask<Boolean>() {
            @Override
            public Boolean execute() throws Exception {
                return loginProcesser.action(finalSession, pkg);
            }

            //异步任务返回
            @Override
            public void onBack(Boolean r) {
                if (r) {
                    ctx.pipeline().remove(LoginRequestHandler.this);
                    log.info("登录成功:" + finalSession.getUser());

                } else {
                    SessionManger.inst().closeSession(ctx);

                    log.info("登录失败:" + finalSession.getUser());

                }

            }
            //异步任务异常
            @Override
            public void onException(Throwable t) {
                SessionManger.inst().closeSession(ctx);

                log.info("登录失败:" + finalSession.getUser());

            }
        });

    }


}
