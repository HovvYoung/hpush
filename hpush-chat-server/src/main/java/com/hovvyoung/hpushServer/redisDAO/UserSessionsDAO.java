package com.hovvyoung.hpushServer.redisDAO;

import com.hovvyoung.hpushServer.server.session.UserSessions;

/**
 * create by 尼恩 @ 疯狂创客圈
 **/
public interface UserSessionsDAO {
    void save(UserSessions s);

    UserSessions get(String sessionid);

    void cacheUser(String uid, String sessionId);

    void removeUserSession(String uid, String sessionId);
    UserSessions getAllSession(String userId);
}
