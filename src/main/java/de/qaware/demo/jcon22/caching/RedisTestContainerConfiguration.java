package de.qaware.demo.jcon22.caching;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.util.Map;

@Slf4j
public class RedisTestContainerConfiguration implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String REDIS_IMAGE = "redis:7.0.0";
    private static final int REDIS_PORT = 6379;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        var redisContainer = new GenericContainer<>(REDIS_IMAGE)
                .withExposedPorts(REDIS_PORT);
        redisContainer.start();
        applicationContext.addApplicationListener((ApplicationListener<ContextClosedEvent>) event -> {
            log.info("Context closed, stopping container");
            redisContainer.stop();
        });

        applicationContext.getEnvironment().getPropertySources().addLast(new MapPropertySource("redis-cache-config", Map.of(
                "spring.redis.host", redisContainer.getHost(),
                "spring.redis.port", redisContainer.getMappedPort(REDIS_PORT)
        )));

    }
}
