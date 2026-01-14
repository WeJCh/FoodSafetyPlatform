package com.mortal.user.vo;

import lombok.Data;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String userType;
    private Integer status;
}
