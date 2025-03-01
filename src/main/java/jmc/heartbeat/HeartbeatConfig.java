package jmc.heartbeat;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;

@Configuration
@EnableScheduling
@Slf4j
public class HeartbeatConfig {
    @Autowired
    private MqttClient mqttClient;
    @Scheduled(fixedDelay = 10 * 1000)
    public void heartbeat() {
        mqttClient.publish("heartbeat", Buffer.buffer(Instant.now().toString()), MqttQoS.AT_LEAST_ONCE, false, false);
    }
}
