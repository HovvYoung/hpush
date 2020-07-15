package com.hovvyoung.hpushClient.feignClient;

import com.hovvyoung.hpushCommon.constants.ServerConstants;
import com.hovvyoung.hpushCommon.entity.LoginBack;
import com.hovvyoung.hpushCommon.util.JsonUtil;
import feign.Feign;
import feign.codec.StringDecoder;

public class WebOperator {

    public static LoginBack login(String userName, String password) {
        UserAction action = Feign.builder()
//                .decoder(new GsonDecoder())
            .decoder(new StringDecoder())
            .target(UserAction.class, ServerConstants.WEB_URL);

        String s = action.loginAction(userName, password);

        LoginBack back = JsonUtil.jsonToPojo(s, LoginBack.class);
        return back;

    }

    public static LoginBack getImNode() {
        UserAction action = Feign.builder()
            .decoder(new StringDecoder())
            .target(UserAction.class, ServerConstants.WEB_URL);
        String s = action.getImNode();

        LoginBack back = JsonUtil.jsonToPojo(s, LoginBack.class);
        return back;
    }
}
