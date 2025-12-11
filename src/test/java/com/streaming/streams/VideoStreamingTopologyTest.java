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


//testVideoViewCounts	
//testWatchTimeAggregation	
//testViewsByGenre
//testSubscriptionJoin


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
    void testVideoViewCountscomplexe() {

        // Envoi des données dans le désordre
        videoViewsTopic.pipeInput("v2", new VideoView("u8", "v2", 15, "ACTION"));
        videoViewsTopic.pipeInput("v1", new VideoView("u1", "v1", 10, "ACTION"));
        videoViewsTopic.pipeInput("v3", new VideoView("u3", "v3", 25, "DRAMA"));
        videoViewsTopic.pipeInput("v1", new VideoView("u2", "v1", 20, "ACTION"));
        videoViewsTopic.pipeInput("v2", new VideoView("u4", "v2", 40, "ACTION"));
        videoViewsTopic.pipeInput("v1", new VideoView("u1", "v1", 12, "ACTION")); // même user
        videoViewsTopic.pipeInput("v4", new VideoView("u9", "v4", 30, "COMEDY"));
        videoViewsTopic.pipeInput("v3", new VideoView("u3", "v3", 10, "DRAMA"));

        KeyValueStore<String, Long> store =
                testDriver.getKeyValueStore("video-view-counts");

        // Vérifications
        assertEquals(3L, store.get("v1"));  // v1 vue 3 fois
        assertEquals(2L, store.get("v2"));  // v2 vue 2 fois
        assertEquals(2L, store.get("v3"));  // v3 vue 2 fois
        assertEquals(1L, store.get("v4"));  // v4 vue 1 fois
        // Vidéo jamais vue
        assertEquals(null, store.get("v5"));
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
    void testWatchTimeAggregationComplex() {
        // Vues mélangées (différentes vidéos, utilisateurs)
        videoViewsTopic.pipeInput("v10", new VideoView("u1", "v10", 15, "ACTION"));
        videoViewsTopic.pipeInput("v10", new VideoView("u2", "v10", 25, "ACTION"));
        videoViewsTopic.pipeInput("v10", new VideoView("u3", "v10", 40, "ACTION"));

        // Autre vidéo pour vérifier que les stores ne se mélangent pas
        videoViewsTopic.pipeInput("v20", new VideoView("u1", "v20", 5, "DRAMA"));
        videoViewsTopic.pipeInput("v20", new VideoView("u4", "v20", 50, "DRAMA"));

        KeyValueStore<String, Long> store =
                testDriver.getKeyValueStore("video-watch-time");


        assertEquals(80L, store.get("v10"));
        assertEquals(55L, store.get("v20"));
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
    void testViewsByGenre_MultipleComplexCases() {
        // Metadata
        metadataTopic.pipeInput("A1", new VideoMetadata("A1", "ActionMovie1", "ACTION", 2020, 100));
        metadataTopic.pipeInput("A2", new VideoMetadata("A2", "ActionMovie2", "ACTION", 2021, 110));
        metadataTopic.pipeInput("D1", new VideoMetadata("D1", "DramaMovie", "DRAMA", 2022, 120));
        metadataTopic.pipeInput("C1", new VideoMetadata("C1", "ComedyMovie", "COMEDY", 2019, 90));
        // Vues de ACTION
        videoViewsTopic.pipeInput("A1", new VideoView("u1", "A1", 10, "ACTION"));
        videoViewsTopic.pipeInput("A1", new VideoView("u2", "A1", 15, "ACTION"));
        videoViewsTopic.pipeInput("A2", new VideoView("u3", "A2", 20, "ACTION"));
        // Vues DRAMA
        videoViewsTopic.pipeInput("D1", new VideoView("u4", "D1", 12, "DRAMA"));
        // Vues COMEDY
        videoViewsTopic.pipeInput("C1", new VideoView("u5", "C1", 22, "COMEDY"));
        videoViewsTopic.pipeInput("C1", new VideoView("u6", "C1", 35, "COMEDY"));
        // Récupération du store
        KeyValueStore<String, Long> store = testDriver.getKeyValueStore("views-by-genre");

        assertEquals(3L, store.get("ACTION"));   
        assertEquals(1L, store.get("DRAMA"));    
        assertEquals(2L, store.get("COMEDY"));   
    }


    @Test
    void testSubscriptionJoin() {
        // Abonnement
        subscriptionTopic.pipeInput("u1",new UserSubscription("u1", UserSubscription.SubscriptionTier.PREMIUM));

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


    @Test
    void testSubscriptionJoinComplex() {
        //Définition des abonnements
        subscriptionTopic.pipeInput("u1",new UserSubscription("u1", UserSubscription.SubscriptionTier.PREMIUM));
        subscriptionTopic.pipeInput("u2",new UserSubscription("u2", UserSubscription.SubscriptionTier.FREE));
        subscriptionTopic.pipeInput("u3",new UserSubscription("u3", UserSubscription.SubscriptionTier.VIP));
        //Metadata vidéos
        metadataTopic.pipeInput("vA", new VideoMetadata("vA", "MovieA", "ACTION", 2020, 100));
        metadataTopic.pipeInput("vB", new VideoMetadata("vB", "MovieB", "DRAMA", 2021, 95));
        //Vues pour chaque utilisateur 
        // PREMIUM user
        videoViewsTopic.pipeInput("vA", new VideoView("u1", "vA", 10, "ACTION"));
        videoViewsTopic.pipeInput("vB", new VideoView("u1", "vB", 20, "DRAMA"));
        // FREE user
        videoViewsTopic.pipeInput("vA", new VideoView("u2", "vA", 15, "ACTION"));
        // VIP user
        videoViewsTopic.pipeInput("vA", new VideoView("u3", "vA", 40, "ACTION"));
        videoViewsTopic.pipeInput("vB", new VideoView("u3", "vB", 50, "DRAMA"));
        // User without subscription 
        videoViewsTopic.pipeInput("vA", new VideoView("u4", "vA", 25, "ACTION"));
        //On récupère le store final
        KeyValueStore<String, Long> store = testDriver.getKeyValueStore("views-by-subscription");
        //Vérifications
        assertEquals(2L, store.get("PREMIUM"), "u1 should contribute 2 views");
        assertEquals(1L, store.get("FREE"), "u2 should contribute 1 view");
        assertEquals(2L, store.get("VIP"), "u3 should contribute 2 views");
    }

}
