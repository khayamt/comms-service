package za.co.kpolit.comms_service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import za.co.kpolit.comms_service.service.WhatsAppService;
import za.co.kpolit.comms_service.service.ZapierService;
import za.co.kpolit.comms_service.model.WhatsAppMessage;
import za.co.kpolit.comms_service.repository.WhatsAppMessageRepository;
import java.util.Map;
import java.util.List;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/webhook")
public class WhatsAppController {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppController.class);
    private final WhatsAppService whatsappService;
    private final ZapierService zapierService;
    private final WhatsAppMessageRepository messageRepository;
    public WhatsAppController(WhatsAppService whatsappService, WhatsAppMessageRepository messageRepository, ZapierService zapierService) {
        this.whatsappService = whatsappService;
        this.messageRepository = messageRepository;
        this.zapierService = zapierService;
    }

    @Value("${whatsapp.verify-token}")
    private String verifyToken;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(@RequestParam(name = "hub.mode", required = false) String mode,
                                                @RequestParam(name = "hub.verify_token", required = false) String token,
                                                @RequestParam(name = "hub.challenge", required = false) String challenge) {
        logger.info("Verifying WhatsApp Webhook: " + challenge);
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        } else {
            return ResponseEntity.status(403).body("Verification failed");
        }
    }

    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody Map<String, Object> body) {
        logger.info("Received WhatsApp Webhook: " + body);

        try {
            List<Map<String, Object>> entryList = (List<Map<String, Object>>) body.get("entry");
            if (entryList != null && !entryList.isEmpty()) {
                Map<String, Object> entry = entryList.get(0);
                List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
                if (changes != null && !changes.isEmpty()) {
                    Map<String, Object> value = (Map<String, Object>) changes.get(0).get("value");
                    // ✅ Extract phone_number_id dynamically
                    Map<String, Object> metadata = (Map<String, Object>) value.get("metadata");
                    String phoneNumberId = (String) metadata.get("phone_number_id");
                    String to = (String) metadata.get("display_phone_number");

                    List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
                    if (messages != null && !messages.isEmpty()) {
                        Map<String, Object> message = messages.get(0);
                        String text = ((Map<String, String>) message.get("text")).get("body");
                        String from = (String) message.get("from");
                        String response = "Thank you for the message, I will respond latter. Still in development.";
                        logger.info("📩 Message from " + from + ": " + text);
                        saveMessage(from,to,"INCOMING","text",text,phoneNumberId);

                        response = String.valueOf(sendToZapier(phoneNumberId,from,text));
                        sendWhatsAppMessageBlocking(phoneNumberId,from,response);
                        logger.info("Responded to " + from + ": " + response);
                    }
                }
            }
        } catch (Exception e) {
            logger.info("Error parsing webhook: " + e.getMessage());
        }

        return ResponseEntity.ok("EVENT_RECEIVED");
    }
    @PostMapping("/forwardToZapier")
    public Mono<ResponseEntity<String>> sendToZapier(@RequestParam String phoneNumberId, @RequestParam String to, @RequestParam String text) {
        logger.info("Sending message Zapier " + to);
        //
        return zapierService.forwardToZapier(text)
                .map(resp -> ResponseEntity.ok("Forwarded to Zapier: " + resp))
                .onErrorResume(e -> {
                    logger.error("❌ Error forwarding to Zapier", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to forward: " + e.getMessage()));
                });
        //
    }

    @PostMapping("/receiveGeneric")
    public ResponseEntity<String> receiveGenericMessage(@RequestBody Map<String, Object> body) {
        logger.info("Received Generic WhatsApp Webhook: " + body);
        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    @PostMapping("/sendTextBlocking")
    public ResponseEntity<String> sendWhatsAppMessageBlocking(
            @RequestParam String phoneNumberId,
            @RequestParam String to,
            @RequestParam String text) {

        try {
            logger.info("📤 Sending message to {}", to);
            String response = whatsappService.sendTextMessageBlocking(phoneNumberId, to, text);
            saveMessage(phoneNumberId, to, "OUTGOING", "text", text, phoneNumberId);
            return ResponseEntity.ok("Message sent successfully: " + response);
        } catch (Exception e) {
            logger.error("❌ Error sending message: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to send message: " + e.getMessage());
        }
    }

    @PostMapping("/sendText")
    public Mono<ResponseEntity<String>> sendWhatsAppMessage(@RequestParam String phoneNumberId, @RequestParam String to, @RequestParam String text) {
        logger.info("Sending message to " + to);
        return whatsappService.sendTextMessage(phoneNumberId, to, text)
                .map(response ->
                {
                    logger.info("✅ Message sent successfully:....");
                    saveMessage(phoneNumberId,to,"OUTGOING","text",text,phoneNumberId);
                    return ResponseEntity.ok("Message sent successfully: " + response);
                })
                .onErrorResume(e ->
                {
                    System.err.println("❌ Error sending message: " + e.getMessage());
                    return Mono.just(ResponseEntity.internalServerError()
                        .body("Failed to send message: " + e.getMessage()));
                });
    }
    @PostMapping("/sendTemplate")
    public Mono<ResponseEntity<String>> sendTemplateWhatsAppMessage(@RequestParam String phoneNumberId, @RequestParam String to, @RequestParam String template) {
        return whatsappService.sendTemplateMessage(phoneNumberId,to, template)
                .map(response ->
                {   saveMessage(phoneNumberId,to,"OUTGOING","template",template,phoneNumberId);
                    return ResponseEntity.ok("Message sent successfully: " + response);
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError()
                        .body("Failed to send message: " + e.getMessage())));
    }
    private void saveMessage(String from, String to, String direction, String messageType, String messageText, String phoneNumberId)
    {

        // 🗄️ Save message to DB
        WhatsAppMessage msg = new WhatsAppMessage();
        msg.setFromNumber(from);
        msg.setToNumber(to);
        msg.setDirection(direction);
        msg.setMessageType(messageType);
        msg.setContent(messageText);
        msg.setTimestamp(Instant.now());
        msg.setPhoneNumberId(phoneNumberId);

        messageRepository.save(msg);

    }

}
