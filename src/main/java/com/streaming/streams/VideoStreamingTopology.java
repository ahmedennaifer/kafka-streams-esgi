package com.streaming.streams;

import com.streaming.config.KafkaConfig;
import com.streaming.model.*;
import com.streaming.utils.JsonSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Topologie Kafka Streams principale
 *
 * OBJECTIFS DU PROJET:
 * 1. Créer une KTable à partir du topic video-metadata
 * 2. Créer une KTable à partir du topic user-subscriptions
 * 3. Lire le stream video-views et effectuer des agrégations:
 * - Compter le nombre de vues par vidéo
 * - Calculer le temps de visionnage total par utilisateur
 * - Calculer le nombre de vues par genre
 * 4. Effectuer un join entre le stream de vues et la KTable de métadonnées
 * 5. Effectuer un join entre les vues enrichies et la KTable d'abonnements
 * 6. Calculer les vidéos "trending" avec une fenêtre temporelle de 5 minutes
 *
 * Les State Stores créés seront ensuite exposés via l'API REST
 */
public class VideoStreamingTopology {

    private static final Logger logger = LoggerFactory.getLogger(VideoStreamingTopology.class);
    private KafkaStreams streams;

    /**
     * Construit la topologie Kafka Streams
     * À COMPLÉTER PAR LES ÉTUDIANTS
     *
     * (Version utilisée par l'application réelle)
     */
    public void buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();
        Topology topology = buildTopologyInternal(builder);

        streams = new KafkaStreams(topology, KafkaConfig.getStreamsConfig("video-streaming-app"));
        logger.info("Topologie Kafka Streams construite avec succès");
    }

    /**
     * Version utilisée PAR LES TESTS.
     * Elle construit la topologie mais ne démarre PAS KafkaStreams.
     */
    public Topology buildTopologyForTest() {
        StreamsBuilder builder = new StreamsBuilder();
        return buildTopologyInternal(builder);
    }

    /**
     * Méthode interne contenant tout le code (inchangé) du professeur.
     * Les commentaires du prof sont strictement préservés.
     */
    private Topology buildTopologyInternal(StreamsBuilder builder) {

        // Serdes pour la sérialisation/désérialisation
        JsonSerde<VideoView> videoViewSerde = new JsonSerde<>(VideoView.class);
        JsonSerde<VideoMetadata> videoMetadataSerde = new JsonSerde<>(VideoMetadata.class);
        JsonSerde<UserSubscription> userSubscriptionSerde = new JsonSerde<>(UserSubscription.class);
        JsonSerde<VideoStats> videoStatsSerde = new JsonSerde<>(VideoStats.class);
        JsonSerde<EnrichedVideoView> enrichedViewSerde = new JsonSerde<>(EnrichedVideoView.class);

        // ==============================================
        // TODO 1: CRÉER LES KTABLES
        // ==============================================

        // TODO 1.1: Créer une KTable à partir du topic video-metadata
        KTable<String, VideoMetadata> videoMetadataTable = builder.table(
                KafkaConfig.VIDEO_METADATA_TOPIC,
                Consumed.with(Serdes.String(), videoMetadataSerde),
                Materialized.as("video-metadata-store"));

        // TODO 1.2: Créer une KTable à partir du topic user-subscriptions
        KTable<String, UserSubscription> userSubscriptionsTable = builder.table(
                KafkaConfig.USER_SUBSCRIPTIONS_TOPIC,
                Consumed.with(Serdes.String(), userSubscriptionSerde),
                Materialized.as("user-subscriptions-store"));

        // ==============================================
        // TODO 2: LIRE LE STREAM DE VUES
        // ==============================================

        KStream<String, VideoView> videoViewsStream = builder.stream(
                KafkaConfig.VIDEO_VIEWS_TOPIC,
                Consumed.with(Serdes.String(), videoViewSerde));

        // ==============================================
        // TODO 3: AGRÉGATIONS
        // ==============================================

        // TODO 3.1: Compter le nombre de vues par vidéo
        videoViewsStream
                .groupByKey(Grouped.with(Serdes.String(), videoViewSerde))
                .count(Materialized.as("video-view-counts"));

        // TODO 3.2: Temps de visionnage total par vidéo
        videoViewsStream
                .groupByKey(Grouped.with(Serdes.String(), videoViewSerde))
                .aggregate(
                        () -> 0L,
                        (key, view, agg) -> agg + view.getDurationSeconds(),
                        Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("video-watch-time")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(Serdes.Long()));

        // TODO 3.3: Calculer le nombre de vues par genre
        videoViewsStream
                .selectKey((key, value) -> value.getGenre())
                .groupByKey(Grouped.with(Serdes.String(), videoViewSerde))
                .count(Materialized.as("views-by-genre"));

        // TODO 3.4: Calculer le temps de visionnage total par utilisateur
        videoViewsStream
                .selectKey((key, value) -> value.getUserId())
                .groupByKey(Grouped.with(Serdes.String(), videoViewSerde))
                .aggregate(
                        () -> 0L,
                        (key, view, agg) -> agg + view.getDurationSeconds(),
                        Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("user-watch-time")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(Serdes.Long()));

        // ==============================================
        // TODO 4: JOIN STREAM-TABLE
        // ==============================================

        KStream<String, EnrichedVideoView> enrichedViews = videoViewsStream.join(
                videoMetadataTable,
                (view, metadata) -> new EnrichedVideoView(view, metadata, null),
                Joined.with(Serdes.String(), videoViewSerde, videoMetadataSerde));

        // ==============================================
        // TODO 5: JOIN STREAM-TABLE (enrichissement avec abonnement)
        // ==============================================

        KStream<String, EnrichedVideoView> fullyEnrichedViews =
                enrichedViews
                        .selectKey((key, value) -> value.getUserId())
                        .leftJoin(
                                userSubscriptionsTable,
                                (videoView, userSubscription) -> {
                                if (userSubscription != null) {
                                        videoView.setSubscriptionTier(userSubscription.getTier().name()); //retourne free premium ou vip
                                }
                                return videoView;
                                },
                                Joined.with(
                                        Serdes.String(),
                                        enrichedViewSerde,         // <-- On utilise le serde original
                                        userSubscriptionSerde
                                )
                        );

        // TODO 5.2: Calculer les statistiques par type d'abonnement
        KTable<String, Long> viewsBySubscription = fullyEnrichedViews
                .selectKey((key, value) -> value.getSubscriptionTier())
                .groupByKey(Grouped.with(Serdes.String(), enrichedViewSerde))
                .count(Materialized.as("views-by-subscription"));

        // ==============================================
        // TODO 6: WINDOWING - TRENDING VIDEOS
        // ==============================================

        videoViewsStream
                .groupByKey(Grouped.with(Serdes.String(), videoViewSerde))
                .windowedBy(
                        TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5))
                                .advanceBy(Duration.ofMinutes(1)))
                .count(Materialized.as("trending-videos"));

        return builder.build();
    }

    /**
     * Démarre l'application Kafka Streams
     */
    public void start() {
        if (streams == null) {
            throw new IllegalStateException("La topologie doit être construite avant de démarrer");
        }

        streams.start();
        logger.info("Application Kafka Streams démarrée");

        // Hook pour un arrêt propre
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Arrêt de l'application Kafka Streams...");
            streams.close();
        }));
    }

    /**
     * Retourne l'instance KafkaStreams pour accéder aux stores
     */
    public KafkaStreams getStreams() {
        return streams;
    }

    public void close() {
        if (streams != null) {
            streams.close();
        }
    }
}
