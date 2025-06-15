package com.synex.domain;

import java.util.List;

//outgoing JSON: { "reply": "...", "escalateToHuman": false, "suggestions": [] }
public class ChatResponse {
	private final String reply;
	private final boolean escalateToHuman;
	private final List<String> suggestions;
	private DialogState nextState;

	

	public ChatResponse(String reply, boolean escalateToHuman, List<String> suggestions, DialogState nextState) {
		super();
		this.reply = reply;
		this.escalateToHuman = escalateToHuman;
		this.suggestions = suggestions;
		this.nextState = nextState;
	}

	public String getReply() {
		return reply;
	}

	public boolean isEscalateToHuman() {
		return escalateToHuman;
	}

	public List<String> getSuggestions() {
		return suggestions;
	}

	public DialogState getNextState() {
		return nextState;
	}

	public void setNextState(DialogState nextState) {
		this.nextState = nextState;
	}
}