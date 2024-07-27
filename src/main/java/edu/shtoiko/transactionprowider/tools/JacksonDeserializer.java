package edu.shtoiko.transactionprowider.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class JacksonDeserializer<T> implements Deserializer<T> {
    private final ObjectMapper objectMapper;
    private Class<T> targetType;

    public JacksonDeserializer(ObjectMapper objectMapper, Class<T> targetType) {
        this.objectMapper = objectMapper;
        this.targetType = targetType;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        return deserialize(topic, null, data);
    }

    @Override
    public T deserialize(String topic, Headers headers, byte[] data) {
        try {
            if (data == null) {
                return null;
            }
            return objectMapper.readValue(data, targetType);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization error", e);
        }
    }

    @Override
    public void close() {
        Deserializer.super.close();
    }
}