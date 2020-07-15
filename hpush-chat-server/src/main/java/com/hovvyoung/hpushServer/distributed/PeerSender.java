package com.hovvyoung.hpushServer.distributed;

import com.hovvyoung.hpushCommon.bean.UserDTO;
import com.hovvyoung.hpushCommon.bean.msg.Notification;
import com.hovvyoung.hpushCommon.bean.msg.ProtoMsg;
import com.hovvyoung.hpushCommon.codec.ProtobufDecoder;
import com.hovvyoung.hpushCommon.codec.ProtobufEncoder;
import com.hovvyoung.hpushCommon.entity.ImNode;
import com.hovvyoung.hpushCommon.util.JsonUtil;
import com.hovvyoung.hpushServer.protoBuilder.NotificationMsgBuilder;
import com.hovvyoung.hpushServer.serverHandler.ImNodeExceptionHandler;
import com.hovvyoung.hpushServer.serverHandler.ImNodeHeartBeatClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * create by 尼恩 @ 疯狂创客圈
 **/
@Slf4j
@Data
public class PeerSender {

    private Channel channel;

    private ImNode node;
    /**
     * 唯一标记
     */
    private boolean connectFlag = false;
    private UserDTO user;

    GenericFutureListener<ChannelFuture> closeListener = (ChannelFuture f) ->
    {
        log.info("分布式连接已经断开……{}", node.toString());
        channel = null;
        connectFlag = false;
    };

    private GenericFutureListener<ChannelFuture> connectedListener = (ChannelFuture f) ->
    {
        final EventLoop eventLoop = f.channel().eventLoop();
        if (!f.isSuccess()) {
            log.info("连接失败!在10s之后准备尝试重连!");
            eventLoop.schedule(() -> PeerSender.this.doConnect(), 10, TimeUnit.SECONDS);

            connectFlag = false;
        } else {
            connectFlag = true;

            log.info(new Date() + "分布式节点连接成功:{}", node.toString());

            channel = f.channel();
            channel.closeFuture().addListener(closeListener);

            /**
             * 发送链接成功的通知
             */
            Notification<ImNode> notification=new Notification<>(ImWorker.getInst().getLocalNodeInfo());
            notification.setType(Notification.CONNECT_FINISHED);
            String json= JsonUtil.pojoToJson(notification);
            ProtoMsg.Message pkg = NotificationMsgBuilder.buildNotification(json);
            writeAndFlush(pkg);
        }
    };


    private Bootstrap b;
    private EventLoopGroup g;

    public PeerSender(ImNode n) {
        this.node = n;

        /**
         * 客户端的是Bootstrap，服务端的则是 ServerBootstrap。
         * 都是AbstractBootstrap的子类。
         **/

        b = new Bootstrap();
        /**
         * 通过nio方式来接收连接和处理连接
         */

        g = new NioEventLoopGroup();


    }

    /**
     * 重连
     */
    public void doConnect() {

        // 服务器ip地址
        String host = node.getHost();
        // 服务器端口
        int port =node.getPort();

        try {
            if (b != null && b.group() == null) {
                b.group(g);
                b.channel(NioSocketChannel.class);
                b.option(ChannelOption.SO_KEEPALIVE, true);
                b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
                b.remoteAddress(host, port);

                // 设置通道初始化
                b.handler(
                        new ChannelInitializer<SocketChannel>() {
                            public void initChannel(SocketChannel ch) {
                                ch.pipeline().addLast("decoder", new ProtobufDecoder());
                                ch.pipeline().addLast("encoder", new ProtobufEncoder());
                                ch.pipeline().addLast("imNodeHeartBeatClientHandler",new ImNodeHeartBeatClientHandler());
                                ch.pipeline().addLast("exceptionHandler",new ImNodeExceptionHandler());
                            }
                        }
                );
                log.info(new Date() + "开始连接分布式节点:{}", node.toString());

                ChannelFuture f = b.connect();
                f.addListener(connectedListener);


                // 阻塞
//                 f.channel().closeFuture().sync();
            } else if (b.group() != null) {
                log.info(new Date() + "再一次开始连接分布式节点", node.toString());
                ChannelFuture f = b.connect();
                f.addListener(connectedListener);
            }
        } catch (Exception e) {
            log.info("客户端连接失败!" + e.getMessage());
        }

    }

    public void stopConnecting() {
        g.shutdownGracefully();
        connectFlag = false;
    }

    public void writeAndFlush(Object pkg) {
        if (connectFlag == false) {
            log.error("分布式节点未连接:", node.toString());
            return;
        }
        channel.writeAndFlush(pkg);
    }


}
