package jmc.heartbeat;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import jmc.metrics.MqttMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
@Slf4j
public class HeartbeatConfig {
    public static final long heartbeatInterval = 10 * 1000L;
    @Autowired
    MqttMetrics metrics;
    @Autowired
    private MqttClient mqttClient;
    @Scheduled(fixedDelay = heartbeatInterval)
    public void heartbeat() {
        long startTime = System.currentTimeMillis();
        if (mqttClient.isConnected()) {
            mqttClient.ping();
            mqttClient
                    .publish("heartbeat",
                            Buffer.buffer(Instant.now().toString()),
                            MqttQoS.AT_LEAST_ONCE,
                            false,
                            false)
                    .onFailure(failed -> {
                            log.error("MQTT publish failure on topic {}", "heartbeat", failed.getCause());
                            metrics.incrementPublishFailure("heartbeat");
                    })
                    .onSuccess(publish -> {
                            log.trace("MQTT publish failure on topic {}: {}", "heartbeat", publish);
                            metrics.recordPublishTime("heartbeat", System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS);
                    });
        } else {
            log.error("mqtt client disconnected");
            metrics.incrementPublishFailure("heartbeat");        }
    }
}
