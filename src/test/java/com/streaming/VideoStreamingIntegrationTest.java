package com.streaming.integration;

import com.streaming.model.UserSubscription;
import com.streaming.model.VideoMetadata;
import com.streaming.model.VideoView;
import com.streaming.streams.VideoStreamingTopology;
import com.streaming.utils.JsonSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.junit.jupiter.api.*;

import java.util.Properties;

/**
 * TESTS D'INTÉGRATION DE LA TOPOLOGIE KAFKA STREAMS
 *
 * Ici on teste la topologie complète :
 * - la création des stores
 * - la cohérence des agrégations
 * - le bon fonctionnement des joins
 *
 * Chaque test démarre une topologie NEUVE.
 */
public class VideoStreamingIntegrationTest {

    private TopologyTestDriver testDriver;
    private TestInputTopic<String, VideoView> viewsTopic;
    private TestInputTopic<String, VideoMetadata> metadataTopic;
    private TestInputTopic<String, UserSubscription> subscriptionTopic;

    /**
     *  Avant CHAQUE test :
     * - On reconstruit la topologie
     * - On recrée un TestDriver neuf
     * - On recrée les topics d'entrée
     *
     *  Cela évite que les tests se polluent entre eux.
     */
    @BeforeEach
    void setup() {

        VideoStreamingTopology topology = new VideoStreamingTopology();
        Topology topo = topology.buildTopologyForTest();

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "integration-test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        props.put(StreamsConfig.STATE_DIR_CONFIG, "test_state_integration");

        // TestDriver NEUF
        testDriver = new TopologyTestDriver(topo, props);

        JsonSerde<VideoView> viewSerde = new JsonSerde<>(VideoView.class);
        JsonSerde<VideoMetadata> metadataSerde = new JsonSerde<>(VideoMetadata.class);
        JsonSerde<UserSubscription> subscriptionSerde = new JsonSerde<>(UserSubscription.class);

        // Création des topics d'entrée
        viewsTopic = testDriver.createInputTopic(
                "video-views",
                Serdes.String().serializer(),
                viewSerde.serializer());

        metadataTopic = testDriver.createInputTopic(
                "video-metadata",
                Serdes.String().serializer(),
                metadataSerde.serializer());

        subscriptionTopic = testDriver.createInputTopic(
                "user-subscriptions",
                Serdes.String().serializer(),
                subscriptionSerde.serializer());
    }

    /**
     * Après CHAQUE test :
     * On ferme proprement le TestDriver
     */
    @AfterEach
    void teardown() {
        if (testDriver != null) {
            testDriver.close();
        }
    }

    //TEST 1 : VIEW COUNT
    @Test
    void testVideoViewCounts() {

        viewsTopic.pipeInput("v1", new VideoView("u1", "v1", 10, "ACTION"));
        viewsTopic.pipeInput("v1", new VideoView("u2", "v1", 20, "ACTION"));

        KeyValueStore<String, Long> store =
                testDriver.getKeyValueStore("video-view-counts");

        Assertions.assertEquals(2L, store.get("v1"));
    }

    //TEST 2 : WATCH TIME
    @Test
    void testWatchTimeAggregation() {

        viewsTopic.pipeInput("v2", new VideoView("u1", "v2", 30, "DRAMA"));
        viewsTopic.pipeInput("v2", new VideoView("u2", "v2", 40, "DRAMA"));

        KeyValueStore<String, Long> store =
                testDriver.getKeyValueStore("video-watch-time");

        Assertions.assertEquals(70L, store.get("v2"));
    }

    //TEST 3 : BY GENRE
    @Test
    void testViewsByGenre() {

        metadataTopic.pipeInput("g1", new VideoMetadata("g1", "T1", "ACTION", 2020, 90));
        metadataTopic.pipeInput("g2", new VideoMetadata("g2", "T2", "ACTION", 2020, 90));
        metadataTopic.pipeInput("g3", new VideoMetadata("g3", "T3", "DRAMA", 2020, 90));

        viewsTopic.pipeInput("g1", new VideoView("u1", "g1", 10, "ACTION"));
        viewsTopic.pipeInput("g2", new VideoView("u2", "g2", 20, "ACTION"));
        viewsTopic.pipeInput("g3", new VideoView("u3", "g3", 30, "DRAMA"));

        KeyValueStore<String, Long> store =
                testDriver.getKeyValueStore("views-by-genre");

        Assertions.assertEquals(2L, store.get("ACTION"));
        Assertions.assertEquals(1L, store.get("DRAMA"));
    }

    //TEST 4 : SUBSCRIPTION JOIN
    @Test
    void testSubscriptionJoin() {

        // Abonnement PREMIUM pour u1
        subscriptionTopic.pipeInput("u1",
                new UserSubscription("u1", UserSubscription.SubscriptionTier.PREMIUM));

        metadataTopic.pipeInput("x", new VideoMetadata("x", "Dummy", "ACTION", 2020, 90));

        // Vue qui doit récupérer l'abonnement PREMIUM
        viewsTopic.pipeInput("x",
                new VideoView("u1", "x", 10, "ACTION"));

        KeyValueStore<String, Long> store =
                testDriver.getKeyValueStore("views-by-subscription");

        Assertions.assertEquals(1L, store.get("PREMIUM"));
    }





}
