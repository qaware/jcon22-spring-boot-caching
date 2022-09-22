package de.qaware.demo.jcon22.caching;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Consumer;

@Aspect
@RequiredArgsConstructor
@EqualsAndHashCode
public class CacheTransactionAwareAspect {
    private final CacheErrorHandler cacheErrorHandler;
    private final Cache cache;

    private static Object proceedAfterCommit(ProceedingJoinPoint pjp, Consumer<RuntimeException> errorHandler) throws Throwable {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        pjp.proceed();
                    } catch (RuntimeException e) {
                        errorHandler.accept(e);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            return null;
        } else {
            return pjp.proceed();
        }
    }

    @Around("execution(* org.springframework.cache.Cache.put(..))")
    public Object wrapPutMethod(ProceedingJoinPoint pjp) throws Throwable {
        return proceedAfterCommit(pjp, e -> {
            var args = pjp.getArgs();
            cacheErrorHandler.handleCachePutError(e, cache, args[0], args[1]);
        });
    }

    @Around("execution(* org.springframework.cache.Cache.evict(..))")
    public Object wrapEvictMethod(ProceedingJoinPoint pjp) throws Throwable {
        return proceedAfterCommit(pjp, e -> {
            var args = pjp.getArgs();
            cacheErrorHandler.handleCacheEvictError(e, cache, args[0]);
        });
    }

    @Around("execution(* org.springframework.cache.Cache.clear(..))")
    public Object wrapClearMethod(ProceedingJoinPoint pjp) throws Throwable {
        return proceedAfterCommit(pjp, e -> {
            cacheErrorHandler.handleCacheClearError(e, cache);
        });
    }
}
