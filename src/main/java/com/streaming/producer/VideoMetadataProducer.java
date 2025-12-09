package com.streaming.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streaming.config.KafkaConfig;
import com.streaming.model.VideoMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Producteur qui génère des métadonnées de vidéos
 * À exécuter une fois au démarrage pour initialiser le catalogue
 */
public class VideoMetadataProducer {
    private static final Logger logger = LoggerFactory.getLogger(VideoMetadataProducer.class);
    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;
    
    private static final String[] GENRES = {"Action", "Comedy", "Drama", "SciFi", "Horror", "Documentary"};
    
    private static final String[][] TITLES = {
        // Action
        {"Speed Warriors", "Night Hunter", "Last Stand", "Iron Fist", "Storm Breaker"},
        // Comedy
        {"Laugh Factory", "Office Mayhem", "Love & Chaos", "The Roommates", "Wedding Disaster"},
        // Drama
        {"Broken Dreams", "Silent Tears", "The Journey", "Lost Memories", "Family Ties"},
        // SciFi
        {"Mars Colony", "AI Uprising", "Time Paradox", "Cyber Future", "Alien Contact"},
        // Horror
        {"Dark Woods", "The Curse", "Midnight Terror", "Ghost House", "Evil Rising"},
        // Documentary
        {"Nature's Wonders", "History Uncovered", "Tech Revolution", "Ocean Mysteries", "Space Exploration"}
    };

    public VideoMetadataProducer() {
        this.producer = new KafkaProducer<>(KafkaConfig.getProducerConfig());
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Génère et envoie un catalogue de vidéos
     */
    public void generateCatalog(int videosPerGenre) {
        List<VideoMetadata> catalog = new ArrayList<>();
        
        for (int genreIndex = 0; genreIndex < GENRES.length; genreIndex++) {
            String genre = GENRES[genreIndex];
            String[] titlesForGenre = TITLES[genreIndex];
            
            for (int i = 0; i < videosPerGenre; i++) {
                String title = titlesForGenre[i % titlesForGenre.length];
                if (i >= titlesForGenre.length) {
                    title += " " + (i / titlesForGenre.length + 1);
                }
                
                VideoMetadata metadata = new VideoMetadata(
                    "video-" + genre.toLowerCase() + "-" + (i + 1),
                    title,
                    genre,
                    2015 + (i % 10),
                    60 + (i * 15) % 120  // Durée entre 60 et 180 minutes
                );
                
                catalog.add(metadata);
            }
        }
        
        logger.info("Envoi de {} vidéos au catalogue...", catalog.size());
        
        for (VideoMetadata metadata : catalog) {
            try {
                String json = objectMapper.writeValueAsString(metadata);
                ProducerRecord<String, String> record = new ProducerRecord<>(
                    KafkaConfig.VIDEO_METADATA_TOPIC,
                    metadata.getVideoId(),
                    json
                );
                
                producer.send(record, (recordMetadata, exception) -> {
                    if (exception != null) {
                        logger.error("Erreur lors de l'envoi de la métadonnée: {}", metadata.getVideoId(), exception);
                    }
                });
                
            } catch (Exception e) {
                logger.error("Erreur de sérialisation pour: {}", metadata.getVideoId(), e);
            }
        }
        
        producer.flush();
        logger.info("Catalogue de vidéos envoyé avec succès !");
    }

    public void close() {
        producer.close();
    }

    public static void main(String[] args) {
        VideoMetadataProducer producer = new VideoMetadataProducer();
        
        // Génère 10 vidéos par genre (60 vidéos au total)
        producer.generateCatalog(10);
        
        producer.close();
        logger.info("Producteur de métadonnées terminé.");
    }
}
