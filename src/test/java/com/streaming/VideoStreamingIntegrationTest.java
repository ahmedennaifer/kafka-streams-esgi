package com.streaming.integration;

import com.streaming.model.UserSubscription;
import com.streaming.model.VideoMetadata;
import com.streaming.model.VideoView;
import com.streaming.streams.VideoStreamingTopology;
import com.streaming.utils.JsonSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.state.KeyValueStore;
import org.junit.jupiter.api.*;

import java.util.Properties;

public class VideoStreamingIntegrationTest {

    private static TopologyTestDriver testDriver;
    private static TestInputTopic<String, VideoView> viewsTopic;
    private static TestInputTopic<String, VideoMetadata> metadataTopic;
    private static TestInputTopic<String, UserSubscription> subscriptionTopic;

    @BeforeAll
    static void setup() throws Exception {
        VideoStreamingTopology topology = new VideoStreamingTopology();
        Topology topo = topology.buildTopologyForTest();

        Properties props = new Properties();
        props.put("application.id", "integration-test");
        props.put("bootstrap.servers", "dummy:1234");
        props.put("state.dir", "test_state_integration");

        testDriver = new TopologyTestDriver(topo, props);

        JsonSerde<VideoView> viewSerde = new JsonSerde<>(VideoView.class);
        JsonSerde<VideoMetadata> metadataSerde = new JsonSerde<>(VideoMetadata.class);
        JsonSerde<UserSubscription> subscriptionSerde = new JsonSerde<>(UserSubscription.class);

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

    @AfterAll
    static void teardown() {
        if (testDriver != null) testDriver.close();
    }

    @Test
    void testIntegrationQuery() throws Exception {
        metadataTopic.pipeInput("v1",
                new VideoMetadata("v1", "Test", "ACTION", 2020, 90));

        viewsTopic.pipeInput("v1",
                new VideoView("u1", "v1", 10, "ACTION"));

        KeyValueStore<String, Long> store =
                testDriver.getKeyValueStore("video-view-counts");

        Long count = store.get("v1");

        Assertions.assertEquals(1L, count);
    }
}
