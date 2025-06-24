package com.synex.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synex.domain.ActionResult;
import com.synex.domain.ConversationState;
import com.synex.service.NLPService3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OpenAiService3 implements NLPService3 {

	private final OpenAiService client;
	private final ObjectMapper mapper = new ObjectMapper();

	private static final String MULTI_LANG_SYSTEM_PROMPT = """
			You are a multilingual hotel-booking assistant.
			For *each* incoming user message, ignore all previous turns,
			detect the language of *only* that message,
			and reply in exactly that language.
			If the user’s message is in English, *you must reply in English*.
			If it’s in French, reply in French, etc.
			If it’s in Spanish, reply in Spanish, etc.
			Do not reveal any internal translation steps.
			""";

	public OpenAiService3(@Value("${openai.api.key}") String apiKey) {
		this.client = new OpenAiService(apiKey);
	}

	// Auto‐detect version, for JSON extraction where language doesn’t matter:
	private List<ChatMessage> withSystemPrompt(String userContent) {
		return List.of(new ChatMessage("system", MULTI_LANG_SYSTEM_PROMPT), new ChatMessage("user", userContent));
	}

	// Language‐aware version, for follow-ups & replies:
	private List<ChatMessage> withSystemPrompt(String userContent, ConversationState st) {
		String lang = Optional.ofNullable(st.getLanguage()).filter(s -> !s.isBlank()).orElse(null);

		String system;
		if (lang != null) {
			// map ISO code -> display name
			String display = switch (lang.toLowerCase()) {
			case "es", "español" -> "Spanish";
			case "fr", "français" -> "French";
			case "de", "deutsch" -> "German";
			default -> "English";
			};
			system = "You are a hotel-booking assistant. Always reply in " + display
					+ ". Do not reveal internal translation steps.";
		} else {
			system = MULTI_LANG_SYSTEM_PROMPT; // your old auto-detect prompt
		}

		return List.of(new ChatMessage("system", system), new ChatMessage("user", userContent));
	}

	/** Extract exactly the named slots by asking GPT to return only JSON. */
	@Override
	public Map<String, String> fillSlots(List<String> slots, String userMessage) {
		try {
			// 1) Build a hint for date slots
			String prompt = """
							    Extract these fields as JSON keys: %s
					User said: "%s".
					Respond with only a JSON object, and:
					- Figure out that, if the user provide state or city or both, if one is given identify as state or city then fill corresponding field only,
					dont assume city or state by your own for the missing city or state field.
					- if both state and city given ,fill both correspondingly.
					- Always convert the name of the state to two letter, and always in USA.
					- For any date field like checkIn or checkOut, always return the date in ISO format: YYYY-MM-DD.
					- If the user's date is ambiguous or missing, set the value to null.
					- If the user gives only a month/day, assume the current year 2025.
					- For "guests", if the user mentions adults, children, etc., sum them and return as a single integer.
					- If "guests" is ambiguous or not provided, set it to null.
					- If a field is a list, return as a list-based.
					If action is "filtered_hotels" or "hotelList":
					  - List the matching hotels with their key features (star, price, main amenities).
					  - Ask user to select one for details, or to continue.
					If action is "filter_failed":
					  - Explain that no hotels matched the filters, so you are showing all available hotels in that location.
					  - Offer to let them change filters or pick from the available list.
					If the user gives a max price above $500, set it to 500, and politely inform the user of this limit in your reply. Do not ask the user to enter it again.
								- If the user gives random spelling for city, just adjust and give the first letter of the city as capital and rest as small.
					- If you already know either city, state, or both, do not ask for them again, unless the user specifically gives a new location.
					- If you have only state, search by state and dont ask for city.
					- If you have only city, search by city and dont ask for state.
					- Never ask for city/state again if already any one of the value for state or city is already set, unless the user changes it.
					- On no results either by cit or state or city and state, you give one tailored apology + optional “try a different location?” instead of looping.
					- As soon as you have either a city OR a state, search for hotels using that information.
					- If the user provides both, use both to narrow down the results.
					- Do NOT keep asking for city or state if either one is already set, unless the user gives a new location.
					- If there are no hotels in the current city/state, politely ask the user if they want to try a different location.
					- Always accept updates if the user gives a new city or state at any time.
					-NEVER ask the user to re-enter city or state if you already presented hotels for their selected location. Only ask to choose a hotel by number or name.
					- If the user wants to narrow results, they will mention a specific city.
					- Once you have found hotels by state, DO NOT ask the user for city again.
					- If the user refers to a hotel by name, extract its name as "hotelName".
					- Always ensure that check-out is after check-in. Prompt user again if invalid.
					- For **any** slot whose name suggests a confirmation (e.g. `confirmBooking`, `confirmPayment`, `confirmAnother`),
					interpret common variants (“yes”, “no”, “sure”, “ok”, “paid”, “not”, etc.) **and output** a JSON **boolean** (`true` or `false`).
					- Translate the following text into the user’s requested language.
					If the text isn’t English, first translate it to English yourself, then translate that English into the target language.
					Output only the final translation.
					- You are a multilingual hotel-booking assistant.  Whenever a user speaks to you, detect the language they’re using and reply **in that same language**.
					 You may do any internal work (for example, translate into English to compute the answer), but **only** output your final message in the user’s language.
					 Don’t ever output internal translation steps.
					- If the user provides only a state name, fill **only** the `state` slot; set `city` to null.

							    """
					.formatted(String.join(", ", slots), userMessage.replace("\"", "\\\""));

			// 3) Call the LLM
//			ChatCompletionRequest req = ChatCompletionRequest.builder().model("gpt-4o-mini")
//					.messages(List.of(new ChatMessage("user", prompt))).build();
			ChatCompletionRequest req = ChatCompletionRequest.builder().model("gpt-4o-mini")
					.messages(withSystemPrompt(prompt)).build();

			ChatCompletionResult res = client.createChatCompletion(req);
			String json = res.getChoices().get(0).getMessage().getContent().trim();

			if (json.startsWith("```")) {
				int start = json.indexOf('\n');
				int end = json.lastIndexOf("```");
				if (start >= 0 && end > start) {
					json = json.substring(start + 1, end).trim();
				}
			}

			System.out.println("Sanitized LLM output: " + json);

			// 4) Parse into a Map<String,String> and keep only the slots you asked for
			Map<String, Object> all = mapper.readValue(json, new TypeReference<Map<String, Object>>() {
			});
			Map<String, String> result = new HashMap<>();
			for (String key : all.keySet()) {
				Object val = all.get(key);
				if (val instanceof List<?> list) {
					result.put(key, list.stream().map(Object::toString).collect(Collectors.joining(",")));
				} else if (val != null) {
					result.put(key, val.toString());
				} else {
					result.put(key, null);
				}
			}
			result.keySet().retainAll(slots);
			return result;

		} catch (Exception e) {
			// on any error, return empty so the orchestrator will re-ask
			e.printStackTrace();
			return Collections.emptyMap();
		}
	}

	/** Generate one follow-up question for whatever slots remain missing. */
	@Override
	public String generateQuestion(List<String> missingSlots, ConversationState ctx) {
		String context = buildContextString(ctx);
		String prompt = """
				You are a friendly hotel‐booking assistant.
				Context so far: %s
				Ask exactly one concise question to elicit: %s
				Do NOT ask for city or state if the user has already provided city or state.
				Only ask for the missing slots listed.

				You are a **multilingual hotel-booking assistant**.  **Always** obey these formatting rules for **every** outgoing message:

					Follow this rules mandatory
					Markdown only—no HTML or plain text.
					Hotel lists service pricing must be a numbered list
					Booking should look like summary, not the paragraph
					Always end with a clear question or call-to-action in the user’s language.
					Never add, remove or reorder any data (stars, prices, dates, arrows, punctuation, list order).
					Vary your phrasing when asking the same thing in million ways
				"""
				.formatted(context, missingSlots);

//		ChatCompletionRequest req = ChatCompletionRequest.builder().model("gpt-4o-mini")
//				.messages(List.of(new ChatMessage("system", prompt))).build();

//		
		ChatCompletionRequest req = ChatCompletionRequest.builder().model("gpt-4o-mini")
				.messages(withSystemPrompt(prompt, ctx)) // <-- two-arg
				.build();

		ChatCompletionResult res = client.createChatCompletion(req);
		return res.getChoices().get(0).getMessage().getContent().trim();
	}

	/** Render a 1–2 sentence reply from a structured ActionResult. */
	@Override
	public String renderActionReply(ActionResult result, ConversationState ctx) {
		String action = result.actionName();
		Map<String, Object> data = result.data();
		String prompt;

		switch (action) {
		case "hotelList_location":
			prompt = """
					You are a helpful hotel booking assistant. The user has provided a city and/or state.
					List the available hotels in a friendly way, mention city/state if present.
					DO NOT ask the user to pick a hotel. DO NOT mention choosing a number.
					Just summarize what’s available.
					Data: %s
					""".formatted(result.data());
			break;

		case "hotelList_filtered":
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> hotelsFiltered = (List<Map<String, Object>>) data.get("hotelList_filtered");
			if (hotelsFiltered == null)
				hotelsFiltered = List.of();

			prompt = """
					The user has provided some filters (such as price, stars, or amenities).
					List the hotels matching these filters in a friendly way, and now invite the user to choose a hotel by number or name.
					Data: %s
					"""
					.formatted(Map.of("hotelList_filtered", hotelsFiltered));
			break;

		case "no_hotels_location":
			prompt = """
					INSTRUCTION:
					When type is "no_hotels_location":
					Apologize to the user that no hotels were found for the city (data.city) and state (data.state).
					Ask if they want to try a different location.
					Only use data provided, do not add extra information.

					DATA:
					%s
					""".formatted(data);
			break;

		case "filter_failed":
			prompt = """
					The user tried to filter hotels (by price, stars, or amenities), but there were no matches.
					Politely explain that no hotels matched their filters.
					Then, show the hotels still available in the chosen city and/or state, and ask the user to pick one by number or name.
					List the hotels in a friendly way, mention city/state, and invite them to choose.
					Data: %s
					"""
					.formatted(result.data());
			break;
		case "hotelList":
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> hotels = (List<Map<String, Object>>) data.get("hotels");
			if (hotels == null)
				hotels = List.of();

			prompt = """
					Here are the hotels available based on your previous selections.
					Please choose a hotel by number or name.
					Data: %s
					""".formatted(Map.of("hotels", hotels));
			break;

		case "hotelDetails":
			prompt = """
					You are a hotel booking assistant.
					The user selected a hotel: %s.
					Show the available services: %s.
					Ask if they want to add services or proceed to booking.
					""".formatted(data.get("name"), data.get("services"));
			break;
		case "extractServices":
			@SuppressWarnings("unchecked")
			List<String> options = (List<String>) result.data().get("availableServices");
			String input = (String) result.data().get("userInput");
			prompt = """
					You have these available services: %s.
					The user said: "%s"
					Extract only the services from that list which the user explicitly mentioned.
					Return exactly one JSON object with a key "serviceNames" whose value is an array of matching names.
					If they didn’t mention any, return {"serviceNames":[]} and nothing else.
					""".formatted(String.join(", ", options), input.replace("\"", "\\\""));
			break;
		case "serviceNotMatched":
			List<String> availableServices = safeList(data, "availableServices");
			String services = availableServices.stream().map(s -> "- " + s).collect(Collectors.joining("\n"));
			prompt = """
					The service(s) you requested could not be matched to any available options for the selected hotel.
					Here are the available services you can choose from:
					%s

					(If available, prices will be shown.)

					Please pick one or more services by name from the list above, or reply "none" if you do not want any additional services.

					Data: %s
					"""
					.formatted(services, data);
			break;

		case "serviceSubtotal":
			prompt = """
					INSTRUCTION:
					When type is "serviceSubtotal":
					List all the services in data.picked.
					Show the subtotal in data.subtotal.
					Politely ask if the user wants to continue to room selection or add more services.
					Only use data provided, do not invent information.

					DATA:
					%s
					""".formatted(data);
			break;

		case "roomTypeList":
			prompt = """
					INSTRUCTION:
					When type is "roomTypeList":
					List all available room types from data.availableRooms.
					Ask the user to pick a room type by name or number.
					Only use data provided, do not add extra information.
					Try to match from the list with the details provide by the user even if they provide some spell mistake or case or fuzzy .Pick only one.
					DATA:
					%s
					"""
					.formatted(data);
			break;
		case "roomTypeNotMatched":
			List<String> availableRooms = safeList(data, "availableRooms");
			String tried = (String) data.getOrDefault("triedName", "");
			prompt = """
					The user tried to select the room type: "%s",
					but it was not found among available options.
					Please politely list the available room types: %s.
					Ask the user to pick one of these.
					""".formatted(tried, String.join(", ", availableRooms));
			break;

		case "askNumRooms":
			prompt = """
					Ask user for booking number of rooms
					""";
			break;

		case "confirmRooms":
			prompt = """
					You have selected %s room%s.
					What is the name for the reservation?
					""".formatted(data.get("noRooms"),
					(Integer.parseInt(data.get("noRooms").toString()) > 1 ? "s" : ""));
			break;
		case "showSubtotal":
			prompt = """
					INSTRUCTION:
					When type is "showSubtotal":
					Present a booking summary using all fields in data (hotel name, room, check-in, check-out, guests, rooms, services, subtotal, customerName).
					Ask the user to confirm the booking (yes/no).
					Only use what is in data, do not add extra information.

					DATA:
					%s
					"""
					.formatted(data);
			break;

		case "createBooking":
			prompt = """
					You are a hotel booking assistant.
					The booking is created. The subtotal is $%s.
					Ask if the user wants to proceed to payment.
					""".formatted(data.get("subtotal"));
			break;

		case "bookingConfirmed":
			prompt = """
					INSTRUCTION:
					When type is "bookingConfirmed":
					Thank the user, confirm the reservation; list all details (hotel, room, check-in, check-out, guests, rooms, services, subtotal, bookingId, customerName) from data.
					Advise the user to save their booking ID.
					Ask if they'd like to book another or need further help.
					Only use what is in data, do not invent information.

					DATA:
					%s
					"""
					.formatted(data);
			break;

		case "startNewBooking":
			prompt = """
					INSTRUCTION:
					When type is "startNewBooking":
					Invite the user to start a new hotel search, ask for the city and dates.
					Only use data provided.

					DATA:
					%s
					""".formatted(data);
			break;

		case "proceedToPayment":
			prompt = """
					INSTRUCTION:
					The user does not want to start another booking and is ready to proceed to payment for their current booking.
					Politely ask the user to provide payment (simulated, if you're not collecting real payments) or say 'paid' to confirm.
					DATA:
					%s
					"""
					.formatted(data);
			break;
		case "payment":
			prompt = """
					INSTRUCTION:
					When type is "payment":
					Mention the payment status in data.paid and (if present) booking ID from data.bookingId.
					Thank the user for their payment.
					Ask if they'd like to leave feedback.
					Only use data provided, do not add information.

					DATA:
					%s
					""".formatted(data);
			break;

		case "feedbackSaved":
			prompt = """
					Thank the user for their feedback and offer further assistance.
					""";
			break;
		case "endBookingSession":
			prompt = """
					INSTRUCTION:
					The booking session is now complete (after feedback is collected).
					Thank the user by name (data.customerName if present).
					Politely end the session and let them know they can start a new booking any time.
					DATA:
					%s
					""".formatted(data);
			break;

		case "askAgentInfo":
			prompt = """
					INSTRUCTION:
					When type is "askAgentInfo":
					You are a friendly assistant connecting the user to a live support agent.
					Ask the user for their full name and phone number so we can reach them.
					Output only that one concise question.
					""";
			break;

		case "agentInfoReceived":
			prompt = """
					INSTRUCTION:
					When type is "agentInfoReceived":
					Acknowledge that you’ve received their info.
					Politely say an agent will reach out shortly.
					Then invite them: “How else can I help you today?” (or similar).
					Output only that one message.
					""";
			break;

		case "askContinueBooking":
			prompt = """
					INSTRUCTION:
					When type is "askContinueBooking":
					Ask the user if they’d like to continue with their hotel booking or end the chat.
					Expect a yes/no response.
					Output only that one question.
					""";
			break;

		case "endAgentChat":
			prompt = """
					INSTRUCTION:
					When type is "endAgentChat":
					Thank the user for contacting support and let them know you’re here if they need anything else.
					Politely close the conversation.
					Output only that one message.
					""";
			break;

		default:
			prompt = """
					INSTRUCTION:
					You are a helpful assistant. I ran action "%s" with data shown below.
					Reply in one or two friendly sentences using only data fields.
					Markdown only—no HTML or plain text.
					Hotel lists must be a numbered list
					Always end with a clear question or call-to-action in the user’s language.
					Never add, remove or reorder any data (stars, prices, dates, arrows, punctuation, list order).
					Vary your phrasing when asking the same thing twice
					DATA:
					%s
					""".formatted(action, data);
		}

//		ChatCompletionRequest req = ChatCompletionRequest.builder().model("gpt-4o-mini")
//				.messages(List.of(new ChatMessage("system", prompt))).build();

//		ChatCompletionRequest req = ChatCompletionRequest.builder().model("gpt-4o-mini")
//				.messages(withSystemPrompt(prompt)).build();

		ChatCompletionRequest req = ChatCompletionRequest.builder().model("gpt-4o-mini")
				.messages(withSystemPrompt(prompt, ctx)) // <-- two-arg
				.build();

		ChatCompletionResult res = client.createChatCompletion(req);
		return res.getChoices().get(0).getMessage().getContent().trim();
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> safeList(Map<String, Object> data, String key) {
		Object val = data.get(key);
		if (val == null)
			return List.of();
		if (val instanceof List<?>)
			return ((List<?>) val).stream().map(x -> (T) x).collect(Collectors.toList());
		return List.of((T) val);
	}

	/** A simple one-line summary of the conversation state for context. */
	private String buildContextString(ConversationState ctx) {
		return "stage=" + ctx.getStage() + ", city=" + ctx.getCity() + ", checkIn=" + ctx.getCheckIn() + ", checkOut="
				+ ctx.getCheckOut() + ", guests=" + ctx.getGuests();
	}

	@Override
	public List<String> translateList(List<String> items, String targetLang) {
		// 1) to JSON
		String jsonList;
		try {
			jsonList = mapper.writeValueAsString(items);
		} catch (JsonProcessingException e) {
			return items;
		}

		// 2) ask GPT to translate the JSON array
		String prompt = String.format("You are a localization assistant.%n" + "%n"
				+ "Input: a JSON array of suggestion strings in English.%n" + "%n" + "Task:%n"
				+ "- Translate all *free-text* parts into %s.%n"
				+ "- Preserve **exactly** every numeric value, ISO date (YYYY-MM-DD), arrow (→), currency symbol, range notation, punctuation, and formatting.%n"
				+ "- Do not add, remove, merge, split, reorder, or modify any array items or their structure.%n" + "%n"
				+ "Output:%n" + "Return *only* the translated JSON array of the same length and order—no extra text.",
				targetLang);

		ChatCompletionRequest req = ChatCompletionRequest.builder().model("gpt-4o-mini")
				.messages(List.of(new ChatMessage("system", prompt), new ChatMessage("user", jsonList))).build();

		String res = client.createChatCompletion(req).getChoices().get(0).getMessage().getContent().trim();

		// 3) strip code fences
		if (res.startsWith("```")) {
			res = res.replaceAll("(?s)```.*?```", "").trim();
		}

		// 4) parse JSON back to Java
		try {
			return mapper.readValue(res, new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
			return items;
		}
	}
}
