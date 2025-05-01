package org.example.authservice.Service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "api-gateway")
public interface UserServiceClient {
    @PostMapping("/api/users")
    ResponseEntity<String> createUser(@RequestBody UserRequest userRequest,
                                      @RequestHeader("Authorization") String token);
}
