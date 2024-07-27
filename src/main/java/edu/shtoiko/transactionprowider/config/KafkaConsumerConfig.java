package edu.shtoiko.transactionprowider.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.shtoiko.transactionprowider.model.entity.Transaction;
import edu.shtoiko.transactionprowider.tools.JacksonDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.JacksonUtils;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

//@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return JacksonUtils.enhancedObjectMapper();
    }

//    @Bean
//    public ConsumerFactory<String, Transaction> consumerFactory(ObjectMapper objectMapper) {
//        Map<String, Object> configProps = new HashMap<>();
//        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "TransactionProviders");
//        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
//        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonDeserializer.class.getName());
//        configProps.put("targetType", Transaction.class);
//
//        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(),
//            new JacksonDeserializer<>(objectMapper, Transaction.class));
//    }
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, Transaction> kafkaListenerContainerFactory(
//        ConsumerFactory<String, Transaction> consumerFactory) {
//        ConcurrentKafkaListenerContainerFactory<String, Transaction> factory =
//            new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(consumerFactory);
//        factory.setConcurrency(3);
//        factory.getContainerProperties().setPollTimeout(3000);
//        return factory;
//    }

    @Bean
    public ReceiverOptions<String, Transaction> kafkaReceiverOptions(ObjectMapper objectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
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