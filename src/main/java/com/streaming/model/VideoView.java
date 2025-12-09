package com.streaming.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Représente un événement de visionnage de vidéo
 */
public class VideoView {
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("video_id")
    private String videoId;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("duration_seconds")
    private int durationSeconds;
    
    @JsonProperty("genre")
    private String genre;

    public VideoView() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    public VideoView(String userId, String videoId, int durationSeconds, String genre) {
        this.userId = userId;
        this.videoId = videoId;
        this.durationSeconds = durationSeconds;
        this.genre = genre;
        this.timestamp = Instant.now().toEpochMilli();
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    @Override
    public String toString() {
        return "VideoView{" +
                "userId='" + userId + '\'' +
                ", videoId='" + videoId + '\'' +
                ", timestamp=" + timestamp +
                ", durationSeconds=" + durationSeconds +
                ", genre='" + genre + '\'' +
                '}';
    }
}
