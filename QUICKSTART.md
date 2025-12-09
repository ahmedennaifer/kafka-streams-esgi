# 🚀 Guide de démarrage rapide

## Pour les étudiants - Démarrage en 5 minutes

### 1. Démarrer Kafka avec Docker

```bash
# Démarrer Kafka en mode KRaft (sans ZooKeeper)
docker-compose up -d

# Vérifier que Kafka est démarré
docker logs kafka-video-streaming

# Attendre que le message "Kafka Server started" apparaisse (~30 secondes)
```

💡 **Kafka UI** est également disponible sur http://localhost:8080 pour visualiser vos topics et messages.

### 2. Créer les topics

```bash
# Rendre le script exécutable (première fois seulement)
chmod +x create-topics.sh

# Créer les topics
./create-topics.sh
```

Vous devriez voir:
- ✅ `video-views` (3 partitions)
- ✅ `video-metadata` (1 partition, compacted)
- ✅ `user-subscriptions` (1 partition, compacted)

### 3. Compiler le projet

```bash
mvn clean package
```

### 4. Initialiser les données de référence

**Terminal 1** - Catalogue de vidéos (une seule fois):
```bash
mvn exec:java -Dexec.mainClass="com.streaming.producer.VideoMetadataProducer"
```

Attendez le message "Catalogue de vidéos envoyé avec succès !" puis fermez (Ctrl+C).

**Terminal 2** - Abonnements utilisateurs (une seule fois):
```bash
mvn exec:java -Dexec.mainClass="com.streaming.producer.UserSubscriptionProducer"
```

Attendez le message "Abonnements utilisateurs envoyés avec succès !" puis fermez (Ctrl+C).

### 5. Lancer votre application Streams

**Terminal 3** - Application principale:
```bash
mvn exec:java -Dexec.mainClass="com.streaming.VideoStreamingApp"
```

Attendez que l'API soit prête (~5 secondes). Vous verrez:
```
Application démarrée avec succès !
API REST: http://localhost:7000
```

⚠️ **Important**: Ne fermez pas ce terminal !

### 6. Générer les événements de visionnage

**Terminal 4** - Générateur d'événements (continu):
```bash
mvn exec:java -Dexec.mainClass="com.streaming.producer.VideoViewProducer"
```

Vous verrez des messages comme "Envoyé 100 événements de visionnage".

⚠️ **Important**: Laissez ce terminal ouvert pendant vos tests !

### 7. Tester l'API

Ouvrez votre navigateur sur http://localhost:7000

Ou utilisez curl:
```bash
# Page d'accueil avec la doc
curl http://localhost:7000/

# Vues d'une vidéo Action populaire
curl http://localhost:7000/videos/video-action-1/views

# Stats par genre
curl http://localhost:7000/stats/by-genre

# Vidéos trending
curl http://localhost:7000/trending
```

## 🎯 Votre mission

### Phase 1: Implémenter la topologie (3h)

Ouvrez `src/main/java/com/streaming/streams/VideoStreamingTopology.java`

Complétez les TODO dans l'ordre:
1. ✅ Créer les KTables (metadata et subscriptions)
2. ✅ Créer le KStream de vues
3. ✅ Implémenter les agrégations
4. ✅ Implémenter les joins
5. ✅ Implémenter le windowing

**Conseil**: Testez après chaque TODO ! Redémarrez l'application et vérifiez les logs.

### Phase 2: Implémenter l'API (2h)

Ouvrez `src/main/java/com/streaming/api/VideoStreamingApi.java`

Complétez les handlers dans l'ordre:
1. ✅ `getVideoViews()` - Le plus simple pour commencer
2. ✅ `getVideoWatchTime()`
3. ✅ `getVideoMetadata()`
4. ✅ `getUserWatchTime()`
5. ✅ `getStatsByGenre()` - Nécessite une itération
6. ✅ `getStatsBySubscription()`
7. ✅ `getTrendingVideos()` - Le plus complexe (window store)
8. ✅ `getAllVideos()` - Bonus

**Conseil**: Testez chaque endpoint dès qu'il est implémenté !

### Phase 3: Validation (1h)

1. Vérifiez que tous les endpoints retournent des données cohérentes
2. Observez l'évolution des compteurs en temps réel
3. Testez avec différentes vidéos (video-action-1, video-comedy-1, etc.)
4. Vérifiez que les trending videos changent toutes les 5 minutes

## 🧪 Vérifications rapides

### Les données arrivent-elles dans Kafka ?

```bash
# Voir les messages du topic video-views
docker exec -it kafka-video-streaming kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic video-views \
  --from-beginning \
  --max-messages 5
```

### Mon application Streams fonctionne-t-elle ?

Regardez les logs dans le Terminal 3. Vous devriez voir:
- Pas d'erreurs ou d'exceptions
- Messages "Processing record..." (si vous avez décommenté les logs de debug)

### Mes State Stores sont-ils créés ?

```bash
# Liste des consumer groups (votre app devrait apparaître)
docker exec kafka-video-streaming kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --list
```

Vous devriez voir `video-streaming-app`.

## 🐛 Problèmes courants

### "Store not available"
- Attendez quelques secondes après le démarrage
- Vérifiez que les topics existent
- Vérifiez que le VideoViewProducer tourne

### Les compteurs restent à 0
- Vérifiez que le VideoViewProducer est bien lancé
- Vérifiez dans Kafka UI (http://localhost:8080) que les messages arrivent
- Regardez les logs de votre application pour les exceptions

### "Cannot resolve key" ou problèmes de join
- Vérifiez que les clés correspondent (videoId dans les deux côtés du join)
- Pour le join avec subscriptions, il faut re-keyer par userId
- Vérifiez les logs pour les warnings de co-partitioning

### L'API retourne "error: Non implémenté"
- C'est normal ! C'est à vous de l'implémenter 😊
- Commencez par le plus simple: `getVideoViews()`

## 📊 Résultats attendus après 2 minutes

Avec 5 événements/seconde:
- ~600 vues générées
- `video-action-1` devrait avoir ~50-80 vues (c'est une vidéo populaire)
- Genre "Action" devrait dominer avec ~200 vues
- Le temps de visionnage total devrait être ~300,000 secondes (~83 heures)

## 🎓 Conseils pédagogiques

1. **Commencez simple**: Implémentez d'abord les agrégations de base
2. **Testez souvent**: Redémarrez l'app après chaque modification importante
3. **Utilisez les logs**: Ajoutez des `.peek()` ou `.foreach()` pour débugger
4. **Lisez les erreurs**: Les messages d'erreur Kafka Streams sont très informatifs
5. **Consultez la doc**: Le README contient tous les indices nécessaires

## 🛑 Pour arrêter

```bash
# Arrêter les producteurs (Ctrl+C dans leurs terminaux)

# Arrêter l'application Streams (Ctrl+C dans son terminal)

# Arrêter Kafka
docker-compose down

# Pour tout nettoyer (données comprises)
docker-compose down -v
```

## ✨ Bon travail !

Vous êtes maintenant prêt à construire votre pipeline d'analytics en temps réel ! 🚀
