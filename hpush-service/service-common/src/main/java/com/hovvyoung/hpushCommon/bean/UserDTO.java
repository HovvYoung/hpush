package com.hovvyoung.hpushCommon.bean;

import com.hovvyoung.hpushCommon.bean.msg.ProtoMsg;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class UserDTO {

    String userId;
    String userName;
    String devId;   // deviceId
    String pwd;   //password
    String nickName = "nickName";
    PLATTYPE platform = PLATTYPE.WINDOWS;    // One user can login in using different device with dif platforms

    // windows,mac,android, ios, web , other
    public enum PLATTYPE {
        WINDOWS, MAC, ANDROID, IOS, WEB, OTHER;
    }

    private String sessionId;

    public void setPlatform(int platform) {
        PLATTYPE[] values = PLATTYPE.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].ordinal() == platform) {
                this.platform = values[i];
            }
        }

    }


    @Override
    public String toString() {
        return "User{" +
                "uid='" + userId + '\'' +
                ", devId='" + devId + '\'' +
                ", pwd='" + pwd + '\'' +
                ", nickName='" + nickName + '\'' +
                ", platform=" + platform +
                '}';
    }

    public static UserDTO fromMsg(ProtoMsg.LoginRequest info) {
        UserDTO user = new UserDTO();
        user.userId = info.getUid();
        user.devId = info.getDeviceId();
        user.pwd = info.getPwd();
        user.setPlatform(info.getPlatform());
        log.info("登录中: {}", user.toString());
        return user;

    }

}
