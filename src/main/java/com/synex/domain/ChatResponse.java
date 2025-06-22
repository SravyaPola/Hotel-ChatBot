package com.synex.domain;

import java.util.List;

//outgoing JSON: { "reply": "...", "escalateToHuman": false, "suggestions": [] }
public class ChatResponse {

	private String message;
	private boolean endOfConversation;
	private List<String> suggestions;
	private ConversationState state;

	public ChatResponse() {
	}

	public ChatResponse(String message, boolean endOfConversation, List<String> suggestions, ConversationState state) {
		this.message = message;
		this.endOfConversation = endOfConversation;
		this.suggestions = suggestions;
		this.state = state;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isEndOfConversation() {
		return endOfConversation;
	}

	public void setEndOfConversation(boolean endOfConversation) {
		this.endOfConversation = endOfConversation;
	}

	public List<String> getSuggestions() {
		return suggestions;
	}

	public void setSuggestions(List<String> suggestions) {
		this.suggestions = suggestions;
	}

	public ConversationState getState() {
		return state;
	}

	public void setState(ConversationState state) {
		this.state = state;
	}

}