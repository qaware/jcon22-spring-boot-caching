package de.qaware.demo.jcon22.caching;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HelloRepository {

    @Cacheable(cacheNames = "hello")
    public String getHelloResponse() throws InterruptedException {
        var sleepInMs = Math.round(10 * Math.random() + 20);
        Thread.sleep(sleepInMs);
        log.info("Repository access took {} ms (because I was sleeping)", sleepInMs);
        return "Hello from repository";
    }
}
