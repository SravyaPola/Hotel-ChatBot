package com.synex.domain;

// incoming JSON: { "message": "...", "language": "en" }
public class ChatRequest {

	private String message;
	private String language; // e.g. "en"
	private ConversationState state; // null on first turn

	public ChatRequest() {
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public ConversationState getState() {
		return state;
	}

	public void setState(ConversationState state) {
		this.state = state;
	}

}
