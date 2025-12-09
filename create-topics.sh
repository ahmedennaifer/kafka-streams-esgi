#!/bin/bash

set +e

# Script pour créer les topics Kafka nécessaires au projet
# Usage: ./create-topics.sh

BOOTSTRAP_SERVER="localhost:9092"

echo "🔧 Création des topics Kafka..."
echo ""

# Topic pour les événements de visionnage (avec partitionnement pour performance)
echo "📺 Création du topic video-views..."
docker exec kafka-video-streaming /opt/kafka/bin/kafka-topics.sh \
  --create --if-not-exists \
  --topic video-views \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --partitions 1 \
  --replication-factor 1 \
  --config retention.ms=3600000

# Topic pour les métadonnées de vidéos (compacted pour garder dernier état)
echo "🎬 Création du topic video-metadata..."
docker exec kafka-video-streaming /opt/kafka/bin/kafka-topics.sh \
  --create --if-not-exists \
  --topic video-metadata \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --partitions 1 \
  --replication-factor 1 \
  --config cleanup.policy=compact \
  --config segment.ms=60000 \
  --config min.cleanable.dirty.ratio=0.01

# Topic pour les abonnements utilisateurs (compacted)
echo "👤 Création du topic user-subscriptions..."
docker exec kafka-video-streaming /opt/kafka/bin/kafka-topics.sh \
  --create --if-not-exists \
  --topic user-subscriptions \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --partitions 1 \
  --replication-factor 1 \
  --config cleanup.policy=compact \
  --config segment.ms=60000 \
  --config min.cleanable.dirty.ratio=0.01

echo ""
echo "✅ Topics créés avec succès !"
echo ""
echo "📋 Liste des topics:"
docker exec kafka-video-streaming /opt/kafka/bin/kafka-topics.sh \
  --list \
  --bootstrap-server "$BOOTSTRAP_SERVER"

echo ""
echo "ℹ️  Pour voir les détails d'un topic:"
echo "   docker exec kafka-video-streaming kafka-topics.sh --describe --topic video-views --bootstrap-server localhost:9092"
