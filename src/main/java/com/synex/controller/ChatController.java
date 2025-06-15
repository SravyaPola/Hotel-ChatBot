package com.synex.controller;

import com.synex.domain.ChatRequest;
import com.synex.domain.ChatResponse;
import com.synex.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
        @RequestBody ChatRequest req,
        @RequestHeader(name = "Accept-Language", required = false, defaultValue = "en")
            String language
    ) {
        // push the header value into your request DTO
        req.setLanguage(language);

        // now call the single-arg service method
        ChatResponse serviceResp = chatService.chat(req);

        // wrap it back into your outbound DTO
       
        return ResponseEntity.ok(serviceResp);
    }
}
