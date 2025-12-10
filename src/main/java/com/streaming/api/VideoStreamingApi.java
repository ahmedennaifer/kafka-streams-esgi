package com.streaming.api;

import com.streaming.model.VideoMetadata;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.apache.kafka.streams.kstream.Windowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * API REST pour exposer les State Stores de Kafka Streams
 * 
 * OBJECTIFS:
 * - Permettre d'interroger les stores créés dans la topologie
 * - Valider visuellement que les agrégations fonctionnent correctement
 * 
 * À COMPLÉTER PAR LES ÉTUDIANTS
 */
public class VideoStreamingApi {
  private static final Logger logger = LoggerFactory.getLogger(VideoStreamingApi.class);

  private final KafkaStreams streams;
  private final Javalin app;

  public VideoStreamingApi(KafkaStreams streams, int port) {
    this.streams = streams;
    this.app = Javalin.create(config -> {
      config.showJavalinBanner = false;
    });

    setupRoutes();
    app.start(port);
    logger.info("API REST démarrée sur le port {}", port);
  }

  /**
   * Configure les routes de l'API
   */
  private void setupRoutes() {
    // Page d'accueil avec la documentation
    app.get("/", this::handleHome);

    // ==============================================
    // TODO: COMPLÉTER LES ENDPOINTS
    // ==============================================

    // TODO 1: Récupérer les statistiques d'une vidéo
    // GET /videos/{videoId}/views
    // Doit retourner le nombre de vues depuis le store "video-view-counts"
    app.get("/videos/{videoId}/views", this::getVideoViews);

    // TODO 2: Récupérer le temps de visionnage d'une vidéo
    // GET /videos/{videoId}/watch-time
    // Doit retourner le temps total de visionnage depuis le store
    // "video-watch-time"
    app.get("/videos/{videoId}/watch-time", this::getVideoWatchTime);

    // TODO 3: Récupérer les métadonnées d'une vidéo
    // GET /videos/{videoId}/metadata
    // Doit retourner les métadonnées depuis le store "video-metadata-store"
    app.get("/videos/{videoId}/metadata", this::getVideoMetadata);

    // TODO 4: Récupérer le temps de visionnage d'un utilisateur
    // GET /users/{userId}/watch-time
    // Doit retourner le temps total de visionnage de l'utilisateur
    app.get("/users/{userId}/watch-time", this::getUserWatchTime);

    // TODO 5: Récupérer les statistiques par genre
    // GET /stats/by-genre
    // Doit retourner toutes les statistiques du store "views-by-genre"
    app.get("/stats/by-genre", this::getStatsByGenre); 

    // TODO 6: Récupérer les statistiques par type d'abonnement
    // GET /stats/by-subscription
    // Doit retourner toutes les statistiques du store "views-by-subscription"
    app.get("/stats/by-subscription", this::getStatsBySubscription);

    // TODO 7: Récupérer les vidéos trending
    // GET /trending
    // Doit retourner le top 10 des vidéos depuis le store "trending-videos"
    app.get("/trending", this::getTrendingVideos);

    // TODO 8 (BONUS): Récupérer toutes les vidéos avec leurs stats
    // GET /videos/all
    // Doit combiner les informations de plusieurs stores
    app.get("/videos/all", this::getAllVideos);
  }

  /**
   * Page d'accueil avec la documentation de l'API
   */
  private void handleHome(Context ctx) {
    String html = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Video Streaming Analytics API</title>
            <style>
                body { font-family: Arial, sans-serif; max-width: 1000px; margin: 50px auto; padding: 20px; }
                h1 { color: #333; }
                .endpoint { background: #f5f5f5; padding: 15px; margin: 10px 0; border-radius: 5px; }
                .method { color: #fff; padding: 5px 10px; border-radius: 3px; font-weight: bold; }
                .get { background: #61affe; }
                code { background: #eee; padding: 2px 6px; border-radius: 3px; }
                a { color: #61affe; text-decoration: none; }
                a:hover { text-decoration: underline; }
            </style>
        </head>
        <body>
            <h1>🎬 Video Streaming Analytics API</h1>
            <p>API pour interroger les statistiques de la plateforme de streaming</p>

            <h2>Endpoints disponibles</h2>

            <div class="endpoint">
                <span class="method get">GET</span> <code>/videos/{videoId}/views</code>
                <p>Récupère le nombre total de vues d'une vidéo</p>
                <p>Exemple: <a href="/videos/video-action-1/views" target="_blank">/videos/video-action-1/views</a></p>
            </div>

            <div class="endpoint">
                <span class="method get">GET</span> <code>/videos/{videoId}/watch-time</code>
                <p>Récupère le temps total de visionnage d'une vidéo (en secondes)</p>
                <p>Exemple: <a href="/videos/video-action-1/watch-time" target="_blank">/videos/video-action-1/watch-time</a></p>
            </div>

            <div class="endpoint">
                <span class="method get">GET</span> <code>/videos/{videoId}/metadata</code>
                <p>Récupère les métadonnées d'une vidéo</p>
                <p>Exemple: <a href="/videos/video-action-1/metadata" target="_blank">/videos/video-action-1/metadata</a></p>
            </div>

            <div class="endpoint">
                <span class="method get">GET</span> <code>/users/{userId}/watch-time</code>
                <p>Récupère le temps total de visionnage d'un utilisateur (en secondes)</p>
                <p>Exemple: <a href="/users/user-1/watch-time" target="_blank">/users/user-1/watch-time</a></p>
            </div>

            <div class="endpoint">
                <span class="method get">GET</span> <code>/stats/by-genre</code>
                <p>Récupère les statistiques de vues par genre</p>
                <p>Exemple: <a href="/stats/by-genre" target="_blank">/stats/by-genre</a></p>
            </div>

            <div class="endpoint">
                <span class="method get">GET</span> <code>/stats/by-subscription</code>
                <p>Récupère les statistiques de vues par type d'abonnement</p>
                <p>Exemple: <a href="/stats/by-subscription" target="_blank">/stats/by-subscription</a></p>
            </div>

            <div class="endpoint">
                <span class="method get">GET</span> <code>/trending</code>
                <p>Récupère les vidéos trending (5 dernières minutes)</p>
                <p>Exemple: <a href="/trending" target="_blank">/trending</a></p>
            </div>

            <div class="endpoint">
                <span class="method get">GET</span> <code>/videos/all</code>
                <p>Liste toutes les vidéos avec leurs statistiques</p>
                <p>Exemple: <a href="/videos/all" target="_blank">/videos/all</a></p>
            </div>
        </body>
        </html>
        """;

    ctx.html(html);
  }

  // ==============================================
  // HANDLERS À COMPLÉTER PAR LES ÉTUDIANTS
  // ==============================================

  /**
   * TODO 1: Récupérer le nombre de vues d'une vidéo
   * 
   * Étapes:
   * 1. Récupérer le videoId depuis le path parameter
   * 2. Obtenir le store "video-view-counts" avec getKeyValueStore()
   * 3. Récupérer la valeur avec store.get(videoId)
   * 4. Retourner un JSON avec {videoId, viewCount}
   */
  private void getVideoViews(Context ctx) {
    String videoId = ctx.pathParam("videoId");

    try {
      ReadOnlyKeyValueStore<String, Long> store = getKeyValueStore("video-view-counts");
      Long viewCount = store.get(videoId);

      if (viewCount == null) {
        ctx.status(404).json(Map.of(
            "error", "video not found or no views recorded?",
            "videoId", videoId));
        return;
      }

      ctx.json(Map.of(
          "videoId", videoId,
          "viewCount", viewCount));

    } catch (Exception e) {
      logger.error("error querying store", e);
      ctx.status(500).json(Map.of(
          "error", "error",
          "message", e.getMessage()));
    }
  }

  /**
   * TODO 2: Récupérer le temps de visionnage d'une vidéo
   */
  private void getVideoWatchTime(Context ctx) {
    String videoId = ctx.pathParam("videoId");

    try {
      ReadOnlyKeyValueStore<String, Long> store = getKeyValueStore("video-watch-time");
      Long watchTime = store.get(videoId);

      if (watchTime == null) {
        ctx.status(404).json(Map.of(
            "error", "video not found",
            "videoId", videoId));
        return;
      }

      ctx.json(Map.of(
          "videoId", videoId,
          "watchTimeSeconds", watchTime,
          "watchTimeMinutes", watchTime / 60.0));

    } catch (Exception e) {
      logger.error("error querying store", e);
      ctx.status(500).json(Map.of(
          "error", "error",
          "message", e.getMessage()));
    }
  }

  /**
   * TODO 3: Récupérer les métadonnées d'une vidéo
   */
  private void getVideoMetadata(Context ctx) {
    String videoId = ctx.pathParam("videoId");

    try {
      ReadOnlyKeyValueStore<String, VideoMetadata> store = getKeyValueStore("video-metadata-store");
      VideoMetadata metadata = store.get(videoId);

      if (metadata == null) {
        ctx.status(404).json(Map.of(
            "error", "video not found",
            "videoId", videoId));
        return;
      }

      ctx.json(metadata);

    } catch (Exception e) {
      logger.error("error querying store", e);
      ctx.status(500).json(Map.of(
          "error", "error",
          "message", e.getMessage()));
    }
  }
  /**
   * TODO 4: Récupérer le temps de visionnage d'un utilisateur
   */
  private void getUserWatchTime(Context ctx) {
    String userId = ctx.pathParam("userId");

    try {
      ReadOnlyKeyValueStore<String, Long> store = getKeyValueStore("user-watch-time");
      Long watchTime = store.get(userId);

      if (watchTime == null) {
        ctx.status(404).json(Map.of(
            "error", "user not found",
            "userId", userId));
        return;
      }

      ctx.json(Map.of(
          "userId", userId,
          "totalWatchTimeSeconds", watchTime,
          "totalWatchTimeHours", String.format("%.2f", watchTime / 3600.0)));

    } catch (Exception e) {
      logger.error("error querying store", e);
      ctx.status(500).json(Map.of(
          "error", "error",
          "message", e.getMessage()));
    }
  }

  /**
   * TODO 5: Récupérer les statistiques par genre
   * 
   * Indice: Utiliser store.all() pour récupérer toutes les entrées
   * puis itérer avec un while(iterator.hasNext())
   */
  private void getStatsByGenre(Context ctx) {
  try {
    ReadOnlyKeyValueStore<String, Long> store = getKeyValueStore("views-by-genre");
    
    Map<String, Long> statsByGenre = new HashMap<>();
    
    KeyValueIterator<String, Long> iterator = store.all();
    
    while (iterator.hasNext()) {
      KeyValue<String, Long> entry = iterator.next();
      statsByGenre.put(entry.key, entry.value);
    }
    
    iterator.close();
    
    ctx.json(Map.of(
        "statsByGenre", statsByGenre
        // "totalViews", totalViews
    ));
    
  } catch (Exception e) {
    logger.error("Erreur lors de la récupération des stats par genre", e);
    ctx.status(500).json(Map.of(
        "error", "Erreur serveur",
        "message", e.getMessage()
    ));
  }
}

  /**
   * TODO 6: Récupérer les statistiques par type d'abonnement
   */
  private void getStatsBySubscription(Context ctx) {
  try {
    ReadOnlyKeyValueStore<String, Long> store = getKeyValueStore("views-by-subscription");
    
    Map<String, Long> statsBySubscription = new HashMap<>();
    
    KeyValueIterator<String, Long> iterator = store.all();
    
    while (iterator.hasNext()) {
      KeyValue<String, Long> entry = iterator.next();
      statsBySubscription.put(entry.key, entry.value);
    }
    
    iterator.close();
    
    long totalViews = statsBySubscription.values().stream()
        .mapToLong(Long::longValue)
        .sum();
    
    ctx.json(Map.of(
        "statsBySubscription", statsBySubscription,
        "totalViews", totalViews
    ));
    
  } catch (Exception e) {
    logger.error("Erreur lors de la récupération des stats par abonnement", e);
    ctx.status(500).json(Map.of(
        "error", "Erreur serveur",
        "message", e.getMessage()
    ));
  }
}

  /**
   * TODO 7: Récupérer les vidéos trending
   * 
   * Indice:
   * - Utiliser getWindowStore("trending-videos")
   * - Utiliser store.fetchAll() pour récupérer toutes les fenêtres
   * - Pour chaque fenêtre, récupérer la clé (videoId) et la valeur (count)
   * - Trier par count décroissant et prendre le top 10
   */
  private void getTrendingVideos(Context ctx) {
    // VOTRE CODE ICI

    ctx.json(Map.of("error", "Non implémenté"));
  }

  /**
   * TODO 8 (BONUS): Combiner plusieurs stores pour avoir une vue complète
   */
  private void getAllVideos(Context ctx) {
    // VOTRE CODE ICI

    ctx.json(Map.of("error", "Non implémenté"));
  }

  // ==============================================
  // MÉTHODES UTILITAIRES
  // ==============================================

  /**
   * Récupère un Key-Value Store par son nom
   */
  private <K, V> ReadOnlyKeyValueStore<K, V> getKeyValueStore(String storeName) {
    return streams.store(
        StoreQueryParameters.fromNameAndType(
            storeName,
            QueryableStoreTypes.keyValueStore()));
  }

  /**
   * Récupère un Window Store par son nom
   */
  private <K, V> ReadOnlyWindowStore<K, V> getWindowStore(String storeName) {
    return streams.store(
        StoreQueryParameters.fromNameAndType(
            storeName,
            QueryableStoreTypes.windowStore()));
  }

  /**
   * Arrête l'API
   */
  public void stop() {
    app.stop();
    logger.info("API REST arrêtée");
  }
}
