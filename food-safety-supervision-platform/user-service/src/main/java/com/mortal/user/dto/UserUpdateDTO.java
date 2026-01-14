package com.mortal.user.dto;

import lombok.Data;

@Data
public class UserUpdateDTO {

    private Long id;
    private String realName;
    private String phone;
    private Integer status;
}
