import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ZkApiTest {

    private static final String ZK_ADDRESS = "192.168.1.75:2181";
    @Test
    public void test() throws IOException, KeeperException, InterruptedException {
        //创建zk连接
        ZooKeeper zooKeeper = new ZooKeeper("192.168.1.75", 2000, new Watcher() {
            public void process(WatchedEvent watchedEvent) {
                System.out.println("触发了" + watchedEvent.getType() + "的事件");
            }
        });
        //2.创建父节点
        String path = zooKeeper.create("/itheima", "itheimaValue".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        System.out.println(path);
        //3.创建字节点

        //4.获取节点中的值

        //5.修改节点值

        //6.判断存在

        //7.删除节点
    }

    @Test
    public void curatorTest() {
        CuratorFramework client = ClientFactory.createSimple(ZK_ADDRESS);
        try {
            client.start();
            String data = "hello";
            byte[] payload = data.getBytes("UTF-8");
            String zkPath = "/test/CRUD/node-1";
            client.create()
                    .creatingParentContainersIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(zkPath, payload);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseableUtils.closeQuietly(client);
        }
    }
}
