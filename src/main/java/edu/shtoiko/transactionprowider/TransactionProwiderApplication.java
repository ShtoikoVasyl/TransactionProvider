package edu.shtoiko.transactionprowider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class TransactionProwiderApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionProwiderApplication.class, args);
    }
}
