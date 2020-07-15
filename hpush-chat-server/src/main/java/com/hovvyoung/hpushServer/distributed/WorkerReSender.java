package com.hovvyoung.hpushServer.distributed;

import com.hovvyoung.hpushCommon.codec.ProtobufEncoder;
import com.hovvyoung.hpushCommon.entity.ImNode;
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
public class WorkerReSender {
    //连接远程节点的Netty 通道
    private Channel channel;

    //连接远程节点的POJO信息
    private ImNode remoteNode;

    //连接标记
    private boolean connectFlag = false;

    GenericFutureListener<ChannelFuture> closeListener = (ChannelFuture f) ->
    {
        log.info("分布式连接已经被动断开……", remoteNode.toString());
        channel = null;
        connectFlag = false;
        PeerManager.getInst().remove(remoteNode);
        stopConnecting();
    };

    private GenericFutureListener<ChannelFuture> connectedListener = (ChannelFuture f) ->
    {
        final EventLoop eventLoop = f.channel().eventLoop();
        if (!f.isSuccess()) {
            log.info("连接失败!在10s之后准备尝试重连!");
            eventLoop.schedule(() -> WorkerReSender.this.doConnect(), 10, TimeUnit.SECONDS);

            connectFlag = false;
        } else {
            connectFlag = true;

            log.info("分布式IM节点连接成功:", remoteNode.toString());
            channel = f.channel();
            channel.closeFuture().addListener(closeListener);

        }
    };


    private Bootstrap b;
    private EventLoopGroup g;

    public WorkerReSender(ImNode n) {
        this.remoteNode = n;

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

   // 连接和重连
    public void doConnect() {

        // 服务器ip地址
        String host = remoteNode.getHost();
        // 服务器端口
        int port = remoteNode.getPort();

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
                                ch.pipeline().addLast(new ProtobufEncoder());
                            }
                        }
                );
                log.info(new Date() + "开始连接分布式节点", remoteNode.toString());

                ChannelFuture f = b.connect();
                f.addListener(connectedListener);


                // 阻塞
                // f.channel().closeFuture().sync();
            } else if (b.group() != null) {
                log.info(new Date() + "再一次开始连接分布式节点", remoteNode.toString());
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

    /**
     * 消息转发的方法
     * @param pkg  聊天消息
     */
    public void writeAndFlush(Object pkg) {
        if (connectFlag == false) {
            log.error("分布式节点未连接:", remoteNode.toString());
            return;
        }
        channel.writeAndFlush(pkg);
    }

}
