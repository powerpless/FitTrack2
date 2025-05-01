package org.example.userservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthServiceClient authServiceClient;

    private final JwtUtil jwtUtil;

    @Autowired
    public UserController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody UserRequest userRequest, @RequestHeader("Authorization") String authHeader) {
        System.out.println("[DEBUG] Creating user with username: " + userRequest.getUsername());

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing token");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        if (userRepository.existsByUsername(userRequest.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username already exists");
        }
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email already exists");
        }

        User user = new User();
        user.setUsername(userRequest.getUsername());
        user.setEmail(userRequest.getEmail());
        user.setFirstName(userRequest.getFirstName());
        user.setLastName(userRequest.getLastName());

        userRepository.save(user);
        return ResponseEntity.ok("User created successfully");
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        System.out.println("[DEBUG] Fetching user with ID: " + id);

        ResponseEntity<?> authResponse = validateTokenAndRole(authHeader, "USER");
        if (!authResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(authResponse.getStatusCode())
                    .body(new UserResponse(authResponse.getBody().toString()));
        }

        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(new UserResponse(user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new UserResponse("User not found")));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(@RequestHeader("Authorization") String authHeader) {
        System.out.println("[DEBUG] Fetching all users");

        ResponseEntity<?> authResponse = validateTokenAndRole(authHeader, "USER");
        if (!authResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(authResponse.getStatusCode())
                    .body(List.of(new UserResponse(authResponse.getBody().toString())));
        }

        List<User> users = userRepository.findAll();
        List<UserResponse> userResponses = users.stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userResponses);
    }

    @PutMapping("/id/{id}")
    public ResponseEntity<String> updateUser(@PathVariable Long id, @RequestBody UserRequest userRequest, @RequestHeader("Authorization") String authHeader) {
        System.out.println("[DEBUG] Updating user with ID: " + id);

        ResponseEntity<?> authResponse = validateTokenAndRole(authHeader, "USER");
        if (!authResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(authResponse.getStatusCode()).body(authResponse.getBody().toString());
        }

        return userRepository.findById(id)
                .map(user -> {
                    user.setUsername(userRequest.getUsername());
                    user.setEmail(userRequest.getEmail());
                    user.setFirstName(userRequest.getFirstName());
                    user.setLastName(userRequest.getLastName());
                    userRepository.save(user);
                    return ResponseEntity.ok("User updated successfully");
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
    }

    @DeleteMapping("/id/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        System.out.println("[DEBUG] Deleting user with ID: " + id);

        ResponseEntity<?> authResponse = validateTokenAndRole(authHeader, "USER");
        if (!authResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(authResponse.getStatusCode()).body(authResponse.getBody().toString());
        }

        return userRepository.findById(id)
                .map(user -> {
                    userRepository.delete(user);
                    return ResponseEntity.ok("User deleted successfully");
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        System.out.println("[DEBUG] Fetching current user");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserResponse("Invalid or missing Authorization header"));
        }

        String jwt = authHeader.substring(7);
        try {
            if (!jwtUtil.validateToken(jwt)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new UserResponse("Invalid token"));
            }

            String username = jwtUtil.extractUsername(jwt);
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new UserResponse("Invalid token: username not found"));
            }

            return userRepository.findByUsername(username)
                    .map(user -> ResponseEntity.ok(new UserResponse(user)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new UserResponse("User not found")));
        } catch (Exception e) {
            System.out.println("[DEBUG] Error validating token: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponse("Error validating token: " + e.getMessage()));
        }
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username, @RequestHeader("Authorization") String authHeader) {
        System.out.println("[DEBUG] Fetching user with username: " + username);

        ResponseEntity<?> authResponse = validateTokenAndRole(authHeader, "USER");
        if (!authResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(authResponse.getStatusCode())
                    .body(new UserResponse(authResponse.getBody().toString()));
        }

        return userRepository.findByUsername(username)
                .map(user -> ResponseEntity.ok(new UserResponse(user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new UserResponse("User not found")));
    }

    private ResponseEntity<?> validateTokenAndRole(String authHeader, String requiredRole) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing Authorization header");
        }

        String jwt = authHeader.substring(7);
        try {
            if (!jwtUtil.validateToken(jwt)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }

            String username = jwtUtil.extractUsername(jwt);
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token: username not found");
            }

            ResponseEntity<RoleDTO[]> roleResponse = authServiceClient.getRolesByUsername(username);
            if (roleResponse.getStatusCode().is2xxSuccessful()) {
                RoleDTO[] roles = roleResponse.getBody();
                if (roles != null) {
                    for (RoleDTO role : roles) {
                        if (role.getName().equals(requiredRole)) {
                            return ResponseEntity.ok().build();
                        }
                    }
                }
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Required role " + requiredRole + " not found");
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unable to fetch roles");
        } catch (Exception e) {
            System.out.println("[DEBUG] Error validating token: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error validating token: " + e.getMessage());
        }
    }
}

class UserRequest {
    private String username;
    private String email;
    private String firstName;
    private String lastName;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
}

class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String errorMessage;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
    }

    public UserResponse(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}