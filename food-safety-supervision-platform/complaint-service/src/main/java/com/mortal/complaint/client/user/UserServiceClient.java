package com.mortal.complaint.client.user;

import com.mortal.complaint.client.user.vo.UserVO;
import com.mortal.platform.common.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("user-service")
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    ApiResponse<UserVO> getUserById(@PathVariable("id") Long id);
}
