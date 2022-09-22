package de.qaware.demo.jcon22.caching;

import java.time.Duration;

public interface CacheMetricsConsumer {
    void reportCacheGetOperation(String cacheName, GetCacheResult result, Duration duration);

    void reportCachePutOperation(String cacheName, Duration duration);

    void reportCacheEvictOperation(String cacheName, Duration duration);

    void reportCacheClearOperation(String cacheName);

    enum GetCacheResult {
        HIT, MISS;
    }
}
