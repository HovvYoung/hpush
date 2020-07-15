package com.hovvyoung.hpushCommon.entity;

import com.hovvyoung.hpushCommon.bean.UserDTO;
import lombok.Data;

/**
 * create by 尼恩 @ 疯狂创客圈
 **/
@Data
public class LoginBack {

    ImNode imNode;

    private String pwd;

    private UserDTO userDTO;

}
