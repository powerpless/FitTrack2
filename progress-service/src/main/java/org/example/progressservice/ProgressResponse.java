package org.example.progressservice;

import java.time.LocalDate;

public class ProgressResponse {
    private Long id;
    private String exerciseName;
    private Integer weight;
    private Integer repetitions;
    private LocalDate date;
    private String username;
    private String errorMessage;

    public ProgressResponse(Progress progress) {
        this.id = progress.getId();
        this.exerciseName = progress.getExerciseName();
        this.weight = progress.getWeight();
        this.repetitions = progress.getRepetitions();
        this.date = progress.getDate();
        this.username = progress.getUsername();
    }

    public ProgressResponse(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }
    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }
    public Integer getRepetitions() { return repetitions; }
    public void setRepetitions(Integer repetitions) { this.repetitions = repetitions; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
