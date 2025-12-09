package com.streaming.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;

/**
 * Serde générique pour sérialiser/désérialiser en JSON avec Jackson
 */
public class JsonSerde<T> implements Serde<T> {
    private final ObjectMapper objectMapper;
    private final Class<T> targetType;

    public JsonSerde(Class<T> targetType) {
        this.targetType = targetType;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Serializer<T> serializer() {
        return new JsonSerializer();
    }

    @Override
    public Deserializer<T> deserializer() {
        return new JsonDeserializer();
    }

    private class JsonSerializer implements Serializer<T> {
        @Override
        public byte[] serialize(String topic, T data) {
            if (data == null) {
                return null;
            }
            try {
                return objectMapper.writeValueAsBytes(data);
            } catch (IOException e) {
                throw new SerializationException("Error serializing JSON message", e);
            }
        }
    }

    private class JsonDeserializer implements Deserializer<T> {
        @Override
        public T deserialize(String topic, byte[] data) {
            if (data == null) {
                return null;
            }
            try {
                return objectMapper.readValue(data, targetType);
            } catch (IOException e) {
                throw new SerializationException("Error deserializing JSON message", e);
            }
        }
    }
}
