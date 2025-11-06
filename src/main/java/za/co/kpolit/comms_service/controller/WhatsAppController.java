package za.co.kpolit.comms_service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import za.co.kpolit.comms_service.service.WhatsAppService;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/webhook")
public class WhatsAppController {
    private final WhatsAppService whatsappService;
    public WhatsAppController(WhatsAppService whatsappService) {
        this.whatsappService = whatsappService;
    }

    @Value("${whatsapp.verify-token}")
    private String verifyToken;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(@RequestParam(name = "hub.mode", required = false) String mode,
                                                @RequestParam(name = "hub.verify_token", required = false) String token,
                                                @RequestParam(name = "hub.challenge", required = false) String challenge) {
        System.out.println("Verifying WhatsApp Webhook: " + challenge);
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        } else {
            return ResponseEntity.status(403).body("Verification failed");
        }
    }

    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody Map<String, Object> body) {
        System.out.println("Received WhatsApp Webhook: " + body);

        try {
            List<Map<String, Object>> entryList = (List<Map<String, Object>>) body.get("entry");
            if (entryList != null && !entryList.isEmpty()) {
                Map<String, Object> entry = entryList.get(0);
                List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
                if (changes != null && !changes.isEmpty()) {
                    Map<String, Object> value = (Map<String, Object>) changes.get(0).get("value");
                    List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
                    if (messages != null && !messages.isEmpty()) {
                        Map<String, Object> message = messages.get(0);
                        String text = ((Map<String, String>) message.get("text")).get("body");
                        String from = (String) message.get("from");
                        System.out.println("📩 Message from " + from + ": " + text);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing webhook: " + e.getMessage());
        }

        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    @PostMapping("/receiveGeneric")
    public ResponseEntity<String> receiveGenericMessage(@RequestBody Map<String, Object> body) {
        System.out.println("Received Generic WhatsApp Webhook: " + body);
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
    @PostMapping("/sendText")
    public Mono<ResponseEntity<String>> sendWhatsAppMessage(@RequestParam String to, @RequestParam String text) {
        return whatsappService.sendTextMessage(to, text)
                .map(response -> ResponseEntity.ok("Message sent successfully: " + response))
                .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError()
                        .body("Failed to send message: " + e.getMessage())));
    }
    @PostMapping("/sendTemplate")
    public Mono<ResponseEntity<String>> sendTemplateWhatsAppMessage(@RequestParam String to, @RequestParam String template) {
        return whatsappService.sendTemplateMessage(to, template)
                .map(response -> ResponseEntity.ok("Message sent successfully: " + response))
                .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError()
                        .body("Failed to send message: " + e.getMessage())));
    }

}
