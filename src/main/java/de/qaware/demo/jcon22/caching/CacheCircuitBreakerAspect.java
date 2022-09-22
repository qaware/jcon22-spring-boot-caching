package de.qaware.demo.jcon22.caching;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.dao.QueryTimeoutException;

import java.time.Duration;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED;

@Aspect
class CacheCircuitBreakerAspect {

    static final CircuitBreaker CACHE_CIRCUIT_BREAKER = CircuitBreaker.of("cacheCircuitBreaker", CircuitBreakerConfig.custom()
            .recordException(QueryTimeoutException.class::isInstance)
            .slidingWindow(100, 100, COUNT_BASED)
            .failureRateThreshold(20)
            .enableAutomaticTransitionFromOpenToHalfOpen()
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .writableStackTraceEnabled(false)
            .build()
    );

    @Pointcut("execution(* org.springframework.cache.Cache.*(..))")
    public void anyCacheMethod() {
        // just pointcut
    }

    @Pointcut("execution(* org.springframework.cache.Cache.getName(..))")
    public void getNameMethod() {
        // just pointcut
    }

    @Pointcut("execution(* org.springframework.cache.Cache.getNativeCache(..))")
    public void getNativeCacheMethod() {
        // just pointcut
    }

    @Around("anyCacheMethod() && !getNameMethod() && !getNativeCacheMethod()")
    public Object wrap(ProceedingJoinPoint pjp) throws Throwable {
        return CACHE_CIRCUIT_BREAKER.executeCheckedSupplier(pjp::proceed);
    }
}
