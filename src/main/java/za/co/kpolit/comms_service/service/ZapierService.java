package za.co.kpolit.comms_service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.kpolit.comms_service.configuration.AppProperties;

@Service
public class ZapierService {
    private static final Logger logger = LoggerFactory.getLogger(ZapierService.class);

    private final WebClient webClient;
    private final AppProperties properties;

    public ZapierService(WebClient.Builder builder, AppProperties properties) {
        this.webClient = builder.baseUrl(properties.getZapierUrl()).build();
        this.properties = properties;
    }

    public Mono<String> forwardToZapier(String jsonBody) {
        String zapierUrl = properties.getZapierUrl();
        if (zapierUrl == null || zapierUrl.isBlank()) {
            logger.info("Zapier URL not configured");
            return Mono.error(new IllegalStateException("Zapier URL not configured"));
        }

        return webClient.post()
                .uri(zapierUrl)
                .header("Content-Type", "application/json")
                .bodyValue(jsonBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(resp -> logger.info("✅ Zapier responded: {}", resp))
                .doOnError(err -> logger.error("❌ Failed to forward to Zapier", err));
    }
    public String forwardToZapierBlocking(String jsonBody) {
        String zapierUrl = properties.getZapierUrl();
        if (zapierUrl == null || zapierUrl.isBlank()) {
            String message = "Zapier URL not configured";
            logger.info(message);
            return message;
        }

        return webClient.post()
                .uri(zapierUrl)
                .header("Content-Type", "application/json")
                .bodyValue(jsonBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
