package de.qaware.demo.jcon22.caching;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static de.qaware.demo.jcon22.caching.CacheMetricsConsumer.GetCacheResult.HIT;
import static de.qaware.demo.jcon22.caching.CacheMetricsConsumer.GetCacheResult.MISS;


/**
 * Factory for {@link Cache} decorated with cache metrics.
 */
@Component
@RequiredArgsConstructor
public class CacheMetricsDecoratorFactory {
    private final List<CacheMetricsConsumer> cacheMetricsConsumers;

    /**
     * Decorates the given cache to send metrics
     *
     * @param cache the delegated cache
     * @return decorated cache
     */
    public Cache decorateCache(Cache cache) {
        return new CacheMetricsDecorator(cache);
    }

    /**
     * Decorates {@link Cache} object with metrics operations
     */
    @RequiredArgsConstructor
    class CacheMetricsDecorator implements Cache {
        private final Cache delegate;

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        @Nullable
        @Override
        public ValueWrapper get(Object key) {
            return getCacheAndSendMetrics(() -> delegate.get(key));
        }

        @Nullable
        @Override
        public <T> T get(Object key, @Nullable Class<T> type) {
            return getCacheAndSendMetrics(() -> delegate.get(key, type));
        }

        @Nullable
        @Override
        public <T> T get(Object o, Callable<T> callable) {
            return getCacheAndSendMetrics(() -> delegate.get(o, callable));
        }

        @Nullable
        private <T> T getCacheAndSendMetrics(Supplier<T> getCacheSupplier) {
            T value = null;
            long start = System.nanoTime();
            try {
                value = getCacheSupplier.get();
                return value;
            } finally {
                long stop = System.nanoTime();
                reportCacheGetOperation(value, start, stop);
            }
        }

        private <T> void reportCacheGetOperation(@Nullable T value, long start, long stop) {
            cacheMetricsConsumers.forEach(consumer -> consumer.reportCacheGetOperation(getName(),
                    value != null ? HIT : MISS, Duration.ofNanos(stop - start)
            ));
        }

        @Override
        public void put(Object key, @Nullable Object value) {
            long start = System.nanoTime();
            try {
                delegate.put(key, value);
            } finally {
                long stop = System.nanoTime();
                cacheMetricsConsumers.forEach(consumer -> consumer.reportCachePutOperation(getName(), Duration.ofNanos(stop - start)));
            }
        }

        @Override
        public void evict(Object key) {
            long start = System.nanoTime();
            try {
                delegate.evict(key);
            } finally {
                long stop = System.nanoTime();
                cacheMetricsConsumers.forEach(consumer -> consumer.reportCacheEvictOperation(getName(), Duration.ofNanos(stop - start)));
            }
        }

        @Override
        public void clear() {
            cacheMetricsConsumers.forEach(consumer -> consumer.reportCacheClearOperation(getName()));
            delegate.clear();
        }
    }
}
