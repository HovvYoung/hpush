package com.hovvyoung.hpushServer.server.session;

import com.hovvyoung.hpushCommon.entity.ImNode;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * create by 尼恩 @ 疯狂创客圈
 **/
@Data
public class UserSessions {

    private String userId;
    private Map<String, ImNode> map = new LinkedHashMap<>(10);

    public UserSessions(String userId) {
        this.userId = userId;
    }


    public void addSession(String sessionId, ImNode node) {

        map.put(sessionId, node);
    }

    public void removeSession(String sessionId) {
        map.remove(sessionId);
    }


    public void addLocalSession(LocalSession session) {

       //TODO: map.put(session.getSessionId(), ImWorker.getInst().getLocalNodeInfo());
    }
}
