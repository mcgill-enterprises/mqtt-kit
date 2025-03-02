package jmc.broker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.stomp.StompServer;
import io.vertx.ext.stomp.StompServerHandler;
import io.vertx.mqtt.MqttClient;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class StompBroker {
    @Autowired
    ObjectMapper om;
    @Autowired
    Vertx vertx;
    @Autowired
    MqttClient mqttClient;

    @Bean
    public StompServer stompServer() throws InterruptedException {
        CountDownLatch stompInit = new CountDownLatch(1);
        Future<StompServer> server = StompServer.create(vertx)
                .handler(StompServerHandler.create(vertx))
                .listen()
                .onComplete(init -> {
                    if (init.succeeded()) {
                        log.info("Stomp server ready");
                        stompInit.countDown();
                    } else {
                        log.error(init.cause().getMessage(), init.cause());
                        throw new RuntimeException(init.cause());
                    }
                });
        stompInit.await(10, TimeUnit.SECONDS);
        if (server.succeeded()) {
            return server.result();
        } else {
            throw new RuntimeException(server.cause());
        }
    }
}
