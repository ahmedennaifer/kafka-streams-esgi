# 🎬 Video Streaming Analytics - Projet Kafka Streams

Projet d'analyse en temps réel d'une plateforme de streaming vidéo avec Apache Kafka Streams.

## 📋 Contexte

Vous travaillez pour une plateforme de streaming vidéo similaire à Netflix ou YouTube. Votre mission est de construire un système d'analytics en temps réel pour:
- Compter les vues par vidéo et par genre
- Calculer le temps de visionnage par utilisateur
- Identifier les vidéos "trending" du moment
- Analyser le comportement selon le type d'abonnement (FREE, PREMIUM, VIP)

## 🎯 Objectifs pédagogiques

Ce projet vous permettra de maîtriser:
1. **KStream vs KTable**: Comprendre la différence entre événements et états
2. **Agrégations**: `count()`, `aggregate()`, `groupBy()`
3. **Joins**: Stream-Table join et Table-Table join
4. **Windowing**: Fenêtres temporelles glissantes
5. **State Stores**: Exposition des stores pour requêtes interactives

## 🏗️ Architecture

```
Topics Kafka:
- video-views (events)         → KStream de visionnage
- video-metadata (changelog)   → KTable de métadonnées
- user-subscriptions (changelog) → KTable d'abonnements

Kafka Streams:
- Agrégations (vues, temps de visionnage)
- Joins (enrichissement des vues)
- Windowing (trending videos)
- State Stores (pour l'API REST)

API REST:
- Interrogation des State Stores
- Visualisation des résultats
```

## 📦 Structure du projet

```
src/main/java/com/streaming/
├── model/              # Modèles de données (VideoView, VideoMetadata, etc.)
├── producer/           # Générateurs de données (DÉJÀ IMPLÉMENTÉS)
├── config/             # Configuration Kafka
├── utils/              # Utilitaires (JSON Serde)
├── streams/            # Topologie Kafka Streams (À COMPLÉTER)
├── api/                # API REST (À COMPLÉTER)
└── VideoStreamingApp.java  # Point d'entrée
```

## 🚀 Démarrage

### Prérequis

1. **Kafka local en cours d'exécution**:
```bash
# Avec KRaft (Kafka 3.x+)
bin/kafka-server-start.sh config/kraft/server.properties

# Ou avec Docker Compose (recommandé)
docker-compose up -d
```

2. **Java 17+** et **Maven**

### Compilation

```bash
mvn clean package
```

### Créer les topics

```bash
# Topic pour les vues (avec plusieurs partitions pour le parallélisme)
/opt/kafka/bin/kafka-topics.sh --create --topic video-views \
  --bootstrap-server localhost:9092 \
  --partitions 3 --replication-factor 1

# Topic pour les métadonnées (changelog, 1 partition suffit)
/opt/kafka/bin/kafka-topics.sh --create --topic video-metadata \
  --bootstrap-server localhost:9092 \
  --partitions 1 --replication-factor 1 \
  --config cleanup.policy=compact

# Topic pour les abonnements (changelog)
/opt/kafka/bin/kafka-topics.sh --create --topic user-subscriptions \
  --bootstrap-server localhost:9092 \
  --partitions 1 --replication-factor 1 \
  --config cleanup.policy=compact
```

### Initialiser les données

**1. Générer le catalogue de vidéos** (à lancer UNE SEULE FOIS):
```bash
mvn exec:java -Dexec.mainClass="com.streaming.producer.VideoMetadataProducer"
```

**2. Générer les abonnements utilisateurs** (à lancer UNE SEULE FOIS):
```bash
mvn exec:java -Dexec.mainClass="com.streaming.producer.UserSubscriptionProducer"
```

**3. Générer les événements de visionnage** (CONTINU - à laisser tourner):
```bash
# 5 événements par seconde par défaut
mvn exec:java -Dexec.mainClass="com.streaming.producer.VideoViewProducer"

# Ou spécifier le taux: 10 événements/seconde
mvn exec:java -Dexec.mainClass="com.streaming.producer.VideoViewProducer" -Dexec.args="10"
```

### Lancer l'application Streams

```bash
mvn exec:java -Dexec.mainClass="com.streaming.VideoStreamingApp"
```

Une fois lancée, l'API REST sera accessible sur: **http://localhost:7000**

## 📝 Travail à réaliser

### Partie 1: Topologie Kafka Streams (3h)

Fichier: `src/main/java/com/streaming/streams/VideoStreamingTopology.java`

#### TODO 1: Créer les KTables
- [X] KTable pour `video-metadata`
- [X] KTable pour `user-subscriptions`

#### TODO 2: Lire le stream de vues
- [X] KStream pour `video-views`

#### TODO 3: Agrégations
- [X] Compter les vues par vidéo
- [X] Calculer le temps de visionnage par vidéo
- [X] Compter les vues par genre
- [X] Calculer le temps de visionnage par utilisateur

#### TODO 4: Join Stream-Table
- [X] Enrichir les vues avec les métadonnées des vidéos

#### TODO 5: Changement de clé et nouveau join
- [ ] Re-keyer par `userId`
- [ ] Join avec la table d'abonnements
- [ ] Agréger par type d'abonnement

#### TODO 6: Windowing
- [ ] Fenêtre glissante de 5 minutes pour les trending videos

### Partie 2: API REST (2h)

Fichier: `src/main/java/com/streaming/api/VideoStreamingApi.java`

Implémentez les endpoints suivants en interrogeant les State Stores:

- [ ] `GET /videos/{videoId}/views` - Nombre de vues
- [ ] `GET /videos/{videoId}/watch-time` - Temps de visionnage
- [ ] `GET /videos/{videoId}/metadata` - Métadonnées
- [ ] `GET /users/{userId}/watch-time` - Temps de visionnage utilisateur
- [ ] `GET /stats/by-genre` - Stats par genre
- [ ] `GET /stats/by-subscription` - Stats par abonnement
- [ ] `GET /trending` - Top 10 trending videos
- [ ] `GET /videos/all` - Liste complète (bonus)

### Partie 3: Tests et validation (1h)

1. Vérifiez que les compteurs augmentent en temps réel
2. Testez les différents endpoints de l'API
3. Validez que les joins fonctionnent correctement
4. Observez le comportement des fenêtres temporelles

## 🧪 Exemples de requêtes API

```bash
# Vues d'une vidéo
curl http://localhost:7000/videos/video-action-1/views

# Stats par genre
curl http://localhost:7000/stats/by-genre

# Vidéos trending
curl http://localhost:7000/trending

# Toutes les vidéos
curl http://localhost:7000/videos/all
```

## 💡 Indices

### Création d'une KTable
```java
KTable<String, VideoMetadata> table = builder.table(
    TOPIC_NAME,
    Consumed.with(Serdes.String(), videoMetadataSerde),
    Materialized.as("store-name")
);
```

### Agrégation simple
```java
KTable<String, Long> counts = stream
    .groupByKey()
    .count(Materialized.as("store-name"));
```

### Agrégation avec accumulation
```java
KTable<String, Long> totals = stream
    .groupByKey()
    .aggregate(
        () -> 0L,  // Initializer
        (key, value, aggregate) -> aggregate + value.getDurationSeconds(),
        Materialized.as("store-name")
    );
```

### Join Stream-Table
```java
KStream<String, EnrichedView> enriched = stream.join(
    table,
    (view, metadata) -> new EnrichedView(view, metadata)
);
```

### Changement de clé
```java
KStream<String, VideoView> rekeyed = stream.selectKey(
    (key, value) -> value.getUserId()
);
```

### Windowing
```java
KTable<Windowed<String>, Long> windowed = stream
    .groupByKey()
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
    .count(Materialized.as("store-name"));
```

### Interroger un State Store
```java
ReadOnlyKeyValueStore<String, Long> store = streams.store(
    StoreQueryParameters.fromNameAndType(
        "store-name",
        QueryableStoreTypes.keyValueStore()
    )
);

Long value = store.get(key);
```

## 📊 Résultats attendus

Après 5 minutes avec 5 événements/seconde:
- ~1500 vues générées
- Les vidéos Action et Comedy dominent (distribution biaisée)
- Les utilisateurs FREE sont plus nombreux mais les VIP regardent plus longtemps
- Le top trending change toutes les 5 minutes

## 🐛 Troubleshooting

### Les stores sont null
- Vérifiez que les topics existent
- Attendez quelques secondes après le démarrage
- Vérifiez les logs Kafka Streams

### Les compteurs ne bougent pas
- Vérifiez que le `VideoViewProducer` tourne
- Vérifiez les logs pour les exceptions
- Consultez les métriques Kafka

### Join ne fonctionne pas
- Vérifiez que les clés correspondent (videoId ou userId)
- Assurez-vous que les KTables sont bien peuplées
- Vérifiez les logs pour les warnings de co-partitioning

## 📚 Ressources

- [Documentation Kafka Streams](https://kafka.apache.org/documentation/streams/)
- [Javadoc Kafka Streams](https://kafka.apache.org/36/javadoc/org/apache/kafka/streams/package-summary.html)
- [Cours "Kafka as a Data Hub 2025"](votre-lien-ici)

## ✅ Critères d'évaluation

1. **Topologie fonctionnelle** (40%)
   - Toutes les agrégations implémentées correctement
   - Joins fonctionnels
   - Windowing correct

2. **API REST** (30%)
   - Tous les endpoints implémentés
   - Gestion des erreurs
   - Résultats corrects

3. **Code quality** (20%)
   - Code lisible et commenté
   - Bonnes pratiques
   - Gestion des ressources

4. **Tests et validation** (10%)
   - Démonstration fonctionnelle
   - Tests des différents cas
   - Analyse des résultats

## 🎓 Bon courage !

N'hésitez pas à consulter les logs et à ajouter des `.peek()` ou `.foreach()` dans votre topologie pour débugger !
