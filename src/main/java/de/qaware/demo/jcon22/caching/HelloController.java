package de.qaware.demo.jcon22.caching;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Slf4j
public class HelloController {

    private final HelloRepository repository;

    private static final int REPOSITORY_CALLS = 10;

    @Transactional
    @GetMapping(value = "hello", produces = MediaType.TEXT_PLAIN_VALUE)
    public String hello() throws InterruptedException {
        log.info("Calling /hello endpoint");
        StopWatch stopWatch = new StopWatch();
        StringBuilder sb = new StringBuilder();
        stopWatch.start();
        sb.append(repository.getHelloResponse());
        sb.append('\n');
        stopWatch.stop();
        sb.append("\n\n--> Calling repository took ");
        sb.append(stopWatch.getTotalTimeMillis());
        sb.append(" ms");
        appendCircuitBreakerState(sb);
        return sb.toString();
    }

    @Transactional
    @GetMapping(value = "hello-many", produces = MediaType.TEXT_PLAIN_VALUE)
    public String helloMany() throws InterruptedException {
        log.info("Calling /hello-many endpoint");
        StopWatch stopWatch = new StopWatch();
        StringBuilder sb = new StringBuilder();
        stopWatch.start();
        for (int i = 0; i < REPOSITORY_CALLS; i++) {
            sb.append(repository.getHelloResponse());
            sb.append('\n');
        }
        stopWatch.stop();
        sb.append("\n\n--> Calling repository ");
        sb.append(REPOSITORY_CALLS);
        sb.append(" times took ");
        sb.append(stopWatch.getTotalTimeMillis());
        sb.append(" ms");
        appendCircuitBreakerState(sb);
        return sb.toString();
    }

    private void appendCircuitBreakerState(StringBuilder sb) {
        sb.append('\n')
                .append("Circuit breaker is ")
                .append(CacheCircuitBreakerAspect.CACHE_CIRCUIT_BREAKER.getState());
    }

}
