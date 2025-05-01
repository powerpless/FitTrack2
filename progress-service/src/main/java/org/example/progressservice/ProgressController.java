package org.example.progressservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressRepository progressRepository;
    private final UserServiceClient userClient;
    private final JwtUtil jwtUtil;

    @Autowired
    public ProgressController(ProgressRepository progressRepository, UserServiceClient userClient, JwtUtil jwtUtil) {
        this.progressRepository = progressRepository;
        this.userClient = userClient;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<ProgressResponse> addProgress(@RequestBody ProgressRequest request, @RequestHeader("Authorization") String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ProgressResponse("Missing token"));
            }
            String jwt = token.replace("Bearer ", "");
            if (!jwtUtil.validateToken(jwt)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ProgressResponse("Invalid token"));
            }
            String username = jwtUtil.extractUsername(jwt);
            UserDto user = userClient.getUserByUsername(username, token);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ProgressResponse("User not found"));
            }

            Progress progress = new Progress();
            progress.setExerciseName(request.getExerciseName());
            progress.setWeight(request.getWeight());
            progress.setRepetitions(request.getRepetitions());
            progress.setDate(LocalDate.now());
            progress.setUsername(username);

            Progress saved = progressRepository.save(progress);
            return ResponseEntity.ok(new ProgressResponse(saved));
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to add progress: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ProgressResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<ProgressResponse>> getMyProgress(@RequestHeader("Authorization") String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(List.of(new ProgressResponse("Missing token")));
            }
            String jwt = token.replace("Bearer ", "");
            if (!jwtUtil.validateToken(jwt)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(List.of(new ProgressResponse("Invalid token")));
            }
            String username = jwtUtil.extractUsername(jwt);
            UserDto user = userClient.getUserByUsername(username, token);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of(new ProgressResponse("User not found")));
            }

            List<Progress> progressList = progressRepository.findByUsername(username);
            List<ProgressResponse> responses = progressList.stream()
                    .map(ProgressResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to fetch progress: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of(new ProgressResponse("Error: " + e.getMessage())));
        }
    }
}