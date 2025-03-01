package jmc.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MqttMetrics {
    private final Gauge clientConnectionCounter;
    private final Map<String, Counter> publishFailureCounters;
    private final Map<String, Timer> publishTimers;
    private final AtomicLong conns;
    private final Counter pubAckTimeoutHandler;

    public MqttMetrics(MeterRegistry meterRegistry) {
        conns = new AtomicLong(0);
        clientConnectionCounter = Gauge.builder("mqtt_client_conns", conns, AtomicLong::doubleValue)
                .description("MQTT Client connections")
                .tag("mqtt", "client")
                .register(meterRegistry);

        pubAckTimeoutHandler = Counter.builder("mqtt_client_puback_timeouts")
                .description("MQTT PUBACK timeouts")
                .tag("mqtt", "puback")
                .register(meterRegistry);

        publishTimers = Map.of(
                "heartbeat", mqttPublishLatency(meterRegistry, "mqtt_publish_latency", "heartbeat"),
                "messages", mqttPublishLatency(meterRegistry, "mqtt_publish_latency", "messages"));

        publishFailureCounters = Map.of(
                "heartbeat", mqttPublishFailure(meterRegistry, "mqtt_publish_failure", "heartbeat"),
                "messages", mqttPublishFailure(meterRegistry, "mqtt_publish_failure", "messages"));
    }

    private Counter mqttPublishFailure(MeterRegistry meterRegistry, String timerName, String topic) {
        return Counter.builder(timerName)
                .description("MQTT Publish Failure")
                .tag("topic", topic)
                .register(meterRegistry);
    }

    private static Timer mqttPublishLatency(MeterRegistry meterRegistry, String timerName, String topic) {
        return Timer.builder(timerName)
                .publishPercentiles(0.5, 0.95)
                .publishPercentileHistogram()
                .percentilePrecision(3)
                .tags("topic", topic)
                .description("duration of " + topic + " publish")
                .serviceLevelObjectives(Duration.ofMillis(1), Duration.ofMillis(5), Duration.ofMillis(10), Duration.ofMillis(25))
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofMillis(100))
                .register(meterRegistry);
    }

    public void incrementMqttClients() {
        conns.getAndIncrement();
        clientConnectionCounter.measure();
    }

    public void decrementMqttClients() {
        conns.getAndDecrement();
        clientConnectionCounter.measure();
    }

    public void recordPublishTime(String topic, long elapsed, TimeUnit timeUnit) {
        Optional.ofNullable(publishTimers.get(topic))
                .ifPresent(t -> t.record(elapsed, timeUnit));
    }

    public void incrementPublishFailure(String topic) {
        Optional.ofNullable(publishFailureCounters.get(topic))
                .ifPresent(Counter::increment);
    }

    public void incrementPubackTimeouts() {
        pubAckTimeoutHandler.increment();
    }
}
