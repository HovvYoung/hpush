package com.hovvyoung.hpushServer.redisDAO.impl;

/**
 * create by 尼恩 @ 疯狂创客圈
 **/

import com.hovvyoung.hpushServer.server.session.UserSessions;
import com.hovvyoung.hpushCommon.util.JsonUtil;
import com.hovvyoung.hpushServer.redisDAO.UserSessionsDAO;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * create by 尼恩 @ 疯狂创客圈
 **/
@Repository("UserSessionsRedisImpl")
public class UserSessionsRedisImpl implements UserSessionsDAO {

    public static final String REDIS_PREFIX = "UserSessions:uid:";
    @Resource
    protected RedisTemplate<Serializable, Serializable> redisTemplate;
    private static final long CASHE_LONG = 60 * 4;//4小时


    @Override
    public void save(final UserSessions uss) {
        String key = REDIS_PREFIX + uss.getUserId();
        String value = JsonUtil.pojoToJson(uss);
        redisTemplate.opsForValue().set(key, value, CASHE_LONG, TimeUnit.MINUTES);
    }


    @Override
    public UserSessions get(final String usID) {
        String key = REDIS_PREFIX + usID;
        String value = (String) redisTemplate.opsForValue().get(key);

        if (!StringUtils.isEmpty(value)) {
            return JsonUtil.jsonToPojo(value, UserSessions.class);
        }
        return null;
    }

    @Override
    public void cacheUser(String uid, String sessionId) {
//        UserSessions us = get(uid);
//        if (null == us) {
//            us = new UserSessions(uid);
//        }
//        us.addSession(sessionId, ImWorker.getInst().getLocalNodeInfo());
//        save(us);
    }

    @Override
    public void removeUserSession(String uid, String sessionId) {
        UserSessions us = get(uid);
        if (null == us) {
            us = new UserSessions(uid);
        }
        us.removeSession(sessionId);
        save(us);
    }

    @Override
    public UserSessions getAllSession(String userId) {
        UserSessions us = get(userId);
        if (null != us) {
            return us;
        }
        return null;
    }
}