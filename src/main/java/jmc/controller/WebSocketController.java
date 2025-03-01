package jmc.controller;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import jmc.metrics.MqttMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.util.concurrent.TimeUnit;

@Controller
@Slf4j
public class WebSocketController {
    @Autowired
    private MqttClient mqttClient;
    @Autowired
    private MqttMetrics metrics;
    @MessageMapping("/invoke")
    public void invoke(String message) throws Exception {
        long startTime = System.currentTimeMillis();
        mqttClient.publish("messages", Buffer.buffer(message), MqttQoS.AT_LEAST_ONCE, false, false)
            .onFailure(failed ->
                        metrics.incrementPublishFailure("messages"))
            .onComplete(publish ->
                metrics.recordPublishTime("messages", System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS));
    }
}
