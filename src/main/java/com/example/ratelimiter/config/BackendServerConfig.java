package com.example.ratelimiter.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

@Configuration
public class BackendServerConfig {

    private DisposableServer backendServer;

    @PostConstruct
    public void startBackend() {
        backendServer = HttpServer.create()
                .port(8081)
                .route(routes -> routes
                        .get("/hello", (req, res) ->
                                res.sendString(reactor.core.publisher.Mono.just("Hello from Spring Boot + Redis!")))
                )
                .bindNow();
    }

    @PreDestroy
    public void stopBackend() {
        if (backendServer != null) {
            backendServer.disposeNow();
        }
    }
}
