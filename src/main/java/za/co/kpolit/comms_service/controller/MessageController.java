package za.co.kpolit.comms_service.controller;

import org.springframework.web.bind.annotation.*;
import za.co.kpolit.comms_service.service.WhatsAppService;
import za.co.kpolit.comms_service.model.WhatsAppMessage;
import za.co.kpolit.comms_service.repository.WhatsAppMessageRepository;
import java.util.List;

@RestController
@RequestMapping("/whatsapp/messages")
@CrossOrigin(origins = "*") // ✅ allow Flutter web/mobile to access
public class MessageController {

    private final WhatsAppMessageRepository messageRepository;

    public MessageController(WhatsAppMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    // 📨 Get all messages
    @GetMapping
    public List<WhatsAppMessage> getAllMessages() {
        return messageRepository.findAll();
    }

    // 🔍 Get messages for a specific contact
    @GetMapping("/{phone}")
    public List<WhatsAppMessage> getMessagesByPhone(@PathVariable String phone) {
        return messageRepository.findAll().stream()
                .filter(m -> m.getFromNumber().equals(phone) || m.getToNumber().equals(phone))
                .toList();
    }
}