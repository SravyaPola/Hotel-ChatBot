package com.synex.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.synex.domain.*;
import com.synex.service.ChatService3;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

	@Autowired
	private ChatService3 chatService;

	@PostMapping
	public ChatResponse chat(@RequestBody ChatRequest request) {
		// If no state provided, ChatService will start at START
		return chatService.chat(request);
	}

}
