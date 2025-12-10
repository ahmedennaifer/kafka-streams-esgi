package com.streaming.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ModelsUnitTest {

    @Test
    void testVideoMetadata() {
        VideoMetadata metadata = new VideoMetadata("v1", "TestTitle", "ACTION", 2020, 120);

        assertEquals("v1", metadata.getVideoId());
        assertEquals("TestTitle", metadata.getTitle());
        assertEquals("ACTION", metadata.getGenre());
        assertEquals(2020, metadata.getReleaseYear());
        assertEquals(120, metadata.getDurationMinutes());
    }

    @Test
    void testVideoViewModel() {
        VideoView view = new VideoView("u1", "v1", 30, "DRAMA");

        assertEquals("u1", view.getUserId());
        assertEquals("v1", view.getVideoId());
        assertEquals(30, view.getDurationSeconds());
        assertEquals("DRAMA", view.getGenre());
    }

    @Test
    void testUserSubscription() {
        UserSubscription subscription = new UserSubscription("u1", UserSubscription.SubscriptionTier.PREMIUM);

        assertEquals("u1", subscription.getUserId());
        assertEquals(UserSubscription.SubscriptionTier.PREMIUM, subscription.getTier());
    }

    @Test
    void testEnrichedVideoView() {
        VideoView view = new VideoView("u1", "v1", 40, "ACTION");
        VideoMetadata metadata = new VideoMetadata("v1", "TitleX", "ACTION", 2021, 90);
        UserSubscription subscription =
                new UserSubscription("u1", UserSubscription.SubscriptionTier.VIP);

        EnrichedVideoView enriched =
                new EnrichedVideoView(view, metadata, subscription);

        assertEquals("u1", enriched.getUserId());
        assertEquals("v1", enriched.getVideoId());
        assertEquals("TitleX", enriched.getVideoTitle());
        assertEquals("ACTION", enriched.getGenre());
        assertEquals(40, enriched.getDurationSeconds());
        assertEquals("VIP", enriched.getSubscriptionTier());
    }
}
