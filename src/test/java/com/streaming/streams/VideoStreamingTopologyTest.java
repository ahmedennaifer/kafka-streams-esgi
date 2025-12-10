package com.streaming.streams;

import com.streaming.config.KafkaConfig;
import com.streaming.model.UserSubscription;
import com.streaming.model.VideoMetadata;
import com.streaming.model.VideoView;
import com.streaming.utils.JsonSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VideoStreamingTopologyTest {

    private TopologyTestDriver testDriver;

    private TestInputTopic<String, VideoView> videoViewsTopic;
    private TestInputTopic<String, VideoMetadata> metadataTopic;
    private TestInputTopic<String, UserSubscription> subscriptionTopic;

    @BeforeEach
    void setup() {
        // On utilise la méthode spéciale pour les tests
        VideoStreamingTopology topology = new VideoStreamingTopology();
        Topology topo = topology.buildTopologyForTest();

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-kafka");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        props.put(StreamsConfig.STATE_DIR_CONFIG, "test_state");

        testDriver = new TopologyTestDriver(topo, props);

        JsonSerde<VideoView> viewSerde = new JsonSerde<>(VideoView.class);
        JsonSerde<VideoMetadata> metadataSerde = new JsonSerde<>(VideoMetadata.class);
        JsonSerde<UserSubscription> subscriptionSerde = new JsonSerde<>(UserSubscription.class);

        videoViewsTopic = testDriver.createInputTopic(
                KafkaConfig.VIDEO_VIEWS_TOPIC,
                Serdes.String().serializer(),
                viewSerde.serializer()
        );

        metadataTopic = testDriver.createInputTopic(
                KafkaConfig.VIDEO_METADATA_TOPIC,
                Serdes.String().serializer(),
                metadataSerde.serializer()
        );

        subscriptionTopic = testDriver.createInputTopic(
                KafkaConfig.USER_SUBSCRIPTIONS_TOPIC,
                Serdes.String().serializer(),
                subscriptionSerde.serializer()
        );
    }

    @AfterEach
    void teardown() {
        if (testDriver != null) {
            testDriver.close();
        }
    }

    @Test
    void testVideoViewCounts() {
        videoViewsTopic.pipeInput("v1", new VideoView("u1", "v1", 10, "ACTION"));
        videoViewsTopic.pipeInput("v1", new VideoView("u2", "v1", 20, "ACTION"));

        KeyValueStore<String, Long> store =
                testDriver.getKeyValueStore("video-view-counts");

        assertEquals(2L, store.get("v1"));
    }

    @Test
    void testWatchTimeAggregation() {
        videoViewsTopic.pipeInput("v2", new VideoView("u1", "v2", 30, "DRAMA"));
        videoViewsTopic.pipeInput("v2", new VideoView("u2", "v2", 40, "DRAMA"));

        KeyValueStore<String, Long> store =
                testDriver.getKeyValueStore("video-watch-time");

        assertEquals(70L, store.get("v2"));
    }

    @Test
    void testViewsByGenre() {
        // Metadata d'abord
        metadataTopic.pipeInput("g1", new VideoMetadata("g1", "T1", "ACTION", 2020, 90));
        metadataTopic.pipeInput("g2", new VideoMetadata("g2", "T2", "ACTION", 2020, 90));
        metadataTopic.pipeInput("g3", new VideoMetadata("g3", "T3", "DRAMA", 2020, 90));

        // Vues
        videoViewsTopic.pipeInput("g1", new VideoView("u1", "g1", 10, "ACTION"));
        videoViewsTopic.pipeInput("g2", new VideoView("u2", "g2", 20, "ACTION"));
        videoViewsTopic.pipeInput("g3", new VideoView("u3", "g3", 30, "DRAMA"));

        KeyValueStore<String, Long> store =
                testDriver.getKeyValueStore("views-by-genre");

        assertEquals(2L, store.get("ACTION"));
        assertEquals(1L, store.get("DRAMA"));
    }

    @Test
    void testSubscriptionJoin() {
        // Abonnement
        subscriptionTopic.pipeInput("u1",
                new UserSubscription("u1", UserSubscription.SubscriptionTier.PREMIUM)
        );

        // Metadata vidéo
        metadataTopic.pipeInput("x", new VideoMetadata("x", "Dummy", "ACTION", 2020, 90));

        // Vue
        videoViewsTopic.pipeInput("x",
                new VideoView("u1", "x", 10, "ACTION")
        );

        KeyValueStore<String, Long> store =
                testDriver.getKeyValueStore("views-by-subscription");

        assertEquals(1L, store.get("PREMIUM"));
    }
}
