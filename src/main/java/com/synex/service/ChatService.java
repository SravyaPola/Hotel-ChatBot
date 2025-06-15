package com.synex.service;

import com.synex.domain.*;
import com.synex.repository.*;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

  private final EmbeddingService              embeddingService;
  private final OpenAiService                 openAiService;
  private final DocumentRepository            docRepo;
  private final HotelRepository               hotelRepo;
  private final AmenityRepository             amenityRepo;
  private final HotelRoomRepository           roomRepo;
  private final BookingRepository             bookingRepo;
  private final FeedbackRepository            feedbackRepo;
  private final ServiceRequestRepository      serviceReqRepo;
  private final MessageSource                 messages;
  private final int                           topK;
  private final String                        chatModel;

  public ChatService(
    EmbeddingService embeddingService,
    OpenAiService    openAiService,
    DocumentRepository docRepo,
    HotelRepository    hotelRepo,
    AmenityRepository  amenityRepo,
    HotelRoomRepository roomRepo,
    BookingRepository   bookingRepo,
    FeedbackRepository  feedbackRepo,
    ServiceRequestRepository serviceReqRepo,
    MessageSource      messages,
    @Value("${chatbot.topk}")   int topK,
    @Value("${chatbot.model}")  String chatModel
  ) {
    this.embeddingService = embeddingService;
    this.openAiService    = openAiService;
    this.docRepo          = docRepo;
    this.hotelRepo        = hotelRepo;
    this.amenityRepo      = amenityRepo;
    this.roomRepo         = roomRepo;
    this.bookingRepo      = bookingRepo;
    this.feedbackRepo     = feedbackRepo;
    this.serviceReqRepo   = serviceReqRepo;
    this.messages         = messages;
    this.topK             = topK;
    this.chatModel        = chatModel;
  }

  /** Entry point called by your ChatController **/
  public ChatResponse chat(ChatRequest req) {
    String userMessage = req.getMessage();
    String language    = req.getLanguage();
    DialogState state  = Optional.ofNullable(req.getState())
                                 .orElse(new DialogState());

    Locale locale = Locale.forLanguageTag(language);
    String lower  = userMessage.toLowerCase();

    // 1) FAQ via vector search
    if (lower.matches(".*\\b(check[- ]?in|check[- ]?out|price|amenit|wifi)\\b.*")) {
      return handleFaq(userMessage, locale, state);
    }

    // 2) Any “hotel” query goes to our combined filter
    if (state.isInHotelFlow() || lower.contains("hotel")) {
      state.setInHotelFlow(true);
      return handleHotelSearch(userMessage, locale, state);
    }

    // 3) Booking assistance (stub)
    if (lower.contains("book") || lower.contains("reserve") || lower.contains("availability")) {
      return handleBooking(userMessage, locale, state);
    }

    // 4) Feedback / complaints
    if (lower.contains("feedback") || lower.contains("complaint")) {
      return handleFeedback(userMessage, locale, state);
    }

    // 5) Service requests (towel, spa, housekeeping…)
    if (lower.matches(".*\\b(towel|spa|housekeep|clean)\\b.*")) {
      return handleServiceRequest(userMessage, locale, state);
    }

    // 6) Smart recommendations
    if (lower.contains("recommend") || lower.contains("suggest")) {
      return handleRecommendations(userMessage, locale, state);
    }

    // 7) Fallback to full AI chat
    return handleAiFallback(userMessage, locale, state);
  }

  // 1) FAQ via vector search
  private ChatResponse handleFaq(String q, Locale locale, DialogState state) {
    float[] vec = embeddingService.embed(q);
    var docs = docRepo.findTopKSimilar(vec, topK);
    if (docs.isEmpty()) {
      String msg = messages.getMessage("faq.not_found", null, locale);
      return new ChatResponse(msg, false, List.of("Book a room", "Amenities"), state);
    }
    String context     = docs.stream()
                              .map(KnowledgeDocument::getContent)
                              .collect(Collectors.joining("\n---\n"));
    String systemPrompt = messages.getMessage("faq.system_prompt", new Object[]{context}, locale);
    String aiReply      = askOpenAi(systemPrompt, q);
    return new ChatResponse(aiReply, false, List.of("Book a room", "More info"), state);
  }

  // 2) Hotel Search with step-by-step prompting
  private ChatResponse handleHotelSearch(String text, Locale locale, DialogState state) {
    Filters f = parseFilters(text);

    // 2.1) Ask for location if missing
    if (f.location == null) {
      String askLoc = messages.getMessage("hotel.ask_location", null, locale);
      return new ChatResponse(askLoc, false, List.of("Hotels in CA", "Hotels in TX"), state);
    }
    state.setLocation(f.location);

    // 2.2) Ask for max price if missing
    if (f.maxPrice == null) {
      String askPrice = messages.getMessage("hotel.ask_max_price", null, locale);
      return new ChatResponse(askPrice, false, List.of("Under $200", "Below $150"), state);
    }
    state.setMaxPrice(f.maxPrice);

    // 2.3) Ask for min stars if missing
    if (f.minStars == null) {
      String askStars = messages.getMessage("hotel.ask_min_stars", null, locale);
      return new ChatResponse(askStars, false, List.of("4 stars", "5 stars"), state);
    }
    state.setMinStars(f.minStars);

    // 2.4) Ask for amenities if none yet
    if (f.amenities.isEmpty()) {
      String askAms = messages.getMessage("hotel.ask_amenities", null, locale);
      return new ChatResponse(askAms, false, List.of("WiFi", "Free parking", "Spa"), state);
    }
    state.setAmenities(f.amenities);

    // 2.5) Perform actual search
    List<Hotel> candidates = hotelRepo
      .findByHotelNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrStateContainingIgnoreCase(
        state.getLocation(), state.getLocation(), state.getLocation()
      );
    List<Hotel> matches = candidates.stream()
      .filter(h ->
        h.getAveragePrice() <= state.getMaxPrice() &&
        h.getStarRating()  >= state.getMinStars()  &&
        h.getAmenities().stream()
          .map(Amenity::getName)
          .map(String::toLowerCase)
          .collect(Collectors.toSet())
          .containsAll(state.getAmenities())
      )
      .collect(Collectors.toList());

    if (matches.isEmpty()) {
      String none = messages.getMessage("hotel.none", new Object[]{state.getLocation()}, locale);
      state.setInHotelFlow(false);
      return new ChatResponse(none, false, List.of("Try another search"), state);
    }

    String header = messages.getMessage("hotel.list_header", new Object[]{state.getLocation()}, locale);
    StringBuilder sb = new StringBuilder(header).append("\n\n");
    for (Hotel h : matches) {
      sb.append("• ").append(h.getHotelName())
        .append(" (").append(h.getCity()).append(", ").append(h.getState()).append(")\n")
        .append("    Price: $").append(h.getAveragePrice())
        .append(", Stars: ").append(h.getStarRating()).append("\n");
      String ams = h.getAmenities().stream()
                    .map(Amenity::getName)
                    .collect(Collectors.joining(", "));
      sb.append("    Amenities: ").append(ams.isBlank()?"N/A":ams).append("\n\n");
    }

    state.setInHotelFlow(false);
    return new ChatResponse(sb.toString().trim(), false,
                            List.of("Book first hotel", "More details"),
                            state);
  }

  // Pull out location, maxPrice, minStars & DB‐driven amenities
  private Filters parseFilters(String text) {
    Filters f = new Filters();
    String lower = text.toLowerCase();

    Matcher mLoc   = Pattern.compile("in ([a-z ]+)", Pattern.CASE_INSENSITIVE).matcher(text);
    Matcher mPrice = Pattern.compile("(?:under|below|max) \\$?(\\d+\\.?\\d*)").matcher(lower);
    Matcher mStar  = Pattern.compile("(\\d)\\s*(?:-star|stars?)").matcher(lower);

    if (mLoc.find())   f.location = mLoc.group(1).trim();
    if (mPrice.find()) f.maxPrice = Double.parseDouble(mPrice.group(1));
    if (mStar.find())  f.minStars = Integer.parseInt(mStar.group(1));

    var allAms = amenityRepo.findAll().stream()
                 .map(a -> a.getName().toLowerCase())
                 .collect(Collectors.toList());
    for (String am : allAms) {
      if (lower.contains(am)) {
        f.amenities.add(am);
      }
    }
    return f;
  }
  private static class Filters {
    String      location = null;
    Double      maxPrice = null;
    Integer     minStars = null;
    Set<String> amenities = new HashSet<>();
  }

  // 3) Booking assistance (stub)
  private ChatResponse handleBooking(String text, Locale locale, DialogState state) {
    String ask = messages.getMessage("booking.ask_details", null, locale);
    return new ChatResponse(ask, false, List.of("123,2025-06-20,2025-06-22,2"), state);
  }

  // when you have structured booking details …
  public ChatResponse listRoomOptions(
      Integer hotelId,
      LocalDate checkIn,
      LocalDate checkOut,
      int roomsRequested,
      DialogState state,
      Locale locale
  ) {
    var rooms = roomRepo.findByHotel_HotelId(hotelId).stream()
      .filter(r ->
        bookingRepo.countBookedRooms(r.getHotelRoomId(), checkIn, checkOut)
        + roomsRequested
        <= r.getNoRooms()
      ).collect(Collectors.toList());

    if (rooms.isEmpty()) {
    	  String none = messages.getMessage("booking.none", null, locale);
    	  return new ChatResponse(
    	    none,
    	    false,
    	    List.of("Try other dates"),
    	    state           // ← pass the current dialog‐state back
    	  );
    	}

    	String header = messages.getMessage("booking.options_header", null, locale);
    	String body = rooms.stream().limit(3)
    	    .map(r -> MessageFormat.format(
    	          messages.getMessage("booking.option_format", null, locale),
    	          r.getType().getName(),
    	          r.getPrice(),
    	          r.getNoRooms()
    	    ))
    	    .collect(Collectors.joining("\n"));

    	return new ChatResponse(
    	  header + "\n" + body,
    	  false,
    	  List.of("Confirm first option","Contact agent"),
    	  state          // ← and here too
    	);
  }

  // 4) Feedback / complaints
  private ChatResponse handleFeedback(String text, Locale locale, DialogState state) {
    feedbackRepo.save(new Feedback(text));
    String thanks = messages.getMessage("feedback.thanks", null, locale);
    return new ChatResponse(thanks, true, List.of("Talk to agent"), state);
  }

  // 5) Hotel service requests
  private ChatResponse handleServiceRequest(String text, Locale locale, DialogState state) {
    serviceReqRepo.save(new ServiceRequest(text));
    String ack = messages.getMessage("service.ack", null, locale);
    return new ChatResponse(ack, false, List.of("Anything else?"), state);
  }

  // 6) Smart recommendations from your docs
  private ChatResponse handleRecommendations(String text, Locale locale, DialogState state) {
    float[] vec = embeddingService.embed(text);
    var docs = docRepo.findTopKSimilar(vec, topK);
    String recs = docs.stream().limit(5)
                 .map(KnowledgeDocument::getContent)
                 .collect(Collectors.joining("\n• ", "\n• ", ""));
    return new ChatResponse(
      messages.getMessage("recs.header", null, locale) + recs,
      false,
      List.of("Book now", "More recs"),
      state
    );
  }

  // 7) Fallback to full ChatGPT conversational path
  private ChatResponse handleAiFallback(String text, Locale locale, DialogState state) {
    float[] vec = embeddingService.embed(text);
    var docs = docRepo.findTopKSimilar(vec, topK);
    String ctx = docs.stream()
                     .map(KnowledgeDocument::getContent)
                     .collect(Collectors.joining("\n---\n"));
    String system = messages.getMessage("fallback.system_prompt", new Object[]{ctx}, locale);
    String reply = askOpenAi(system, text);
    return new ChatResponse(reply, false, List.of("Book a room", "Human help"), state);
  }

  // helper to fire the OpenAI chat API
  private String askOpenAi(String systemPrompt, String userText) {
    var msgs = List.of(
      new ChatMessage("system", systemPrompt),
      new ChatMessage("user",   userText)
    );
    var req = ChatCompletionRequest.builder()
                  .model(chatModel)
                  .messages(msgs)
                  .temperature(0.2)
                  .build();
    ChatCompletionChoice c = openAiService
      .createChatCompletion(req)
      .getChoices().get(0);
    return c.getMessage().getContent().trim();
  }
}
