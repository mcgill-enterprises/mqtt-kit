package jmc.heartbeat;

import io.vertx.mqtt.MqttClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@Slf4j
public class PingConfig {
    private static final long pingInterval = 10;
    @Autowired
    private MqttClient mqttClient;

    @Scheduled(fixedDelay = pingInterval)
    public void ping() {
        if (mqttClient.isConnected()) {
            mqttClient.ping();
        }
    }
}
