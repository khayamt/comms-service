package za.co.kpolit.comms_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class WhatsAppService {
    private final WebClient webClient;

    @Value("${whatsapp.access-token}")
    private String accessToken;

    //@Value("${whatsapp.phone-number-id}")
    //private String phoneNumberId;

    @Value("${whatsapp.api-url}")
    private String apiUrl = "https://graph.facebook.com/v22.0";

    public WhatsAppService(WebClient.Builder builder) {
        this.webClient = builder.baseUrl(apiUrl).build();
    }

    public Mono<String> sendTextMessage(String phoneNumberId, String to, String text) {
        String url = String.format("/%s/messages", phoneNumberId);
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", text)
        );
        System.out.println("📤 Sending WhatsApp message to: " + to);
        System.out.println("Payload: " + payload);


        return webClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(resp -> System.out.println("✅ WhatsApp API response: " + resp))
                .doOnError(error -> {
                    System.err.println("❌ Error sending WhatsApp message to " + to);
                    System.err.println("Error Type: " + error.getClass().getSimpleName());
                    System.err.println("Error Message: " + error.getMessage());
                })
                .onErrorResume(error -> {
                    String fallbackMsg = "Failed to send message: " + error.getMessage();
                    System.err.println("⚠️ Returning fallback response: " + fallbackMsg);
                    return Mono.just(fallbackMsg);
                });
    }

    public Mono<String> sendTemplateMessage(String phoneNumberId, String to, String template) {
        String url = String.format("/%s/messages", phoneNumberId);
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "template",
                "template", Map.of(
                        "name", template,
                        "language", Map.of("code", "en_US")));

        return webClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(resp -> System.out.println("WhatsApp API response: " + resp));
    }

}
