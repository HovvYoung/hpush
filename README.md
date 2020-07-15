`` 本项目是基于<<Netty_Redis_Zookeeper并发实战>>的学习总结``

### Protobuf
Protobuf是Google提出的一种数据交换格式，更适合于高性能、快速响应的数据传输应用场景。  
JSON、XML是文本格式，数据具有可读性；而Protobuf是二进制数据格式，只有反序列化之后才能得到真正可读的数据，体积较小，更加适合网络传输。  
在需要大数据量传输的应用场景中，Protobuf可以明显地减少传输的数据量和提升网络IO的速度。对于开发高性能的通信服务器来说，Protobuf传输协议是最高性能的传输协议之一。

### 登录流程
1. client发送登录数据包
2. server进行用户信息验证
3. server创建session会话
4. server返回登录结果信息给client，包括成功标志，Session ID等

流程  
console控制台命令 -> LoginSender发送器组装Protobuf数据包 -> client channel -> server channel -> loginRequestHandler -> 登录请求业务服务器LoginMsgProcesser处理异步的业务逻辑
 -> Server Channel -> Client Channel -> LoginResponseHandler处理登录相应，保存会话sessionid等
 

### hpush-chat-client
##### clientHandler
`ChatMsgHandler` `ExceptionHandler` `HeartBeatClientHandler` `LoginResponseHandler`

##### CommandController.java  连接服务器
```
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
        // 从控制台获取登录信息
        UserDTO user = new UserDTO();
        user.setUserId(command.getUserName());
        user.setPwd(command.getPassword());
        user.setDevId("0001");

        log.info("step1：开始登录WEB GATE");
        // 由网关向Zookeeper获取一个可用的Server服务器节点
        LoginBack webBack = WebOperator.getImNode();
        ImNode node = webBack.getImNode();
        log.info("step1 WEB GATE 返回的node节点是：{}", JsonUtil.pojoToJson(node));

        log.info("step2：开始连接Netty 服务节点");

        // set a connect state listener. This will be added to the channelFuture of connection.
        nettyClient.setConnectedListener(connectedListener);

        nettyClient.setHost(node.getHost());
        nettyClient.setPort(node.getPort());
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
```

``一般Netty用write(pkg)/writeAndFlush(pkg)发送数据包。writeAndFlush()是立即返回一个ChannelFuture异步任务实例，但是真正的TCP写操作还未执行。
因为同一条通道的所有出/入站处理都是串行的(放在一条MPSC队列)，都会在它所绑定的EventLoop线程上执行``

### hpush-chat-server 

##### serverHandler
`ChatRedirectHandler` `HeartBeatServerHandler` `ImNodeExceptionHandler` `ImNodeHeartBeatClientHandler`
`LoginRequestHandler` `RemoteNotificationHandler` `ServerExceptionHandler`  
  
**不同于客户端在Netty的Handler入站处理器模块中统一完成业务的处理逻辑，Server处理登录分成两个模块，一个模块是Handler业务处理器，另一个是Processor以异步方式完成请求的业务逻辑处理。**  
专门开辟一个独立的线程池，负责一个独立的异步任务处理队列，对于耗时的业务操作封装成异步任务，并放入异步任务队列去处理  
  
  
ServerSession: 如果校验成功，设置相应的会话状态isLogin=true,；然后将Session加入到Server的SessionMap中，可以接收其他用户发送的聊天信息。  
每个ServerSession有唯一标识sessionId, 不是userid(多设备登录)。

```
    //反向导航
    public static LocalSession getSession(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        return channel.attr(LocalSession.SESSION_KEY).get();
    }
```


##### 长连接 心跳
**ChannelOption.SO_KEEPALIVE**  
    Channeloption.SO_KEEPALIVE参数对应于套接字选项中的SO_KEEPALIVE，该参数用于设置TCP连接，当设置该选项以后，连接会测试链接的状态，这个选项用于可能长时间没有数据交流的
　　连接。当设置该选项以后，如果在两小时内没有数据的通信时，TCP会自动发送一个活动探测数据报文。

在HeartBeatServerHandler中，若长时间(150s)未收到客户端的心跳，关闭对应channel。
``` 
    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        log.info(READ_IDLE_GAP + "秒内未读到数据，关闭连接",ctx.channel().attr(ServerConstants.CHANNEL_NAME).get());
        SessionManger.inst().closeSession(ctx);
    }
```

### Zookeeper
分布式命名：使用ZooKeeper持久顺序节点的顺序特性来维护节点的NodeId编号
分布式ID生成
* java uuid
* 分布式缓存redis生成id: 利用redis的原子操作incr和incrby，生成全局唯一的ID
* Twitter的snowFlake算法

JNID(Java命名和目录接口)  
Watcher标准的事件处理器  
* KeeperState 通知状态
* EventType 事件类型

Zookeeper发送WatcherEvent给注册过的客户端，然后Curator封装成WatchedEvent传递给监听器的process(WatchedEvent e)回调方法
WatchedEvent
* 通知状态 keeperState
* 事件类型 EventType
* 节点路径 path

### 服务集群
* Netty服务集群  
    主要负责维持和客户端的TCP连接，完成消息的发送和转发
* Zookeeper集群
负责Netty Server集群的管理，包括注册、路由、负载均衡。集群IP注册和节点ID分配。

* Redis集群
负责用户、用户绑定关系、用户群组关系、用户远程会话等等数据的缓存。缓存其他的配置数据或者临时数据，加快读取速度。

* MySql集群
保存用户、群组、离线消息等。  
非结构化离线信息也可存在mongodb。

* RocketMQ消息队列集群
主要是将优先级不高的操作，从高并发模式转成低并发模式。例如可以将离线信息发向消息队列，然后通过低并发的异步任务保存到数据库。



##### Worker


  ImWorker类  
  IM 节点的ZK协调客户端  
  命名思路：所有工作节点都在ZK同一个父节点下创建顺序节点，然后从返回的临时路径上取得自己的那个后缀编号。  
```java
public class ImWorker {

    //Zk curator 客户端
    private CuratorFramework client = null;

    //保存当前Znode节点的路径，创建后返回
    private String pathRegistered = null;      //zk中路径

    private ImNode localNode = null;        // 对应netty服务器的信息

    private static ImWorker singleInstance = null;
}

```  
  
  节点的 payload 为当前worker 实例, 即把当前类转成bytes写进zk的一个节点。worker实例有个ImNode属性。
```java
public class ImNode implements Comparable<ImNode>, Serializable {

    private static final long serialVersionUID = -499010884211304846L;


    //worker 的Id,zookeeper负责生成
    private long id;

    //Netty 服务 的连接数
    private Integer balance = 0;

    //Netty 服务 IP
    private String host;

    //Netty 服务 端口
    private Integer port;

    public ImNode() {
    }
    // ...
}
```
  增加负载，表示有用户登录成功。
  
  ImLoadBalance类 负载均衡  
  获取所有netty临时节点，调用getData方法取得每个节点的二进制负载，最后将负载信息转成ImNode对象，然后排序找最小。
  
  WorkerRouter  
  为每一个worker节点增加一个workerRouter,订阅到集群中所有在线的netty服务器，并与他们建立长连接用于转发消息。  
  用Curator的TreeCache缓存订阅了节点的NODE_ADDED节点添加消息。调用processNodeAdded(data)方法本地保存节点的POJO信息，
  并且建立一个消息中专的Netty客户连接 ()。
 ```
reSender = new WorkerReSender(imNode)
reSender.doConnect(); //启动客户端
workerMap.put(id, reSender);

public class WorkerReSender {
    //连接远程节点的Netty 通道
    private Channel channel;

    //连接远程节点的POJO信息
    private ImNode remoteNode;
    
    //连接标记
    private boolean connectFlag = false;
    ...
}
```

WorkerReSender转发器  
本质就是个netty client 连接其他netty 服务器




