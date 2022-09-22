package de.qaware.demo.jcon22.caching;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class MicrometerCacheMetricsConsumer implements CacheMetricsConsumer {
    private static final String TAG_NAME = "name";
    private static final String TAG_GET_CACHE_RESULT = "result";
    private static final String CACHE_GET_TIMER_NAME = "cache_gets";
    private static final String CACHE_PUT_TIMER_NAME = "cache_puts";
    private static final String CACHE_EVICT_TIMER_NAME = "cache_evicts";
    private static final String CACHE_CLEAR_COUNTER_NAME = "cache_clears_total";
    private final MeterRegistry meterRegistry;

    @Override
    public void reportCacheGetOperation(String cacheName, CacheMetricsConsumer.GetCacheResult result, Duration duration) {
        meterRegistry.timer(CACHE_GET_TIMER_NAME,
                TAG_NAME, cacheName,
                TAG_GET_CACHE_RESULT, result.name()
        ).record(duration);
    }

    @Override
    public void reportCachePutOperation(String cacheName, Duration duration) {
        meterRegistry.timer(CACHE_PUT_TIMER_NAME, TAG_NAME, cacheName).record(duration);
    }

    @Override
    public void reportCacheEvictOperation(String cacheName, Duration duration) {
        meterRegistry.timer(CACHE_EVICT_TIMER_NAME, TAG_NAME, cacheName).record(duration);
    }

    @Override
    public void reportCacheClearOperation(String cacheName) {
        meterRegistry.counter(CACHE_CLEAR_COUNTER_NAME, TAG_NAME, cacheName).increment();
    }
}
