package com.hovvyoung.hpushServer.server.session;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalUserSessions {
    private String userId;
    private Map<String, List<LocalSession>> map = new HashMap<>();

    public LocalUserSessions(String userId) {
        this.userId = userId;
    }

    public void removeSession(String sessionId) {
        map.remove(sessionId);
    }

    public void getSessions(String uid) {

    }

    public void addLocalSession(LocalSession session) {

        //TODO: map.put(session.getSessionId(), ImWorker.getInst().getLocalNodeInfo());
    }
}
