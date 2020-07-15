package com.hovvyoung.hpushServer.server.session;

import com.hovvyoung.hpushCommon.entity.ImNode;
import com.hovvyoung.hpushServer.redisDAO.UserSessionsDAO;
import io.netty.channel.ChannelHandlerContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Data
@Repository("SessionManger")
public class SessionManger {


    //    @Autowired
    UserSessionsDAO userSessionsDAO;


    private static SessionManger singleInstance = null;

    /*本地会话集合*/

    private ConcurrentHashMap<String, LocalSession> localSessionMap = new ConcurrentHashMap();


    /*远程会话集合*/

    private ConcurrentHashMap<String, RemoteSession> remoteSessionMap = new ConcurrentHashMap();

    /*本地用户集合*/
    private ConcurrentHashMap<String, UserSessions> sessionsLocalCache = new ConcurrentHashMap();

    private ConcurrentHashMap<String, List<String>> userSessionIdMap = new ConcurrentHashMap<>();


    public static SessionManger inst() {
        return singleInstance;
    }

    public static void setSingleInstance(SessionManger singleInstance) {
        SessionManger.singleInstance = singleInstance;
    }

    /**
     * 登录成功之后， 增加session对象
     */
    public void addLocalSession(LocalSession session) {
        String sessionId = session.getSessionId();
        localSessionMap.put(sessionId, session);


        String uid = session.getUser().getUserId();

//        //增加用户数
//        OnlineCounter.getInst().increment();
//        log.info("本地session增加：{},  在线总数:{} ",
//                JsonUtil.pojoToJson(session.getUser()),
//                OnlineCounter.getInst().getCurValue());
//        ImWorker.getInst().incBalance();


        //增加用户的session 信息到缓存
//        userSessionsDAO.cacheUser(uid, sessionId);

        addUserSessionIdMap(uid, sessionId);


//        /**
//         * 通知其他节点
//         */
//        notifyOtherImNode(session, Notification.SESSION_ON);

    }


    /**
     * 删除session
     */
    public void removeLocalSession(String sessionId) {
        if (!localSessionMap.containsKey(sessionId)) {
            return;
        }
        LocalSession session = localSessionMap.get(sessionId);
        String uid = session.getUser().getUserId();
        localSessionMap.remove(sessionId);

//
//        //减少用户数
//        OnlineCounter.getInst().decrement();
//        log.info("本地session减少：{},  在线总数:{} ",
//                JsonUtil.pojoToJson(session.getUser()),
//                OnlineCounter.getInst().getCurValue());
//        ImWorker.getInst().decrBalance();

        //分布式保存user和所有session
        userSessionsDAO.removeUserSession(uid, sessionId);

//
//        /**
//         * 通知其他节点
//         */
//        notifyOtherImNode(session, Notification.SESSION_OFF);

    }

    /**
     * 通知其他节点
     *
     * @param session session
     * @param type    类型
     */
//    private void notifyOtherImNode(LocalSession session, int type) {
//        UserDTO user = session.getUser();
//        RemoteSession remoteSession = RemoteSession.builder()
//                .sessionId(session.getSessionId())
//                .imNode(PeerManager.getInst().getLocalNode())
//                .userId(user.getUserId())
//                .valid(true)
//                .build();
//        Notification<RemoteSession> notification = new Notification<RemoteSession>(remoteSession);
//        notification.setType(type);
//        PeerManager.getInst().sendNotification(JsonUtil.pojoToJson(notification));
//    }


    /**
     * 根据用户id，获取session对象
     * 通过userId获取sessionId, 再查询到本地是否有对应的session
     */
    public List<ServerSession> getSessionsByUserId(String userId) {

        List<ServerSession> sessions = new LinkedList<>();
        UserSessions userSessions = loadFromCache(userId);

        if (null == userSessions) {
            return null;
        }

        Map<String, ImNode> allSession = userSessions.getMap();

        allSession.keySet().stream().forEach(key -> {

            //首先取得本地的session
            ServerSession session = localSessionMap.get(key);

            //没有命中，取得远程的session
            if (session == null) {
                session = remoteSessionMap.get(key);

            }
            sessions.add(session);
        });

        if (sessions.size() == 0) {
            List<String> sidList = getUserSessionIdMap(userId);
            if (sidList != null) {
                for (String sid : sidList) {
                    LocalSession session = localSessionMap.get(sid);
                    sessions.add(session);
                }
            }
        }

        return sessions;

    }

    /**
     * 从二级缓存加载
     *
     * @param userId 用户的id
     * @return 用户的集合
     */
    private UserSessions loadFromRedis(String userId) {


        UserSessions userSessions = userSessionsDAO.getAllSession(userId);

        if (null == userSessions) {
            return null;
        }
//        Map<String, ImNode> map = userSessions.getMap();
//        map.keySet().stream().forEach(key -> {
//            ImNode node = map.get(key);
//            //当前节点直接忽略
//            if (!node.equals(ImWorker.getInst().getLocalNodeInfo())) {
//
//                remoteSessionMap.put(key, new RemoteSession(key, userId, node));
//
//            }
//        });


        return userSessions;
    }


    /**
     * 从二级缓存加载
     *
     * @param userId 用户的id
     * @return 用户的集合
     */
    private UserSessions loadFromCache(String userId) {

        //本地缓存
        UserSessions userSessions = sessionsLocalCache.get(userId);

        if (null != userSessions
                && null != userSessions.getMap()
                && userSessions.getMap().keySet().size() > 0) {
            return userSessions;
        }


        UserSessions finalUserSessions = new UserSessions(userId);
        localSessionMap.values().stream().forEach(session -> {

            if (userId.equals(session.getUser().getUserId())) {
                finalUserSessions.addLocalSession(session);
            }
        });

        remoteSessionMap.values().stream().forEach(session -> {

            if (userId.equals(session.getUserId())) {
                finalUserSessions.addSession(session.getSessionId(), session.getImNode());
            }
        });

        sessionsLocalCache.put(userId, finalUserSessions);


        return finalUserSessions;
    }


    /**
     * 增加 远程的 session
     */
    public void addRemoteSession(RemoteSession remoteSession) {
        String sessionId = remoteSession.getSessionId();
        if (localSessionMap.containsKey(sessionId)) {
            log.error("通知有误，通知到了会话所在的节点");
            return;
        }

        remoteSessionMap.put(sessionId, remoteSession);
        //删除本地保存的 远程session
        String uid = remoteSession.getUserId();
        UserSessions sessions = sessionsLocalCache.get(uid);
        if (null == sessions) {
            sessions = new UserSessions(uid);
            sessionsLocalCache.put(uid, sessions);
        }

        sessions.addSession(sessionId, remoteSession.getImNode());
    }

    /**
     * 删除 远程的 session
     */
    public void removeRemoteSession(String sessionId) {
        if (localSessionMap.containsKey(sessionId)) {
            log.error("通知有误，通知到了会话所在的节点");
            return;
        }

        RemoteSession s = remoteSessionMap.get(sessionId);
        remoteSessionMap.remove(sessionId);

        //删除本地保存的 远程session
        String uid = s.getUserId();
        UserSessions sessions = sessionsLocalCache.get(uid);
        sessions.removeSession(sessionId);

    }


    //关闭连接
    public void closeSession(ChannelHandlerContext ctx) {

        LocalSession session =
                ctx.channel().attr(LocalSession.SESSION_KEY).get();

        if (null != session && session.isValid()) {
            session.close();
            this.removeLocalSession(session.getSessionId());
        }
    }

    public void addUserSessionIdMap(String uid, String sid) {
        List list = userSessionIdMap.get(uid);
        if (list == null) {
            list = new ArrayList<String>();
        }
        list.add(sid);
        userSessionIdMap.put(uid, list);
    }

    public List<String> getUserSessionIdMap(String uid) {
        return userSessionIdMap.get(uid);
    }

    public boolean removeUserSessionMap(String uid, String sid) {
        List list = userSessionIdMap.get(uid);
        if (list != null) {
            return list.remove(sid);
        }
        return true;
    }


}
