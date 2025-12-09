package com.streaming.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Représente un événement de visionnage enrichi avec les métadonnées de la vidéo
 * et l'abonnement de l'utilisateur (résultat d'un join)
 */
public class EnrichedVideoView {
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("video_id")
    private String videoId;
    
    @JsonProperty("video_title")
    private String videoTitle;
    
    @JsonProperty("genre")
    private String genre;
    
    @JsonProperty("duration_seconds")
    private int durationSeconds;
    
    @JsonProperty("subscription_tier")
    private String subscriptionTier;
    
    @JsonProperty("timestamp")
    private long timestamp;

    public EnrichedVideoView() {}

    public EnrichedVideoView(VideoView view, VideoMetadata metadata, UserSubscription subscription) {
        this.userId = view.getUserId();
        this.videoId = view.getVideoId();
        this.durationSeconds = view.getDurationSeconds();
        this.timestamp = view.getTimestamp();
        
        if (metadata != null) {
            this.videoTitle = metadata.getTitle();
            this.genre = metadata.getGenre();
        } else {
            this.videoTitle = "Unknown";
            this.genre = view.getGenre();
        }
        
        if (subscription != null) {
            this.subscriptionTier = subscription.getTier().name();
        } else {
            this.subscriptionTier = "UNKNOWN";
        }
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

    public String getVideoTitle() {
        return videoTitle;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getSubscriptionTier() {
        return subscriptionTier;
    }

    public void setSubscriptionTier(String subscriptionTier) {
        this.subscriptionTier = subscriptionTier;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "EnrichedVideoView{" +
                "userId='" + userId + '\'' +
                ", videoId='" + videoId + '\'' +
                ", videoTitle='" + videoTitle + '\'' +
                ", genre='" + genre + '\'' +
                ", durationSeconds=" + durationSeconds +
                ", subscriptionTier='" + subscriptionTier + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
