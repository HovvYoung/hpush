package com.hovvyoung.hpushClient.client;

import com.hovvyoung.hpushCommon.entity.ImNode;
import com.hovvyoung.hpushCommon.entity.LoginBack;
import com.hovvyoung.hpushCommon.util.JsonUtil;
import com.hovvyoung.hpushClient.ClientSender.ChatSender;
import com.hovvyoung.hpushClient.ClientSender.LoginSender;
import com.hovvyoung.hpushClient.clientCommand.ChatConsoleCommand;
import com.hovvyoung.hpushClient.clientCommand.LoginConsoleCommand;
import com.hovvyoung.hpushClient.clientCommand.LogoutConsoleCommand;
import com.hovvyoung.hpushCommon.bean.UserDTO;
import com.hovvyoung.hpushCommon.concurrent.FutureTaskScheduler;
import com.hovvyoung.hpushClient.clientCommand.BaseCommand;
import com.hovvyoung.hpushClient.feignClient.WebOperator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
@Service("CommandController")
public class CommandController {

    //聊天命令收集类
    @Autowired
    ChatConsoleCommand chatConsoleCommand;

    //登录命令收集类
    @Autowired
    LoginConsoleCommand loginConsoleCommand;

    //登出命令收集类
    @Autowired
    LogoutConsoleCommand logoutConsoleCommand;

    private Map<String, BaseCommand> commandMap;

    private String menuString;

    //会话类
    private ClientSession session;

    @Autowired
    private NettyClient nettyClient;

    private Channel channel;

    @Autowired
    private ChatSender chatSender;

    @Autowired
    private LoginSender loginSender;

    private boolean connectFlag = false;
    private UserDTO user;

    private Scanner scanner;

    GenericFutureListener<ChannelFuture> closeListener = (ChannelFuture f) ->
    {
        log.info(new Date() + ": 连接已经断开……");
        channel = f.channel();

        // 创建会话
        ClientSession session = channel.attr(ClientSession.SESSION_KEY).get();
        session.close();

        //唤醒用户线程
        notifyCommandThread();
    };


    GenericFutureListener<ChannelFuture> connectedListener = (ChannelFuture f) ->
    {
        final EventLoop eventLoop = f.channel().eventLoop();
        if (!f.isSuccess()) {
            log.info("连接失败!在10s之后准备尝试重连!");
            eventLoop.schedule(
                    () -> nettyClient.doConnect(),
                    10,
                    TimeUnit.SECONDS);

            connectFlag = false;
        } else {
            connectFlag = true;

            log.info("服务器 连接成功!");
            channel = f.channel();

            // 创建会话
            session = new ClientSession(channel);
            session.setConnected(true);
            channel.closeFuture().addListener(closeListener);

            //唤醒用户线程
            notifyCommandThread();
        }

    };



    public void initCommandMap() {
        commandMap = new HashMap<>();
        commandMap.put(chatConsoleCommand.getKey(), chatConsoleCommand);
        commandMap.put(loginConsoleCommand.getKey(), loginConsoleCommand);
        commandMap.put(logoutConsoleCommand.getKey(), logoutConsoleCommand);

        Set<Map.Entry<String, BaseCommand>> entrys = commandMap.entrySet();
        Iterator<Map.Entry<String, BaseCommand>> iterator = entrys.iterator();
    }

    public void startConnectServer() {

        FutureTaskScheduler.add(() ->
        {
            nettyClient.setConnectedListener(connectedListener);
            nettyClient.doConnect();
        });
    }

    public synchronized void notifyCommandThread() {
        //唤醒，命令收集程
        this.notify();

    }

    public synchronized void waitCommandThread() {
        //休眠，命令收集线程
        try {
            this.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    /**
     * 开始连接服务器
     */
    private void userLoginAndConnectToServer() {

        //登录
        if (isConnectFlag()) {
            log.info("已经登录成功，不需要重复登录");
            return;
        }
        LoginConsoleCommand command = (LoginConsoleCommand) commandMap.get(LoginConsoleCommand.KEY);
        command.exec(scanner);

        UserDTO user = new UserDTO();
        user.setUserId(command.getUserName());
        user.setPwd(command.getPassword());
        user.setDevId("0001");

//        TODO: WEB GATE
        log.info("step1：开始登录WEB GATE");
//        LoginBack webBack = WebOperator.login(command.getUserName(), command.getPassword());
        LoginBack webBack = WebOperator.getImNode();
        ImNode node = webBack.getImNode();
        log.info("step1 WEB GATE 返回的node节点是：{}", JsonUtil.pojoToJson(node));

        log.info("step2：开始连接Netty 服务节点");

        // set a connect state listener. This will be added to the channelFuture of connection.
        nettyClient.setConnectedListener(connectedListener);

//        nettyClient.setHost(node.getHost());
//        nettyClient.setPort(node.getPort());

        nettyClient.setHost("192.168.116.1");
        nettyClient.setPort(7000);
        nettyClient.doConnect();
        // connection process is asynchronous, wait here for connectedListener callback.
        waitCommandThread();

        log.info("step2：Netty 服务节点连接成功");
        log.info("step3：开始登录Netty 服务节点");

        this.user = user;
        // save the user to session. Session is created in connectedListener.
        session.setUser(user);

        loginSender.setUser(user);
        loginSender.setSession(session);
        loginSender.sendLoginMsg();  // 加入队列异步发送，但是发送的过程是阻塞的
        waitCommandThread();

        connectFlag =true;  //登录成功
    }

    public void startCommandThread() throws InterruptedException {
        scanner = new Scanner(System.in);
        Thread.currentThread().setName("命令线程");

        while (true) {
            //建立连接
            while (connectFlag == false) {
                //输入用户名，然后登录
                userLoginAndConnectToServer();
            }
            //处理命令
            while (null != session) {
                ChatConsoleCommand command = (ChatConsoleCommand) commandMap.get(ChatConsoleCommand.KEY);
                command.exec(scanner);
                singleChat(command);
            }
        }
    }

    //发送单聊消息
    private void singleChat(ChatConsoleCommand c) {
        //登录
        if (!isLogin()) {
            log.info("还没有登录，请先登录");
            return;
        }
        chatSender.setSession(session);
        chatSender.setUser(user);
        //传入 通过chatConsoleCommand输入的目标用户 和 消息
        chatSender.sendChatMsg(c.getToUserId(), c.getMessage());
    }

    public void logout(BaseCommand command) {
        //登出
        if (!isLogin()) {
            log.info("还没有登录，请先登录");
            return;
        }
        //todo 登出
        System.out.println("log out...");
        // 清空用户信息和登录状态, 回到登录界面, 重新添加登录handler
        return;
    }


    public boolean isLogin() {
        if (null == session) {
            log.info("session is null");
            return false;
        }
        return session.isLogin();
    }

}
