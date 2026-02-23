package ru.stroy1click.confirmationcode.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    public static PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:15.3").withInitScript("init.sql");
    }

    @Bean
    @ServiceConnection
    public static KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));
    }

    @Bean
    public static GenericContainer<?> redisContainer() {
        return new GenericContainer<>("redis:6.2").withExposedPorts(6379);
    }

    @Bean
    public DynamicPropertyRegistrar properties(GenericContainer<?> redisContainer) {
        return (registry) -> {
            String redissonHost = redisContainer.getHost();
            Integer redissonPort =  redisContainer.getMappedPort(6379);

            registry.add("redisson.host", () -> redissonHost);
            registry.add("redisson.port", () -> redissonPort);
        };
    }
}