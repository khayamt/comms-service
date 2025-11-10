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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WhatsAppService {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);

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
        String url = apiUrl + String.format("/%s/messages", phoneNumberId);
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", text)
        );
        logger.info("📤 Sending WhatsApp message to: {}", to);
        logger.info("Payload: {}", payload);


        return webClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class).flatMap(body -> {
                            logger.info("❌ WhatsApp API error: " + response.statusCode());
                            logger.info("Response body: " + body);
                            return Mono.error(new RuntimeException("WhatsApp send failed: " + body));
                        })
                )
                .bodyToMono(String.class)
                .doOnNext(resp -> logger.info("✅ WhatsApp API response: {}", resp))
                .doOnError(error -> {
                    logger.info("❌ Error sending WhatsApp message to " + to);
                    logger.info("Error Type: " + error.getClass().getSimpleName());
                    logger.info("Error Message: " + error.getMessage());
                })
                .onErrorResume(error -> {
                    String fallbackMsg = "Failed to send message: " + error.getMessage();
                    logger.info("⚠️ Returning fallback response: " + fallbackMsg);
                    return Mono.just(fallbackMsg);
                });
    }


    public String sendTextMessageBlocking(String phoneNumberId, String to, String text) {
        String url = String.format("https://graph.facebook.com/v20.0/%s/messages", phoneNumberId);

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", text)
        );

        System.out.println("📤 Sending WhatsApp message to: " + to);
        System.out.println("Payload: " + payload);

        try {
            String response = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, res ->
                            res.bodyToMono(String.class).flatMap(body -> {
                                System.err.println("❌ API Error: " + res.statusCode());
                                System.err.println("Response body: " + body);
                                return Mono.error(new RuntimeException(body));
                            })
                    )
                    .bodyToMono(String.class)
                    .block(); // <--- blocks and returns String

            System.out.println("✅ Response: " + response);
        } catch (Exception e) {
            System.err.println("❌ Exception: " + e.getMessage());
            e.printStackTrace();
        }
        return "Success";
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

        logger.info("📤 Sending WhatsApp {} remplate message to: {}", template, to);
        logger.info("Payload: {}", payload);

        return webClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class).flatMap(body -> {
                            logger.info("❌ WhatsApp Template API error: " + response.statusCode());
                            logger.info("Response body: " + body);
                            return Mono.error(new RuntimeException("WhatsApp send failed: " + body));
                        })
                )
                .bodyToMono(String.class)
                .doOnNext(resp -> logger.info("✅ WhatsApp API response: {}", resp))
                .doOnNext(resp -> System.out.println("WhatsApp API response: " + resp));
    }

}
