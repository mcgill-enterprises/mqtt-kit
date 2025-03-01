package jmc.vertx;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.Charset;

@Configuration
@Slf4j
public class MqttServerConfig {
    @Bean("mqttServer")
    public MqttServer mqttServer(Vertx vertx, SimpMessagingTemplate template) {
        log.info("mqtt server {}", vertx);
        MqttServer mqttServer = MqttServer.create(vertx);
        mqttServer.endpointHandler(endpoint -> {
                    // shows main connect info
                    log.info("MQTT client [{}  request to connect, clean session = {}]", endpoint.clientIdentifier(), endpoint.isCleanSession());

                    if (endpoint.auth() != null) {
                        log.info("[username = {}, password = {}]",
                                endpoint.auth().getUsername(), endpoint.auth().getPassword());
                    }
                    if (endpoint.will() != null) {
                        log.info(
                                "[will topic = {} msg = {} QoS = {} isRetain = {}]",
                                endpoint.will().getWillTopic(),
                                endpoint.will().getWillMessageBytes(),
                                endpoint.will().getWillQos(), endpoint.will().isWillRetain());
                    }

                    log.info("[keep alive timeout = {}]", endpoint.keepAliveTimeSeconds());

                    // accept connection from the remote client
                    endpoint.accept(false);
                    endpoint.disconnectHandler(disc -> log.info("MQTT client [{} request to disconnect]", endpoint.clientIdentifier()));
                    endpoint.publishHandler(message -> {
                        log.info("received message [{}] on topic [{}] with QoS [{}]", message.payload().toString(Charset.defaultCharset()), message.topicName(), message.qosLevel());
                        template.convertAndSend("/topic/" + message.topicName(),
                                String.format("%s",
                                        message.payload().toString(Charset.defaultCharset())));

                        if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
                            endpoint.publishAcknowledge(message.messageId());
                        } else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
                            endpoint.publishReceived(message.messageId());
                        }
                    }).publishReleaseHandler(endpoint::publishComplete);
                    endpoint.subscribeHandler(sub ->
                            log.info("subscribed {}", sub.topicSubscriptions().get(0)));

                })
                .listen(ar -> {
                    log.info("MQTT server is listening on port {}", ar.result().actualPort());
                    if (!ar.succeeded()) {
                        log.error("Error on starting the server", ar.cause());
                    }
                });
        return mqttServer;
    }
}
