package com.streaming.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streaming.config.KafkaConfig;
import com.streaming.model.UserSubscription;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Producteur qui génère des abonnements d'utilisateurs
 * À exécuter une fois au démarrage pour initialiser les utilisateurs
 */
public class UserSubscriptionProducer {
    private static final Logger logger = LoggerFactory.getLogger(UserSubscriptionProducer.class);
    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;
    private final Random random;

    public UserSubscriptionProducer() {
        this.producer = new KafkaProducer<>(KafkaConfig.getProducerConfig());
        this.objectMapper = new ObjectMapper();
        this.random = new Random();
    }

    /**
     * Génère des abonnements pour un certain nombre d'utilisateurs
     * Distribution: 50% FREE, 30% PREMIUM, 20% VIP
     */
    public void generateSubscriptions(int numberOfUsers) {
        logger.info("Génération de {} abonnements utilisateurs...", numberOfUsers);
        
        for (int i = 1; i <= numberOfUsers; i++) {
            String userId = "user-" + i;
            UserSubscription.SubscriptionTier tier = selectTier();
            
            UserSubscription subscription = new UserSubscription(userId, tier);
            
            try {
                String json = objectMapper.writeValueAsString(subscription);
                ProducerRecord<String, String> record = new ProducerRecord<>(
                    KafkaConfig.USER_SUBSCRIPTIONS_TOPIC,
                    userId,
                    json
                );
                
                producer.send(record, (recordMetadata, exception) -> {
                    if (exception != null) {
                        logger.error("Erreur lors de l'envoi de l'abonnement: {}", userId, exception);
                    }
                });
                
            } catch (Exception e) {
                logger.error("Erreur de sérialisation pour: {}", userId, e);
            }
        }
        
        producer.flush();
        logger.info("Abonnements utilisateurs envoyés avec succès !");
    }

    private UserSubscription.SubscriptionTier selectTier() {
        int value = random.nextInt(100);
        if (value < 50) {
            return UserSubscription.SubscriptionTier.FREE;
        } else if (value < 80) {
            return UserSubscription.SubscriptionTier.PREMIUM;
        } else {
            return UserSubscription.SubscriptionTier.VIP;
        }
    }

    public void close() {
        producer.close();
    }

    public static void main(String[] args) {
        UserSubscriptionProducer producer = new UserSubscriptionProducer();
        
        // Génère 100 utilisateurs
        producer.generateSubscriptions(100);
        
        producer.close();
        logger.info("Producteur d'abonnements terminé.");
    }
}
