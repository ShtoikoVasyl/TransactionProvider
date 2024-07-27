package edu.shtoiko.transactionprowider.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.shtoiko.transactionprowider.model.entity.Transaction;
import edu.shtoiko.transactionprowider.tools.JacksonDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.JacksonUtils;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ObjectMapper objectMapper() {
        return JacksonUtils.enhancedObjectMapper();
    }

    @Bean
    public ReceiverOptions<String, Transaction> kafkaReceiverOptions(ObjectMapper objectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "TransactionProviders");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configProps.put("spring.json.trusted.packages", "edu.shtoiko.transactionprowider.model.entity");

        ReceiverOptions<String, Transaction> receiverOptions = ReceiverOptions.create(configProps);
        return receiverOptions.subscription(Collections.singleton("transactions"))
            .withValueDeserializer(new JacksonDeserializer<>(objectMapper, Transaction.class));
    }

    @Bean
    public KafkaReceiver<String, Transaction> kafkaReceiver(ReceiverOptions<String, Transaction> receiverOptions) {
        return KafkaReceiver.create(receiverOptions);
    }
}