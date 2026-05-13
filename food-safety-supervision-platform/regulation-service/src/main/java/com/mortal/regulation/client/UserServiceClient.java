package com.mortal.regulation.client;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.client.dto.UserUpdateDTO;
import com.mortal.regulation.client.vo.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("user-service")
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    ApiResponse<UserVO> getUserById(@PathVariable("id") Long id);

    @PutMapping("/api/admin/users/{id}")
    ApiResponse<UserVO> updateUser(@PathVariable("id") Long id, @RequestBody UserUpdateDTO dto);

    @DeleteMapping("/api/admin/users/{id}")
    void deleteUser(@PathVariable("id") Long id);
}
