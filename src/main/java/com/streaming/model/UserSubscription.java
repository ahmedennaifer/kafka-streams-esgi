package com.streaming.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Représente l'abonnement d'un utilisateur
 */
public class UserSubscription {
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("tier")
    private SubscriptionTier tier;
    
    @JsonProperty("timestamp")
    private long timestamp;

    public UserSubscription() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    public UserSubscription(String userId, SubscriptionTier tier) {
        this.userId = userId;
        this.tier = tier;
        this.timestamp = Instant.now().toEpochMilli();
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public SubscriptionTier getTier() {
        return tier;
    }

    public void setTier(SubscriptionTier tier) {
        this.tier = tier;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "UserSubscription{" +
                "userId='" + userId + '\'' +
                ", tier=" + tier +
                ", timestamp=" + timestamp +
                '}';
    }

    public enum SubscriptionTier {
        FREE, PREMIUM, VIP
    }
}
