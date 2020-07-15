/**
 * Created by 尼恩 at 疯狂创客圈
 */

package com.hovvyoung.hpushClient.protoBuilder;

import com.hovvyoung.hpushClient.client.ClientSession;
import com.hovvyoung.hpushCommon.bean.UserDTO;
import com.hovvyoung.hpushCommon.bean.msg.ProtoMsg;

/**
 * 心跳消息Builder
 */
public class HeartBeatMsgBuilder extends BaseBuilder {
    private final UserDTO user;

    public HeartBeatMsgBuilder(UserDTO user, ClientSession session) {
        super(ProtoMsg.HeadType.HEART_BEAT, session);
        this.user = user;
    }

    public ProtoMsg.Message buildMsg() {
        ProtoMsg.Message message = buildCommon(-1);
        ProtoMsg.MessageHeartBeat.Builder lb =
                ProtoMsg.MessageHeartBeat.newBuilder()
                        .setSeq(0)
                        .setJson("{\"from\":\"client\"}")
                        .setUid(user.getUserId());
        return message.toBuilder().setHeartBeat(lb).build();
    }


}


