import com.hovvyoung.hpushCommon.zookeeper.ClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.junit.jupiter.api.Test;

import java.util.List;

@Slf4j
public class ZkTest {
    private static final String ZK_ADDRESS = "192.168.1.75:2181";

    @Test
    public void createNode() {
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

    @Test
    public void readNode() {
        CuratorFramework client = ClientFactory.createSimple(ZK_ADDRESS);
        try {
            client.start();
            String zkPath = "/test/CRUD/node-1";
            Stat stat = client.checkExists().forPath(zkPath);
            if (null != stat) {
                byte[] payload = client.getData().forPath(zkPath);
                String data = new String(payload, "UTF-8");
                log.info("######### read data:" + data);
                String parentPath = "/test";
                //获取子节点列表
                List<String> children = client.getChildren().forPath(parentPath);
                for (String child : children) {
                    log.info("child:" + child);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseableUtils.closeQuietly(client);
        }
    }

    @Test
    public void updateNode() {
        CuratorFramework client = ClientFactory.createSimple(ZK_ADDRESS);
        try {
            client.start();
            String data = "hello world";
            byte[] payload = data.getBytes("UTF-8");
            String zkPath = "/test/CRUD/node-1";
            client.setData().forPath(zkPath, payload);
            String parentPath = "/test/CRUD";
            //获取子节点列表
            List<String> children = client.getChildren().forPath(parentPath);
            for (String child : children) {
                log.info("child:" + child);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseableUtils.closeQuietly(client);
        }
    }

    @Test
    public void deleteNode() {
        CuratorFramework client = ClientFactory.createSimple(ZK_ADDRESS);
        try {
            client.start();
            String zkPath = "/test/CRUD/node-1";
            client.delete().forPath(zkPath);
            String parentPath = "/test";
            //获取子节点列表
            List<String> children = client.getChildren().forPath(parentPath);
            for (String child : children) {
                log.info("child:" + child);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseableUtils.closeQuietly(client);
        }
    }
}
