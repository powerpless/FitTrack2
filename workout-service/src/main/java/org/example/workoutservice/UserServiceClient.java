package org.example.workoutservice;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserServiceClient {
    @GetMapping("/api/users/username/{username}")
    UserDto getUserByUsername(@PathVariable String username, @RequestHeader("Authorization") String authHeader);
}