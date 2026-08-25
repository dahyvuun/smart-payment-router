package com.dahyvuun.payment_router;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers configuration for all integration tests.
 *
 * @ServiceConnection (Spring Boot 3.1+) automatically injects
 * container connection properties (datasource URL, Redis host/port, Kafka bootstrap servers)
 * into the Spring context — no hardcoding in application-test.yml needed.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(
                DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("payment_router_test")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("init-vector-extensions.sql");
    }

    /**
     * Redis 7 container.
     * @ServiceConnection -> auto-configures spring.data.redis.*
     */
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
    }

    /**
     * Kafka container.
     * @ServiceConnection -> auto-configures spring.kafka.bootstrap-servers
     */
    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));
    }
}