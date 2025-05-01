package org.example.workoutservice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {

    private final WorkoutRepository workoutRepository;
    private final JwtUtil jwtUtil;
    private final UserServiceClient userClient;

    public WorkoutController(WorkoutRepository workoutRepository, JwtUtil jwtUtil, UserServiceClient userClient) {
        this.workoutRepository = workoutRepository;
        this.jwtUtil = jwtUtil;
        this.userClient = userClient;
    }

    @PostMapping
    public ResponseEntity<Workout> addWorkout(@RequestBody Workout workout, @RequestHeader("Authorization") String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            String username = jwtUtil.extractUsername(token.replace("Bearer ", ""));
            if (!jwtUtil.validateToken(token.replace("Bearer ", ""))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            UserDto user = userClient.getUserByUsername(username, token);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            workout.setUsername(username);
            return ResponseEntity.ok(workoutRepository.save(workout));
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to add workout: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @GetMapping
    public List<Workout> getMyWorkouts(@RequestHeader("Authorization") String token) {
        String username = jwtUtil.extractUsername(token.replace("Bearer ", ""));
        return workoutRepository.findByUsername(username);
    }
}
