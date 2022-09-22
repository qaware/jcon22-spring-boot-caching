package de.qaware.demo.jcon22.caching;

import lombok.RequiredArgsConstructor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class CacheManagerBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof CacheManager cacheManager) {
            return new DecoratingCacheManager(cacheManager);
        }
        return bean;
    }

    @RequiredArgsConstructor
    private static class DecoratingCacheManager implements CacheManager {
        private final CacheManager delegate;
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
            factory.addAspect(CacheCircuitBreakerAspect.class);
            return factory.getProxy();
        }
    }

}
