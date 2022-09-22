package de.qaware.demo.jcon22.caching;

import lombok.RequiredArgsConstructor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.function.SingletonSupplier;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class CacheManagerBeanPostProcessor implements BeanPostProcessor, InitializingBean {

    private final ObjectFactory<CachingConfigurer> cachingConfigurerSupplier;
    private final ObjectFactory<CacheMetricsDecoratorFactory> cacheMetricsDecoratorFactorySupplier;
    private Supplier<CacheErrorHandler> cacheErrorHandlerSupplier;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof CacheManager cacheManager) {
            return new DecoratingCacheManager(cacheManager, cacheErrorHandlerSupplier.get());
        }
        return bean;
    }

    @Override
    public void afterPropertiesSet() {
        cacheErrorHandlerSupplier = new SingletonSupplier<>(() -> cachingConfigurerSupplier.getObject().errorHandler(), SimpleCacheErrorHandler::new);
    }

    @RequiredArgsConstructor
    private class DecoratingCacheManager implements CacheManager {
        private final CacheManager delegate;
        private final CacheErrorHandler cacheErrorHandler;
        private final ConcurrentHashMap<String, Cache> decoratedCaches = new ConcurrentHashMap<>();

        @Override
        public Cache getCache(String name) {
            return decoratedCaches.computeIfAbsent(name, this::createDecoratedCache);
        }

        @Override
        public Collection<String> getCacheNames() {
            return delegate.getCacheNames();
        }


        @Nullable
        private Cache createDecoratedCache(String name) {
            var targetCache = delegate.getCache(name);
            if (targetCache == null) {
                return null;
            }
            var factory = new AspectJProxyFactory(targetCache);
            factory.addAspect(new CacheTransactionAwareAspect(cacheErrorHandler, targetCache));
            factory.addAspect(CacheCircuitBreakerAspect.class);
            return cacheMetricsDecoratorFactorySupplier.getObject().decorateCache(factory.getProxy());
        }
    }

}
