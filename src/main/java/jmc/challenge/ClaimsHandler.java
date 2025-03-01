package jmc.challenge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Controller
@Slf4j
public class ClaimsHandler {
    @Autowired
    private MqttClient mqttClient;

    @Autowired
    ObjectMapper om;

    @MessageMapping("/claims")
    public void produceChallenge(String claim) throws JsonProcessingException {
        log.info("claim: {}", claim);
        Map<String, Object> claimJson = om.readValue(claim, new TypeReference<>() {
        });
        String claimRequest = (String) claimJson.get("claim");
        claimJson.put("sha1", DigestUtils.sha1Hex(claimRequest.getBytes(StandardCharsets.UTF_8)));
        Buffer buffer = Buffer.buffer(om.writeValueAsBytes(claimJson));
        Optional.ofNullable(claimJson.get("claim")).ifPresent(_ -> {
            if (mqttClient.isConnected()) {
                mqttClient.publish("claims", buffer, MqttQoS.EXACTLY_ONCE, false, false);
            }
        });
    }
}
