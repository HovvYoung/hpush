package com.hovvyoung.hpushClient.ClientSender;

import com.hovvyoung.hpushClient.client.ClientSession;
import com.hovvyoung.hpushClient.client.CommandController;
import com.hovvyoung.hpushCommon.bean.UserDTO;
import com.hovvyoung.hpushCommon.bean.msg.ProtoMsg;
import com.hovvyoung.hpushCommon.concurrent.CallbackTask;
import com.hovvyoung.hpushCommon.concurrent.CallbackTaskScheduler;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Data
@Slf4j
public abstract class BaseSender {


    private UserDTO user;
    private ClientSession session;

    @Autowired
    protected CommandController commandClient;


    public boolean isConnected() {
        if (null == session) {
            log.info("session is null");
            return false;
        }
        return session.isConnected();
    }

    public boolean isLogin() {
        if (null == session) {
            log.info("session is null");
            return false;
        }
        return session.isLogin();
    }

    public void sendMsg(ProtoMsg.Message message) {

        CallbackTaskScheduler.add(new CallbackTask<Boolean>() {
            @Override
            public Boolean execute() throws Exception {
//                if (null == getSession()) {
//                    throw new Exception("session is null");
//                }
                if (!isConnected()) {
                    log.info("连接还没成功");
                    throw new Exception("连接还没成功");
                }
                final Boolean[] isSuccess = {false};

                ChannelFuture channelFuture = getSession().witeAndFlush(message);
                channelFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future)
                            throws Exception {
                        // 回调
                        if (future.isSuccess()) {
                            isSuccess[0] = true;
                        }
                    }

                });

                try {
                    channelFuture.sync();
                } catch (InterruptedException e) {
                    isSuccess[0] = false;
                    e.printStackTrace();
                    throw new Exception("error occur");
                }

                return isSuccess[0];
            }

            @Override
            public void onBack(Boolean b) {
                if (b) {
                    BaseSender.this.sendSucced(message);

                } else {
                    BaseSender.this.sendfailed(message);

                }

            }

            @Override
            public void onException(Throwable t) {
                BaseSender.this.sendException(message);


            }
        });

    }

    protected void sendSucced(ProtoMsg.Message message) {
            log.info("发送成功");

    }

    protected void sendfailed(ProtoMsg.Message message) {
        log.info("发送失败");
    }

    protected void sendException(ProtoMsg.Message message) {
        log.info("发送消息出现异常");

    }

}
