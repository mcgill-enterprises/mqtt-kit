package jmc.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VertxConfig {
    @Bean("vertx")
    public Vertx vertx() {
        return Vertx.vertx(new VertxOptions()
                .setMetricsOptions(new MicrometerMetricsOptions().setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))));
    }
}
