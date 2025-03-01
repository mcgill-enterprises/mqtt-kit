package jmc.vertx;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.messages.MqttPublishMessage;
import jmc.metrics.MqttMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.Charset;

@Configuration
@Slf4j
public class MqttServerConfig {
    @Autowired
    MqttMetrics metrics;
    @Bean("mqttServer")
    public MqttServer mqttServer(Vertx vertx) {
        MqttServer mqttServer = MqttServer.create(vertx);
        mqttServer.endpointHandler(endpoint -> {
            // shows main connect info
            log.info("MQTT client [{} request to connect, clean session = {}]", endpoint.clientIdentifier(), endpoint.isCleanSession());

            if (endpoint.auth() != null) {
                log.info("[username = {}, password = {}]", endpoint.auth().getUsername(), endpoint.auth().getPassword());
            }
            if (endpoint.will() != null) {
                log.info("[will topic = {} msg = {} QoS = {} isRetain = {}]", endpoint.will().getWillTopic(), endpoint.will().getWillMessageBytes(), endpoint.will().getWillQos(), endpoint.will().isWillRetain());
            }

            log.info("[keepalive timeout = {}]", endpoint.keepAliveTimeSeconds());

            // accept connection from the remote client
            endpoint.accept(false /* no session */);
            endpoint.disconnectHandler(disc -> {
                log.info("MQTT client [{} request to disconnect]", endpoint.clientIdentifier());
                        metrics.decrementMqttClients();
                    })
                    .publishHandler(message -> {
                        log.info("server received message [{}] on topic [{}] with QoS [{}]", message.payload().toString(Charset.defaultCharset()), message.topicName(), message.qosLevel());

                        // Publish message to the client
                        // https://vertx.io/docs/4.1.8/vertx-mqtt/java/#_publish_message_to_the_client
                        publishClientResponse(endpoint, message).onFailure(failedResponse -> log.error("response message publish failed", failedResponse.getCause()));

                        if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
                            endpoint.publishAcknowledge(message.messageId());
                        } else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
                            endpoint.publishReceived(message.messageId());
                        }
                    })
                    .publishReleaseHandler(endpoint::publishComplete)
                    .unsubscribeHandler(unsub -> log.info("unsubscribed {}", unsub.topics().getFirst()))
                    .subscribeHandler(sub -> {
                        log.info("subscribed {}", sub.topicSubscriptions().getFirst());});
            metrics.incrementMqttClients();
        }).listen(ar -> {
            log.info("MQTT server is listening on port {}", ar.result().actualPort());
            if (!ar.succeeded()) {
                log.error("Error on starting the server", ar.cause());
            }
        });
        return mqttServer;
    }

    private Future<Integer> publishClientResponse(MqttEndpoint endpoint, MqttPublishMessage message) {
        return endpoint.publish(message.topicName(), message.payload(), MqttQoS.AT_LEAST_ONCE, false, false);
    }
}
