package jmc.vertx;

import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class MqttClientConfig {

    @Bean("mqttClient")
    public MqttClient mqttClient(Vertx vertx, MqttServer mqttServer) throws InterruptedException {
        MqttClient mqttClient = MqttClient.create(vertx);
        // https://github.com/spring-projects/spring-framework/issues/2329
        CountDownLatch startup = new CountDownLatch(1);
        mqttClient.connect(1883, "localhost").onSuccess(ok -> {
            startup.countDown();
            log.info("mqtt client connected {}", ok);
        }).onFailure(bad -> {
            log.error(bad.getMessage(), bad.getCause());
            throw new RuntimeException(bad);
        });
        mqttClient.subscribeCompletionHandler(comp -> log.info("subscribed {}", comp));
        startup.await(10, TimeUnit.SECONDS);
        return mqttClient;
    }
}
