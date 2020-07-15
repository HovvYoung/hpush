package com.hovvyoung.hpushWebgate.loadbalance;

import com.hovvyoung.hpushCommon.constants.ServerConstants;
import com.hovvyoung.hpushCommon.entity.ImNode;
import com.hovvyoung.hpushCommon.util.JsonUtil;
import com.hovvyoung.hpushCommon.zookeeper.ZKclient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * create by 尼恩 @ 疯狂创客圈
 **/
@Data
@Slf4j
@Service
public class ImLoadBalance {

    //Zk客户端
    private CuratorFramework client = null;
    private String managerPath;

    public ImLoadBalance() {
        this.client = ZKclient.instance.getClient();
//        managerPath=ServerConstants.MANAGE_PATH+"/";
        managerPath= ServerConstants.MANAGE_PATH;
    }

    /**
     * 获取负载最小的IM节点
     *
     * @return
     */
    public ImNode getBestWorker() {
        List<ImNode> workers = getWorkers();

        log.info("全部节点如下：");
        workers.stream().forEach(node -> {
            log.info("节点信息：{}", JsonUtil.pojoToJson(node));
        });
        ImNode best = balance(workers);

        return best;
    }

    /**
     * 按照负载排序
     *
     * @param items 所有的节点
     * @return 负载最小的IM节点
     */
    protected ImNode balance(List<ImNode> items) {
        if (items.size() > 0) {
            // 根据balance值由小到大排序
            Collections.sort(items);

            // 返回balance值最小的那个
            ImNode node = items.get(0);

            log.info("最佳的节点为：{}", JsonUtil.pojoToJson(node));
            return node;
        } else {
            return null;
        }
    }


    /**
     * 从zookeeper中拿到所有IM节点
     */
    protected List<ImNode> getWorkers() {

        List<ImNode> workers = new ArrayList<ImNode>();

        List<String> children = null;
        try {
            children = client.getChildren().forPath(managerPath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        for (String child : children) {
            log.info("child:", child);
            byte[] payload = null;
            try {
                payload = client.getData().forPath(managerPath+"/"+child);

            } catch (Exception e) {
                e.printStackTrace();
            }
            if (null == payload) {
                continue;
            }
            ImNode worker = JsonUtil.jsonBytes2Object(payload, ImNode.class);
            workers.add(worker);
        }
        return workers;

    }
    /**
     * 从zookeeper中删除所有IM节点
     */
    public void removeWorkers() {


        try {
          client.delete().deletingChildrenIfNeeded().forPath(managerPath);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}