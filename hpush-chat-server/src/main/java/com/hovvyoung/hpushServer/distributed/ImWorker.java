package com.hovvyoung.hpushServer.distributed;

import com.hovvyoung.hpushCommon.constants.ServerConstants;
import com.hovvyoung.hpushCommon.entity.ImNode;
import com.hovvyoung.hpushCommon.util.JsonUtil;
import com.hovvyoung.hpushCommon.zookeeper.ZKclient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

/**
 * IM 节点的ZK协调客户端
 * create by 尼恩 @ 疯狂创客圈
 **/


public class ImWorker {

    //Zk curator 客户端
    private CuratorFramework client = null;

    //保存当前Znode节点的路径，创建后返回
    private String pathRegistered = null;      //zk中路径

    private ImNode localNode = null;        // 对应netty服务器的信息

    private static ImWorker singleInstance = null;

    //取得单例
    public static ImWorker getInst() {

        if (null == singleInstance) {

            singleInstance = new ImWorker();
            singleInstance.client =
                    ZKclient.instance.getClient();
            singleInstance.localNode = new ImNode();
        }
        return singleInstance;
    }

    private ImWorker() {

    }

    // 在zookeeper中创建临时节点
    public void init() {

        createParentIfNeeded(ServerConstants.MANAGE_PATH);

        // 创建一个 ZNode 节点
        // 节点的 payload 为当前worker 实例, 即把当前类转成bytes写进zk的一个节点

        try {
            byte[] payload = JsonUtil.object2JsonBytes(localNode);

            pathRegistered = client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath(ServerConstants.PATH_PREFIX, payload);

            //为node 设置id
            localNode.setId(getId());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setLocalNode(String ip, int port) {
        localNode.setHost(ip);
        localNode.setPort(port);
    }

    /**
     * 取得IM 节点编号
     *
     * @return 编号
     */
    public long getId() {

        return getIdByPath(pathRegistered);

    }

  /**
     * 取得IM 节点编号
     *
     * @return 编号
   * @param path  路径
     */
    public long getIdByPath(String path) {
        String sid = null;
        if (null == path) {
            throw new RuntimeException("节点路径有误");
        }
        int index = path.lastIndexOf(ServerConstants.PATH_PREFIX);
        if (index >= 0) {
            index += ServerConstants.PATH_PREFIX.length();
            sid = index <= path.length() ? path.substring(index) : null;
        }

        if (null == sid) {
            throw new RuntimeException("节点ID获取失败");
        }

        return Long.parseLong(sid);

    }


    /**
     * 增加负载，表示有用户登录成功
     *
     * @return 成功状态
     */
    public boolean incBalance() {
        if (null == localNode) {
            throw new RuntimeException("还没有设置Node 节点");
        }
        // 增加负载：增加负载，并写回zookeeper
        while (true) {
            try {
                localNode.incrementBalance();
                byte[] payload = JsonUtil.object2JsonBytes(localNode);
                client.setData().forPath(pathRegistered, payload);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

    }

    /**
     * 减少负载，表示有用户下线，写回zookeeper
     *
     * @return 成功状态
     */
    public boolean decrBalance() {
        if (null == localNode) {
            throw new RuntimeException("还没有设置Node 节点");
        }
        while (true) {
            try {

                localNode.decrementBalance();

                byte[] payload = JsonUtil.object2JsonBytes(localNode);
                client.setData().forPath(pathRegistered, payload);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

    }

    /**
     * 创建父节点
     *
     * @param managePath 父节点路径
     */
    private void createParentIfNeeded(String managePath) {

        try {
            Stat stat = client.checkExists().forPath(managePath);
            if (null == stat) {
                client.create()
                        .creatingParentsIfNeeded()
                        .withProtection()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(managePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 返回本地的节点信息
     *
     * @return 本地的节点信息
     */
    public ImNode getLocalNodeInfo() {
        return localNode;
    }

}