package com.hovvyoung.hpushClient.ClientSender;

import com.hovvyoung.hpushClient.clientBuilder.LoginMsgBuilder;
import com.hovvyoung.hpushCommon.bean.msg.ProtoMsg;
import com.hovvyoung.hpushCommon.util.Print;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("LoginSender")
public class LoginSender extends BaseSender {


    public void sendLoginMsg() {
        if (!isConnected()) {
            log.info("还没有建立连接!");
            return;
        }
        // Customized Print Class
        Print.tcfo("发送登录消息");
        ProtoMsg.Message message = LoginMsgBuilder.buildLoginMsg(getUser(), getSession());
        super.sendMsg(message);
    }


}
