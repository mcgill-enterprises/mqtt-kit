package jmc.subs;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.stomp.StompClient;
import io.vertx.ext.stomp.StompClientConnection;
import io.vertx.ext.stomp.StompServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class StompClientConfig {
    @Autowired
    StompServer stompServer;
    @Bean
    public StompClientConnection stompClient(Vertx vertx) throws InterruptedException {
        CountDownLatch init = new CountDownLatch(1);
        Future<StompClientConnection> client = StompClient.create(vertx).connect()
                .onFailure(err -> log.error(err.getMessage(), err))
                .onSuccess(conn -> {conn.subscribe("heartbeat", frame -> log.info("stomp client received message: topic {}, payload: {}", frame.getDestination(), frame.getBodyAsString("UTF-8"))).onComplete(sub -> init.countDown());});
        init.await(10, TimeUnit.SECONDS);
        return client.result();
    }
}
