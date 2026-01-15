package com.mortal.regulation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("user-service")
public interface UserServiceClient {

    @DeleteMapping("/api/admin/users/{id}")
    void deleteUser(@PathVariable("id") Long id);
}
