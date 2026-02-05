package com.mortal.user.vo;

import java.util.List;
import lombok.Data;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String userType;
    private Integer status;
    private List<String> roles;
}
