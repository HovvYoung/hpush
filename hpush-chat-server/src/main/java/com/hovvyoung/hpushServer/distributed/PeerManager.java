package com.hovvyoung.hpushServer.distributed;

import com.hovvyoung.hpushServer.protoBuilder.NotificationMsgBuilder;
import com.hovvyoung.hpushCommon.bean.msg.ProtoMsg;
import com.hovvyoung.hpushCommon.constants.ServerConstants;
import com.hovvyoung.hpushCommon.entity.ImNode;
import com.hovvyoung.hpushCommon.util.JsonUtil;
import com.hovvyoung.hpushCommon.util.ObjectUtil;
import com.hovvyoung.hpushCommon.zookeeper.ZKclient;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;


import java.util.concurrent.ConcurrentHashMap;

/**
 * 这个类用来监听zk节点的变化
 **/
@Slf4j
public class PeerManager {
    //Zk客户端
    private CuratorFramework client = null;

    private String pathRegistered = null;
    private ImNode node = null;


    private static PeerManager singleInstance = null;
    private static final String path = ServerConstants.MANAGE_PATH;

    private ConcurrentHashMap<Long, PeerSender> peerMap = new ConcurrentHashMap<>();


    public static PeerManager getInst() {
        if (null == singleInstance) {
            singleInstance = new PeerManager();
            singleInstance.client = ZKclient.instance.getClient();
        }
        return singleInstance;
    }

    private PeerManager() {

    }


    /**
     * 初始化节点管理
     */
    public void init() {
        try {

            //订阅节点的增加和删除事件

            PathChildrenCache childrenCache = new PathChildrenCache(client, path, true);
            PathChildrenCacheListener childrenCacheListener = new PathChildrenCacheListener() {

                @Override
                public void childEvent(CuratorFramework client,
                                       PathChildrenCacheEvent event) throws Exception {
                    log.info("开始监听其他的ImWorker子节点:-----");
                    ChildData data = event.getData();
                    switch (event.getType()) {
                        case CHILD_ADDED:
                            log.info("CHILD_ADDED : " + data.getPath() + "  数据:" + data.getData());
                            processNodeAdded(data);
                            break;
                        case CHILD_REMOVED:
                            log.info("CHILD_REMOVED : " + data.getPath() + "  数据:" + data.getData());
                            processNodeRemoved(data);
                            break;
                        case CHILD_UPDATED:
                            log.info("CHILD_UPDATED : " + data.getPath() + "  数据:" + new String(data.getData()));
                            break;
                        default:
                            log.debug("[PathChildrenCache]节点数据为空, path={}", data == null ? "null" : data.getPath());
                            break;
                    }

                }

            };

            childrenCache.getListenable().addListener(childrenCacheListener);
            System.out.println("Register zk watcher successfully!");
            childrenCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processNodeRemoved(ChildData data) {

        byte[] payload = data.getData();
        ImNode n = ObjectUtil.JsonBytes2Object(payload, ImNode.class);

        long id = ImWorker.getInst().getIdByPath(data.getPath());
        n.setId(id);
        log.info("[TreeCache]节点删除, path={}, data={}",
                data.getPath(), JsonUtil.pojoToJson(n));
        PeerSender peerSender = peerMap.get(n.getId());

        if (null != peerSender) {
            peerSender.stopConnecting();
            peerMap.remove(n.getId());
        }
    }

    private void processNodeAdded(ChildData data) {
        byte[] payload = data.getData();
        ImNode n = ObjectUtil.JsonBytes2Object(payload, ImNode.class);

        long id = ImWorker.getInst().getIdByPath(data.getPath());
        n.setId(id);

        log.info("[TreeCache]节点更新端口, path={}, data={}",
                data.getPath(), JsonUtil.pojoToJson(n));

        if(n.equals(getLocalNode()))
        {
            log.info("[TreeCache]本地节点, path={}, data={}",
                    data.getPath(), JsonUtil.pojoToJson(n));
            return;
        }
        PeerSender peerSender = peerMap.get(n.getId());
        if (null != peerSender && peerSender.getNode().equals(n)) {

            log.info("[TreeCache]节点重复增加, path={}, data={}",
                    data.getPath(), JsonUtil.pojoToJson(n));
            return;
        }
        if (null != peerSender) {
            //关闭老的连接
            peerSender.stopConnecting();
        }
        peerSender = new PeerSender(n);
        peerSender.doConnect();

        peerMap.put(n.getId(), peerSender);
    }


    public PeerSender getPeerSender(long id) {
        PeerSender peerSender = peerMap.get(id);
        if (null != peerSender) {
            return peerSender;
        }
        return null;
    }


    public void sendNotification(String json) {
        peerMap.keySet().stream().forEach(
                key -> {
                    if (!key.equals(getLocalNode().getId())) {
                        PeerSender peerSender = peerMap.get(key);
                        ProtoMsg.Message pkg = NotificationMsgBuilder.buildNotification(json);
                        peerSender.writeAndFlush(pkg);
                    }
                }
        );

    }


    public ImNode getLocalNode() {
        return ImWorker.getInst().getLocalNodeInfo();
    }

    public void remove(ImNode remoteNode) {
        peerMap.remove(remoteNode.getId());
        log.info("[TreeCache]移除远程节点信息,  node={}", JsonUtil.pojoToJson(remoteNode));
    }
}
