package com.synex.service;

import com.synex.domain.ParseResult;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class NLPService {
	
	private final OpenAiService openAi;
	
	public NLPService(OpenAiService openAi) {
	    this.openAi = openAi;
	  }

    // Use Jackson ObjectMapper for robust JSON parsing
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parses user input with LLM, extracting as many fields as possible.
     * @param userInput The latest user message (natural language)
     * @param currentStage The current state/stage of the dialog
     * @return ParseResult with as many slots filled as possible
     */
    public ParseResult parse(String userInput, String currentStage) {
        // Build a clear, robust prompt for the LLM
        String prompt =
            "You are a hotel booking assistant. Your task is to extract as many of these fields as possible from the user's message.\n"
            + "Return a single compact JSON object with these keys (use null for missing, and empty array for none):\n"
            + "{city, state, checkIn, checkOut, guests, minStars, minPrice, maxPrice, amenities, hotelName, hotelIndex, serviceIndices, confirm, rating, comments, serviceQuantityDays, roomType, noRooms, customerName, paymentMethod}\n"
            + "Dates must be yyyy-MM-dd. If user mentions 'adults' and 'children', sum for guests.\n"
            + "If user says 'no filters', set minStars, minPrice, maxPrice, amenities to null or empty.\n"
            + "yes/confirm/okay → confirm=true; no → confirm=false.\n"
            + "Output only the JSON.\n"
            + "User: \"" + userInput + "\"";

        // 1. Send the prompt to your LLM (OpenAI, Gemini, etc.) and get back JSON string
        String json = callLLM(prompt);

        // 2. Parse the returned JSON string into your ParseResult object
        ParseResult pr;
        try {
            pr = objectMapper.readValue(json, ParseResult.class);
        } catch (Exception e) {
            // On parse error, return a blank ParseResult and attach raw text for troubleshooting
            pr = new ParseResult();
            pr.setRawText(userInput);
            // You might want to log the error or rethrow as needed
        }

        // 3. Always store the raw user message in the result (for traceability)
        pr.setRawText(userInput);

        return pr;
    }

    /**
     * Dummy method—replace with your actual LLM API call.
     * Should return ONLY the compact JSON (no extra text, no explanations).
     */
    private String callLLM(String prompt) {
        // IMPLEMENT this with your LLM API of choice!
        // For example, with OpenAI Java SDK:
        // return openAiClient.createCompletion("gpt-3.5-turbo", prompt);
    	ChatCompletionRequest request = ChatCompletionRequest.builder()
    		      .model("gpt-4o-mini")             // or your desired model
    		      .messages(List.of(new ChatMessage("user", prompt)))
    		      .temperature(0.7)
    		      .build();

    		    ChatCompletionResult result = openAi.createChatCompletion(request);
    		    return result.getChoices().get(0).getMessage().getContent();
    }
}
