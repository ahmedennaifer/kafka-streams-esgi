package com.streaming;

import com.streaming.api.VideoStreamingApi;
import com.streaming.streams.VideoStreamingTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe principale de l'application Video Streaming Analytics
 * 
 * Cette application Kafka Streams analyse en temps réel les événements
 * de visionnage d'une plateforme de streaming vidéo.
 * 
 * POUR LES ÉTUDIANTS:
 * 1. Complétez la topologie dans VideoStreamingTopology.java
 * 2. Complétez l'API REST dans VideoStreamingApi.java
 * 3. Lancez cette application
 * 4. Utilisez les producteurs pour générer des données
 * 5. Testez vos implémentations via l'API REST
 */
public class VideoStreamingApp {
  private static final Logger logger = LoggerFactory.getLogger(VideoStreamingApp.class);

  public static void main(String[] args) {
    logger.info("==============================================");
    logger.info("  Video Streaming Analytics - Kafka Streams");
    logger.info("==============================================");

    try {
      // Créer et construire la topologie
      logger.info("Construction de la topologie Kafka Streams...");
      VideoStreamingTopology topology = new VideoStreamingTopology();
      topology.buildTopology();

      // Démarrer l'application Streams
      logger.info("Démarrage de l'application Kafka Streams...");
      topology.start();

      // Attendre que l'application soit prête (state stores disponibles)
      logger.info("Attente de l'initialisation des state stores...");
      Thread.sleep(5000);

      // Démarrer l'API REST
      logger.info("Démarrage de l'API REST...");
      VideoStreamingApi api = new VideoStreamingApi(topology.getStreams(), 7001);

      logger.info("==============================================");
      logger.info("  Application démarrée avec succès !");
      logger.info("  API REST: http://localhost:7001");
      logger.info("==============================================");
      logger.info("");
      logger.info("Pour générer des données:");
      logger.info("  1. Lancez VideoMetadataProducer (une seule fois)");
      logger.info("  2. Lancez UserSubscriptionProducer (une seule fois)");
      logger.info("  3. Lancez VideoViewProducer (continu)");
      logger.info("");
      logger.info("Consultez l'API sur: http://localhost:7001");
      logger.info("");
      logger.info("Appuyez sur Ctrl+C pour arrêter l'application");

      // Garder l'application en vie
      Thread.currentThread().join();

    } catch (Exception e) {
      logger.error("Erreur lors du démarrage de l'application", e);
      System.exit(1);
    }
  }
}
