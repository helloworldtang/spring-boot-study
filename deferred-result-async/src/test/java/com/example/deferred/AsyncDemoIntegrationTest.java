package com.example.deferred;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AsyncDemoApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.tomcat.threads.max=2",
                "server.tomcat.threads.min-spare=1",
                "server.tomcat.max-connections=5",
                "server.tomcat.accept-count=2"
        })
class AsyncDemoIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(AsyncDemoIntegrationTest.class);

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void syncEndpoint_isBoundByTomcatThreads() throws Exception {
        int requests = 4;
        long ms = 1500;
        String url = "http://localhost:" + port + "/sync/process?ms=" + ms;
        List<CompletableFuture<ResponseEntity<String>>> futures = new ArrayList<>();
        var pool = Executors.newFixedThreadPool(requests);

        Instant start = Instant.now();
        for (int i = 0; i < requests; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> restTemplate.getForEntity(url, String.class), pool));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        long elapsed = Duration.between(start, Instant.now()).toMillis();
        log.info("sync {} requests finished in {}ms", requests, elapsed);
        assertTrue(elapsed >= 2500, "Expected sync total time >= 2500ms but was " + elapsed);
    }

    @Test
    void asyncEndpoint_releasesTomcatThreads_quicker() throws Exception {
        int requests = 4;
        long ms = 1500;
        String url = "http://localhost:" + port + "/async/process?ms=" + ms;
        List<CompletableFuture<ResponseEntity<String>>> futures = new ArrayList<>();
        var pool = Executors.newFixedThreadPool(requests);

        Instant start = Instant.now();
        for (int i = 0; i < requests; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> restTemplate.getForEntity(url, String.class), pool));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        long elapsed = Duration.between(start, Instant.now()).toMillis();
        log.info("async {} requests finished in {}ms", requests, elapsed);
        assertTrue(elapsed < 2200, "Expected async total time < 2200ms but was " + elapsed);
    }

    @Test
    void exceedingConnectionLimit_causesConnectTimeout_sync() throws Exception {
        int requests = 12; // exceed max-connections(5)+accept-count(2)
        long ms = 5000; // keep connections busy
        String url = "http://localhost:" + port + "/sync/process?ms=" + ms;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(300))
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
                            log.warn("sync request failed: {}", ex.toString());
                        } else {
                            success.incrementAndGet();
                        }
                    }));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).orTimeout(10, TimeUnit.SECONDS).exceptionally(ex -> null).join();
        log.info("sync exceeding connections -> success={}, failures={}", success.get(), failures.get());
        assertTrue(failures.get() >= 1, "Expected some connection failures when exceeding limits");
    }

    @Test
    void exceedingConnectionLimit_causesConnectTimeout_async() throws Exception {
        int requests = 12; // exceed max-connections(5)+accept-count(2)
        long ms = 5000; // keep connections busy until worker completes
        String url = "http://localhost:" + port + "/async/process?ms=" + ms;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(300))
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
                            log.warn("async request failed: {}", ex.toString());
                        } else {
                            success.incrementAndGet();
                        }
                    }));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).orTimeout(10, TimeUnit.SECONDS).exceptionally(ex -> null).join();
        log.info("async exceeding connections -> success={}, failures={}", success.get(), failures.get());
        assertTrue(failures.get() >= 1, "Expected some connection failures when exceeding limits even with DeferredResult");
    }
}
