package edu.shtoiko.transactionprowider.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.shtoiko.transactionprowider.model.entity.Transaction;
import edu.shtoiko.transactionprowider.model.entity.WithdrawResult;
import edu.shtoiko.transactionprowider.model.entity.WithdrawalTransaction;
import edu.shtoiko.transactionprowider.tools.JacksonDeserializer;
import edu.shtoiko.transactionprowider.tools.JacksonSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.JacksonUtils;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.transaction-prowider.group-id}")
    private String transactionProvidersGroupId;

    @Value("${kafka.withdrawal-provider.group-id}")
    private String withdrawalProvidersGroupId;

    @Value("${kafka.transaction.topic}")
    private String transactionsTopic;

    @Value("${kafka.withdrawal.topic}")
    private String withdrawalTopic;

    @Bean
    public ObjectMapper objectMapper() {
        return JacksonUtils.enhancedObjectMapper();
    }

    private Map<String, Object> commonConfigProps() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configProps.put("spring.json.trusted.packages", "edu.shtoiko.transactionprowider.model.entity");
        return configProps;
    }

    @Bean
    public ReceiverOptions<String, Transaction> kafkaTransactionReceiverOptions(ObjectMapper objectMapper) {
        Map<String, Object> configProps = commonConfigProps();
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, transactionProvidersGroupId);

        ReceiverOptions<String, Transaction> receiverOptions = ReceiverOptions.create(configProps);
        return receiverOptions.subscription(Collections.singleton(transactionsTopic))
            .withValueDeserializer(new JacksonDeserializer<>(objectMapper, Transaction.class));
    }

    @Bean
    public KafkaReceiver<String, Transaction> kafkaTransactionReceiver(
        ReceiverOptions<String, Transaction> receiverOptions) {
        return KafkaReceiver.create(receiverOptions);
    }

    @Bean
    public ReceiverOptions<String, WithdrawalTransaction> kafkaWithdrawReceiverOptions(ObjectMapper objectMapper) {
        Map<String, Object> configProps = commonConfigProps();
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, withdrawalProvidersGroupId);

        ReceiverOptions<String, WithdrawalTransaction> receiverOptions = ReceiverOptions.create(configProps);
        return receiverOptions.subscription(Collections.singleton(withdrawalTopic))
            .withValueDeserializer(new JacksonDeserializer<>(objectMapper, WithdrawalTransaction.class));
    }

    @Bean
    public KafkaReceiver<String, WithdrawalTransaction> kafkaWithdrawReceiver(
        ReceiverOptions<String, WithdrawalTransaction> receiverOptions) {
        return KafkaReceiver.create(receiverOptions);
    }

    @Bean
    public SenderOptions<String, WithdrawResult> kafkaWithdrawResultSenderOptions(ObjectMapper objectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            org.apache.kafka.common.serialization.StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonSerializer.class);

        return SenderOptions.<String, WithdrawResult>create(configProps)
            .withValueSerializer(new JacksonSerializer<>(objectMapper));
    }

    @Bean
    public KafkaSender<String, WithdrawResult> kafkaWithdrawResultSender(
        SenderOptions<String, WithdrawResult> senderOptions) {
        return KafkaSender.create(senderOptions);
    }
}