package com.synex.domain;

// incoming JSON: { "message": "...", "language": "en" }
public class ChatRequest {
	private String message;
	private String language;
	private DialogState state;

	public ChatRequest() {
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String m) {
		this.message = m;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String l) {
		this.language = l;
	}

	public DialogState getState() {
		return state;
	}

	public void setState(DialogState state) {
		this.state = state;
	}
}
