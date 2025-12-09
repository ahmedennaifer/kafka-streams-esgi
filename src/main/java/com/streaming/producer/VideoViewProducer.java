package com.streaming.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streaming.config.KafkaConfig;
import com.streaming.model.VideoView;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Producteur qui génère en continu des événements de visionnage de vidéos
 * Simule le comportement réaliste d'une plateforme de streaming
 */
public class VideoViewProducer {
    private static final Logger logger = LoggerFactory.getLogger(VideoViewProducer.class);
    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;
    private final Random random;
    private final AtomicBoolean running;
    
    private static final String[] GENRES = {"Action", "Comedy", "Drama", "SciFi", "Horror", "Documentary"};
    private static final int VIDEO_COUNT_PER_GENRE = 10;
    private static final int USER_COUNT = 100;

    public VideoViewProducer() {
        this.producer = new KafkaProducer<>(KafkaConfig.getProducerConfig());
        this.objectMapper = new ObjectMapper();
        this.random = new Random();
        this.running = new AtomicBoolean(true);
    }

    /**
     * Génère des événements de visionnage en continu
     * @param eventsPerSecond nombre d'événements à générer par seconde
     */
    public void generateViews(double eventsPerSecond) {
        logger.info("Démarrage de la génération de vues ({} événements/seconde)...", eventsPerSecond);
        
        long delayMs = (long) (1000 / eventsPerSecond);
        int eventCount = 0;
        
        while (running.get()) {
            VideoView view = generateRandomView();
            
            try {
                String json = objectMapper.writeValueAsString(view);
                ProducerRecord<String, String> record = new ProducerRecord<>(
                    KafkaConfig.VIDEO_VIEWS_TOPIC,
                    view.getVideoId(),  // Clé = videoId pour permettre le partitionnement
                    json
                );
                
                producer.send(record, (recordMetadata, exception) -> {
                    if (exception != null) {
                        logger.error("Erreur lors de l'envoi de la vue", exception);
                    }
                });
                
                eventCount++;
                if (eventCount % 100 == 0) {
                    logger.info("Envoyé {} événements de visionnage", eventCount);
                }
                
                Thread.sleep(delayMs);
                
            } catch (InterruptedException e) {
                logger.info("Producteur interrompu");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Erreur lors de la génération de vue", e);
            }
        }
        
        producer.flush();
        logger.info("Génération de vues terminée. Total: {} événements", eventCount);
    }

    /**
     * Génère un événement de visionnage aléatoire mais réaliste
     * - Distribution non uniforme des vidéos (certaines plus populaires)
     * - Durées de visionnage variées
     */
    private VideoView generateRandomView() {
        String userId = "user-" + (random.nextInt(USER_COUNT) + 1);
        
        // Distribution biaisée: certaines vidéos sont plus populaires
        String genre = selectPopularGenre();
        int videoNumber;
        if (random.nextDouble() < 0.3) {
            // 30% du temps, on sélectionne les vidéos populaires (1-3)
            videoNumber = random.nextInt(3) + 1;
        } else {
            // 70% du temps, distribution normale
            videoNumber = random.nextInt(VIDEO_COUNT_PER_GENRE) + 1;
        }
        
        String videoId = "video-" + genre.toLowerCase() + "-" + videoNumber;
        
        // Durée de visionnage: entre 30 secondes et 2 heures
        // Distribution gaussienne centrée sur 30 minutes
        int durationSeconds = (int) Math.max(30, Math.min(7200, 
            1800 + random.nextGaussian() * 1200));
        
        return new VideoView(userId, videoId, durationSeconds, genre);
    }

    /**
     * Sélectionne un genre avec une distribution biaisée
     * Action et Comedy sont plus populaires
     */
    private String selectPopularGenre() {
        int value = random.nextInt(100);
        if (value < 30) return "Action";
        if (value < 55) return "Comedy";
        if (value < 70) return "Drama";
        if (value < 80) return "SciFi";
        if (value < 90) return "Horror";
        return "Documentary";
    }

    public void stop() {
        running.set(false);
    }

    public void close() {
        producer.close();
    }

    public static void main(String[] args) {
        VideoViewProducer producer = new VideoViewProducer();
        
        // Génère 5 événements par seconde par défaut
        double eventsPerSecond = 5.0;
        if (args.length > 0) {
            try {
                eventsPerSecond = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                logger.warn("Argument invalide, utilisation de {} événements/seconde", eventsPerSecond);
            }
        }
        
        // Gestion de l'arrêt propre
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Arrêt du producteur...");
            producer.stop();
            producer.close();
        }));
        
        producer.generateViews(eventsPerSecond);
    }
}
