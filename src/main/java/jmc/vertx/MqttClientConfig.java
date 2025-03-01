package jmc.vertx;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttServer;
import jmc.metrics.MqttMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@Configuration
@Slf4j
public class MqttClientConfig {

    @Autowired
    MqttMetrics metrics;
    @Bean("mqttClient")
    public MqttClient mqttClient(Vertx vertx, MqttServer mqttServer,  SimpMessagingTemplate template) throws InterruptedException {
        MqttClient mqttClient = MqttClient.create(vertx);
        // https://github.com/spring-projects/spring-framework/issues/2329
        CountDownLatch startup = new CountDownLatch(1);
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
            template.convertAndSend("/topic/" + pub.topicName(), String.format("%s", pub.payload().toString(Charset.defaultCharset())));
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
