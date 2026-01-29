package com.mortal.user.vo;

import java.util.List;
import lombok.Data;

@Data
public class AuthIntrospectVO {

    private boolean valid;
    private Long userId;
    private String username;
    private String userType;
    private Integer status;
    private Integer deleted;
    private List<String> roles;
}

