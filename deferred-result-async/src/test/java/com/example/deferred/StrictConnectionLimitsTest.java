package com.example.deferred;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = AsyncDemoApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.tomcat.threads.max=2",
                "server.tomcat.threads.min-spare=1",
                "server.tomcat.max-connections=2",
                "server.tomcat.accept-count=1"
        })
class StrictConnectionLimitsTest {
    private static final Logger log = LoggerFactory.getLogger(StrictConnectionLimitsTest.class);

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void strict_sync_exceedingConnections_hasManyFailures() {
        int requests = 30;
        long ms = 5000;
        String url = "http://localhost:" + port + "/sync/process?ms=" + ms;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(200))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < requests; i++) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url + i)).GET().build();
            int finalI = i;
            futures.add(client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .whenComplete((resp, ex) -> {
                        if (ex != null) {
                            failures.incrementAndGet();
                            log.warn("taskIdx {} strict sync request failed: {}", finalI, ex.toString());
                        } else {
                            success.incrementAndGet();
                        }
                    }));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).orTimeout(15, TimeUnit.SECONDS).exceptionally(ex -> null).join();
        log.info("strict sync exceeding connections -> success={}, failures={}", success.get(), failures.get());
        assertTrue(failures.get() >= 10);
    }

    @Test
    void strict_async_exceedingConnections_hasManyFailures() {
        int requests = 30;
        long ms = 5000;
        String url = "http://localhost:" + port + "/async/process?ms=" + ms;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(200))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < requests; i++) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
            futures.add(client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .whenComplete((resp, ex) -> {
                        if (ex != null) {
                            failures.incrementAndGet();
                            log.warn("strict async request failed: {}", ex.toString());
                        } else {
                            success.incrementAndGet();
                        }
                    }));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).orTimeout(15, TimeUnit.SECONDS).exceptionally(ex -> null).join();
        log.info("strict async exceeding connections -> success={}, failures={}", success.get(), failures.get());
        assertTrue(failures.get() >= 10);
    }
}

