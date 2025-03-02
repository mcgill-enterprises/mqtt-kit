package jmc.vertx;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.impl.MqttClientImpl;
import jmc.metrics.MqttMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@Configuration
@Slf4j
public class MqttClientConfig {
    public static void reset(MqttMetrics metrics, MqttClient client) {
        client.disconnect(disc -> log.info("disconnected {}", disc));
        client
        .connect(1883, "localhost").onSuccess(ok -> {
            metrics.incrementMqttClients();
            log.info("mqtt client connected {}", ok);
        }).onFailure(bad -> {
            log.error(bad.getMessage(), bad.getCause());
            throw new RuntimeException(bad);
        });
    }
    @Autowired
    MqttMetrics metrics;
    @Bean("mqttClient")
    public MqttClient mqttClient(Vertx vertx, MqttServer mqttServer,  SimpMessagingTemplate template) throws InterruptedException {
        MqttClient mqttClient = MqttClient.create(vertx, new MqttClientOptions()
                .setClientId(UUID.randomUUID().toString())
                .setAckTimeout(5 /* seconds */)
                .setUsername("default")
                .setPassword("default")
                .setCleanSession(true)
                .setAutoKeepAlive(true));
        // https://github.com/spring-projects/spring-framework/issues/2329
        CountDownLatch startup = new CountDownLatch(1);
        mqttClient.publishCompletionExpirationHandler(pubAckTimeout -> {
            // https://github.com/mcgill-enterprises/mqtt-kit/issues/4
            log.error("MQTT Client PUBACK Timeout {}", pubAckTimeout);

            metrics.incrementPubackTimeouts();
        });

        mqttClient.closeHandler(onClose -> {
            // needs backoff
            // does disconnect still call the closeHandler
            log.warn("mqtt client closed");
            // io.vertx.core.net.NetSocket.closeHandler
//            synchronized (MqttClientImpl.class) {
//                mqttClient.connect(1883, "localhost").onComplete(conn -> log.warn("mqtt client reconnection {}", conn));
//            }
        });

        mqttClient.connect(1883, "localhost").onSuccess(ok -> {
            metrics.incrementMqttClients();
            startup.countDown();
            log.info("mqtt client connected {}", ok);
        }).onFailure(bad -> {
            log.error(bad.getMessage(), bad.getCause());
            throw new RuntimeException(bad);
        });

        mqttClient.publishCompletionHandler(pub -> log.trace("published {}", pub));
        mqttClient.subscribeCompletionHandler(comp -> log.trace("subscribed {}", comp));
        mqttClient.unsubscribeCompletionHandler(unsub -> log.trace("unsubscribed {}", unsub));

        startup.await(10, TimeUnit.SECONDS);

        CountDownLatch subscription = new CountDownLatch(1);

        mqttClient.publishHandler(pub -> {
            log.info("client received message: topic: {}, payload: {}, qos: {}", pub.topicName(), pub.payload().toString(Charset.defaultCharset()), pub.qosLevel());
            try {
                template.convertAndSend("/topic/" + pub.topicName(), String.format("%s", pub.payload().toString(Charset.defaultCharset())));
            } catch (MessagingException websocketError) {
                log.error(websocketError.getMessage(), websocketError);
            }
        }).subscribe(Map.of(
                "heartbeat", MqttQoS.AT_LEAST_ONCE.value(),
                "messages", MqttQoS.AT_LEAST_ONCE.value()))
                .onSuccess(subscribed -> {
                    log.trace("subscription succeeded {}", subscribed);
                    subscription.countDown();
                })
                .onFailure(subfailed -> {
                    log.error(subfailed.getMessage(), subfailed.getCause());
                    throw new RuntimeException(subfailed);
                });

        subscription.await(10, TimeUnit.SECONDS);

        return mqttClient;
    }
}
