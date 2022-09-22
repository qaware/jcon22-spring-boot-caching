package de.qaware.demo.jcon22.caching;

import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;

@Configuration
@EnableCaching
public class CachingConfiguration implements CachingConfigurer {

    @Bean
    public RedisCacheManagerBuilderCustomizer transactionAwareRedisCacheManagerBuilderCustomizer() {
        return RedisCacheManager.RedisCacheManagerBuilder::transactionAware;
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler(LogFactory.getLog(LoggingCacheErrorHandler.class), true);
    }
}
