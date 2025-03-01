package jmc.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import jmc.metrics.MqttMetrics;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
@Slf4j
public class WebSocketController {
    @Autowired
    private MqttClient mqttClient;
    @Autowired
    private MqttMetrics metrics;

    @Autowired
    ObjectMapper om;

    @MessageMapping("/invoke")
    public void invoke(String message) throws Exception {
        Map<String, Object> messageJson = om.readValue(message, new TypeReference<>() {
        });
        String messagePayload = (String) messageJson.get("message");
        messageJson.put("sha1", DigestUtils.sha1Hex(messagePayload.getBytes(StandardCharsets.UTF_8)));
        Buffer buffer = Buffer.buffer(om.writeValueAsBytes(messageJson));
        long mqttStartTime = System.currentTimeMillis();
        mqttClient.publish("messages", buffer, MqttQoS.AT_LEAST_ONCE, false, false)
            .onFailure(failed ->
                        metrics.incrementPublishFailure("messages"))
            .onComplete(publish ->
                metrics.recordPublishTime("messages", System.currentTimeMillis() - mqttStartTime, TimeUnit.MILLISECONDS));
    }
}
