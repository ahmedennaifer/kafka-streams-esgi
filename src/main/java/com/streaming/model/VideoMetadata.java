package com.streaming.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Représente les métadonnées d'une vidéo
 */
public class VideoMetadata {
    @JsonProperty("video_id")
    private String videoId;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("genre")
    private String genre;
    
    @JsonProperty("release_year")
    private int releaseYear;
    
    @JsonProperty("duration_minutes")
    private int durationMinutes;

    public VideoMetadata() {}

    public VideoMetadata(String videoId, String title, String genre, int releaseYear, int durationMinutes) {
        this.videoId = videoId;
        this.title = title;
        this.genre = genre;
        this.releaseYear = releaseYear;
        this.durationMinutes = durationMinutes;
    }

    // Getters and Setters
    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(int releaseYear) {
        this.releaseYear = releaseYear;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    @Override
    public String toString() {
        return "VideoMetadata{" +
                "videoId='" + videoId + '\'' +
                ", title='" + title + '\'' +
                ", genre='" + genre + '\'' +
                ", releaseYear=" + releaseYear +
                ", durationMinutes=" + durationMinutes +
                '}';
    }
}
