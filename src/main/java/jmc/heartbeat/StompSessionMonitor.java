package jmc.heartbeat;

import io.vertx.core.http.WebSocketClient;
import io.vertx.ext.stomp.StompServer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;

import java.lang.reflect.Type;

@Configuration
@Slf4j
public class StompSessionMonitor {
    @Autowired
    StompServer stompServer;
    @PostConstruct
    void init() {
        log.info("stomp server port {}", stompServer.actualPort());
    }
    @Bean
    public StompSessionHandler stompSessionHandler() {
        StompSessionHandler stompSessionHandler = new StompSessionHandler() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {

            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {

            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {

            }

            @Override
            public Type getPayloadType(StompHeaders headers) {
                return null;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {

            }
        };

        return stompSessionHandler;
    }
}
