package com.streaming.streams;

import com.streaming.config.KafkaConfig;
import com.streaming.model.*;
import com.streaming.utils.JsonSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
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
   */
  public void buildTopology() {
    StreamsBuilder builder = new StreamsBuilder();

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
    // Indice: utiliser builder.table() avec le bon Serde
    // Nom du store: "video-metadata-store"
    KTable<String, VideoMetadata> videoMetadataTable = builder.table(
        KafkaConfig.VIDEO_METADATA_TOPIC,
        Consumed.with(Serdes.String(), videoMetadataSerde),
        Materialized.as("video-metadata-store"));

    // TODO 1.2: Créer une KTable à partir du topic user-subscriptions
    // Nom du store: "user-subscriptions-store"
    KTable<String, UserSubscription> userSubscriptionsTable = builder.table(
        KafkaConfig.USER_SUBSCRIPTIONS_TOPIC,
        Consumed.with(Serdes.String(), userSubscriptionSerde),
        Materialized.as("user-subscriptions-store"));
 
    // ==============================================
    // TODO 2: LIRE LE STREAM DE VUES
    // ==============================================

    // TODO 2.1: Créer un KStream à partir du topic video-views
    KStream<String, VideoView> videoViewsStream = builder.stream(
        KafkaConfig.VIDEO_VIEWS_TOPIC,
        Consumed.with(Serdes.String(), videoViewSerde));

    // ==============================================
    // TODO 3: AGRÉGATIONS
    // ==============================================

    // TODO 3.1: Compter le nombre de vues par vidéo
    // Indice: groupByKey() puis count()
    // Nom du store: "video-view-counts"
    KTable<String, Long> videoViewCounts = videoViewsStream
        .groupByKey(Grouped.with(Serdes.String(), videoViewSerde))
        .count(Materialized.as("video-view-counts"));

    // TODO 3.2: Calculer le temps de visionnage total par vidéo
    // Indice: groupByKey() puis aggregate()
    // Vous devez sommer les durationSeconds de chaque vue
    // Nom du store: "video-watch-time"
    KTable<String, Long> videoWatchTime = videoViewsStream
        .groupByKey(Grouped.with(Serdes.String(), videoViewSerde))
        .aggregate(
            () -> 0L, // we need long because values can exceed 2B (cant use int)
            (key, view, aggregate) -> aggregate + view.getDurationSeconds(),
            Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("video-watch-time") // need to typecast to
                                                                                            // generic;
                                                                                            // serde cannot serde long
                                                                                            // natively
                .withKeySerde(Serdes.String())
                .withValueSerde(Serdes.Long()));

    // TODO 3.3: Calculer le nombre de vues par genre
    // Indice: selectKey() pour changer la clé vers le genre, puis groupByKey() et
    // count()
    // Nom du store: "views-by-genre"
    KTable<String, Long> viewsByGenre = videoViewsStream
        .selectKey((key, value) -> value.getGenre())
        .groupByKey(Grouped.with(Serdes.String(), videoViewSerde))
        .count(Materialized.as("views-by-genre"));

    // TODO 3.4: Calculer le temps de visionnage total par utilisateur
    // Indice: selectKey() pour changer la clé vers userId, puis aggregate()
    // Nom du store: "user-watch-time"
    KTable<String, Long> userWatchTime = videoViewsStream
        .selectKey((key, value) -> value.getUserId())
        .groupByKey(Grouped.with(Serdes.String(), videoViewSerde))
        .aggregate(
            () -> 0L,
            (key, view, aggregate) -> aggregate + view.getDurationSeconds(),
            Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("user-watch-time")
                .withKeySerde(Serdes.String())
                .withValueSerde(Serdes.Long()));

    // ==============================================
    // TODO 4: JOIN STREAM-TABLE
    // ==============================================

    // TODO 4.1: Enrichir chaque vue avec les métadonnées de la vidéo
    // Indice: join() entre videoViewsStream et videoMetadataTable
    // La clé doit être le videoId
    KStream<String, EnrichedVideoView> enrichedViews = videoViewsStream
        .join(
            videoMetadataTable,
            (view, metadata) -> new EnrichedVideoView(view, metadata, null),
            Joined.with(Serdes.String(), videoViewSerde, videoMetadataSerde));

    // ==============================================
    // TODO 5: JOIN STREAM-TABLE (enrichissement avec abonnement)
    // ==============================================

    // TODO 5.1: Enrichir les vues avec l'abonnement de l'utilisateur
    // Problème: la clé actuelle est videoId, mais on a besoin de userId pour le
    // join
    // Indice: selectKey() pour changer la clé vers userId, puis leftJoin()
    var enrichedVideoViewSerde = new JsonSerde<>(EnrichedVideoView.class);
     KStream<String, EnrichedVideoView> fullyEnrichedViews = enrichedViews
        .selectKey((key, value) -> value.getUserId())
        .filter((key, value) -> value != null)
        .leftJoin(
            userSubscriptionsTable,
            (videoView, userSubscription) -> {
              var subCass = userSubscription.getClass();
              videoView.setSubscriptionTier(userSubscription.getClass().getCanonicalName());
              return videoView;
            },
            Joined.with(
                Serdes.String(),
                enrichedVideoViewSerde,
                userSubscriptionSerde));

    // TODO 5.2: Calculer les statistiques par type d'abonnement
    // Combien de vues pour chaque tier (FREE, PREMIUM, VIP) ?
    // Indice: selectKey() vers subscriptionTier, groupByKey(), count()
    // Nom du store: "views-by-subscription"
    KTable<String, Long> viewsBySubscription = fullyEnrichedViews
        .selectKey((key, value) -> value.getSubscriptionTier()) 
        .groupByKey(Grouped.with(Serdes.String(), enrichedVideoViewSerde))
        .count(Materialized.as("views-by-subscription"));
    // ==============================================
    // TODO 6: WINDOWING - TRENDING VIDEOS
    // ==============================================

    // TODO 6.1: Calculer les vidéos trending sur une fenêtre glissante de 5 minutes
    // Indice:
    // - Repartir du videoViewsStream
    // - groupByKey()
    // - windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
    // - count()
    // Nom du store: "trending-videos"
   KTable<Windowed<String>, Long> trendingVideos = videoViewsStream
        .groupByKey(Grouped.with(Serdes.String(), videoViewSerde))
        .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
        .count(Materialized.as("trending-videos"));
    // ==================================== // LOGS POUR DEBUG (optionnel)
    // ==============================================

    // Décommenter pour voir les événements passer
    // if (videoViewsStream != null) {
    // videoViewsStream.foreach((key, value) ->
    // logger.info("Vue reçue: {} - {}", key, value));
    // }

    // if (enrichedViews != null) {
    // enrichedViews.foreach((key, value) ->
    // logger.info("Vue enrichie: {}", value));
    // }

    // Construction finale de la topologie
    streams = new KafkaStreams(builder.build(), KafkaConfig.getStreamsConfig("video-streaming-app"));

    logger.info("Topologie Kafka Streams construite avec succès");
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
