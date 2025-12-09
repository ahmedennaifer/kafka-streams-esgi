package com.streaming.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Représente les statistiques d'une vidéo (résultat d'agrégation)
 */
public class VideoStats {
    @JsonProperty("video_id")
    private String videoId;
    
    @JsonProperty("view_count")
    private long viewCount;
    
    @JsonProperty("total_watch_time_seconds")
    private long totalWatchTimeSeconds;

    public VideoStats() {}

    public VideoStats(String videoId, long viewCount, long totalWatchTimeSeconds) {
        this.videoId = videoId;
        this.viewCount = viewCount;
        this.totalWatchTimeSeconds = totalWatchTimeSeconds;
    }

    // Getters and Setters
    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public long getViewCount() {
        return viewCount;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }

    public long getTotalWatchTimeSeconds() {
        return totalWatchTimeSeconds;
    }

    public void setTotalWatchTimeSeconds(long totalWatchTimeSeconds) {
        this.totalWatchTimeSeconds = totalWatchTimeSeconds;
    }

    @Override
    public String toString() {
        return "VideoStats{" +
                "videoId='" + videoId + '\'' +
                ", viewCount=" + viewCount +
                ", totalWatchTimeSeconds=" + totalWatchTimeSeconds +
                '}';
    }
}
