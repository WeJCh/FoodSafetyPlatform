package com.mortal.user.vo;

import java.time.LocalDateTime;
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<String> roles;
}
