package edu.shtoiko.transactionprowider.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.shtoiko.transactionprowider.model.entity.Transaction;
import edu.shtoiko.transactionprowider.model.entity.WithdrawResult;
import edu.shtoiko.transactionprowider.model.entity.WithdrawalTransaction;
import edu.shtoiko.transactionprowider.tools.JacksonDeserializer;
import edu.shtoiko.transactionprowider.tools.JacksonSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.transaction-prowider.group-id}")
    private String transactionProvidersGroupId;

    @Value("${kafka.withdrawal-provider.group-id}")
    private String withdrawalProvidersGroupId;

    @Value("${kafka.transaction.topic}")
    private String transactionsTopic;

    @Value("${kafka.withdrawal.topic}")
    private String withdrawalTopic;

    @Value("${spring.kafka.properties.sasl.jaas.config:}")
    private String jaasConfig;

    @Bean
    public ObjectMapper objectMapper() {
        return JacksonUtils.enhancedObjectMapper();
    }

    private Map<String, Object> commonConfigProps() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonSerializer.class.getName());
        return configProps;
    }

    public Map<String, Object> cloudConfigProps() {
        Map<String, Object> configProps = commonConfigProps();
        configProps.put("security.protocol", "SASL_SSL");
        configProps.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        configProps.put(SaslConfigs.SASL_JAAS_CONFIG, jaasConfig);
        return configProps;
    }

    @Bean
    @Profile("prod")
    public ReceiverOptions<String, Transaction> kafkaTransactionReceiverOptionsProd(ObjectMapper objectMapper) {
        Map<String, Object> configProps = cloudConfigProps();
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, transactionProvidersGroupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonDeserializer.class.getName());

        ReceiverOptions<String, Transaction> receiverOptions = ReceiverOptions.create(configProps);
        return receiverOptions.subscription(Collections.singleton(transactionsTopic))
            .withValueDeserializer(new JacksonDeserializer<>(objectMapper, Transaction.class));
    }

    @Bean
    @Profile("dev")
    public ReceiverOptions<String, Transaction> kafkaTransactionReceiverOptionsDev(ObjectMapper objectMapper) {
        Map<String, Object> configProps = commonConfigProps();
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, transactionProvidersGroupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonDeserializer.class.getName());

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
    @Profile("prod")
    public ReceiverOptions<String, WithdrawalTransaction> kafkaWithdrawReceiverOptionsProd(ObjectMapper objectMapper) {
        Map<String, Object> configProps = cloudConfigProps();
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, withdrawalProvidersGroupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonDeserializer.class.getName());

        ReceiverOptions<String, WithdrawalTransaction> receiverOptions = ReceiverOptions.create(configProps);
        return receiverOptions.subscription(Collections.singleton(withdrawalTopic))
            .withValueDeserializer(new JacksonDeserializer<>(objectMapper, WithdrawalTransaction.class));
    }

    @Bean
    @Profile("dev")
    public ReceiverOptions<String, WithdrawalTransaction> kafkaWithdrawReceiverOptionsDev(ObjectMapper objectMapper) {
        Map<String, Object> configProps = commonConfigProps();
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, withdrawalProvidersGroupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonDeserializer.class.getName());

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
    @Profile("prod")
    public SenderOptions<String, WithdrawResult> kafkaWithdrawResultSenderOptionsProd(ObjectMapper objectMapper) {
        Map<String, Object> configProps = cloudConfigProps();
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonSerializer.class.getName());

        return SenderOptions.<String, WithdrawResult>create(configProps)
            .withValueSerializer(new JacksonSerializer<>(objectMapper));
    }

    @Bean
    @Profile("dev")
    public SenderOptions<String, WithdrawResult> kafkaWithdrawResultSenderOptionsDev(ObjectMapper objectMapper) {
        Map<String, Object> configProps = commonConfigProps();
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonSerializer.class.getName());

        return SenderOptions.<String, WithdrawResult>create(configProps)
            .withValueSerializer(new JacksonSerializer<>(objectMapper));
    }

    @Bean
    public KafkaSender<String, WithdrawResult> kafkaWithdrawResultSender(
        SenderOptions<String, WithdrawResult> senderOptions) {
        return KafkaSender.create(senderOptions);
    }
}