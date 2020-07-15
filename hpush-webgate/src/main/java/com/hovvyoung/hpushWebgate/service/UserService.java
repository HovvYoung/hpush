/**
 * Created by 尼恩 at 疯狂创客圈
 */

package com.hovvyoung.hpushWebgate.service;


import com.hovvyoung.hpushWebgate.entity.UserInfo;
import com.hovvyoung.hpushWebgate.entity.UserPO;

public interface UserService {

    // 返回对象应该带有sessionId, 这里简便起见写UserPO
    UserPO login(UserInfo user);

    UserPO register();

    UserPO getById(String userid);

    int deleteById(String userid);


}
