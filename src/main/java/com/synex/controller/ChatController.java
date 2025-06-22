package com.synex.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.synex.service.ChatService;
import com.synex.service.ChatService2;
import com.synex.service.ChatService3;
import com.synex.service.RoomTypeService;
import com.synex.domain.*;

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
