package com.hovvyoung.hpushServer.server.session;

import com.hovvyoung.hpushCommon.entity.ImNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * create by 尼恩 @ 疯狂创客圈
 **/
@Data
@Builder
@AllArgsConstructor
public class RemoteSession implements ServerSession, Serializable {
    private static final long serialVersionUID = -400010884211394846L;

    private String userId;
    private String sessionId;
    private ImNode imNode;
    private boolean valid= true;

    public RemoteSession() {
        userId = "";
        sessionId = "";
        imNode = new ImNode("unKnown", 0);
    }

    public RemoteSession(
            String sessionId, String userId, ImNode imNode) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.imNode = imNode;
    }

    @Override
    public void writeAndFlush(Object pkg) {
        // TODO: WIRTE
//        long nodeId = imNode.getId();
//        PeerSender sender =
//                PeerManager.getInst().getPeerSender(nodeId);
//
//        sender.writeAndFlush(pkg);
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

}
