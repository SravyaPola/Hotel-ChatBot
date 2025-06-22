package com.synex.service;

import com.synex.domain.*;
import com.synex.domain.ConversationState.Stage;
import com.synex.domain.ParseResult.Intent;
import com.synex.repository.HotelRepository;
import com.synex.repository.RoomTypeRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ChatService2 {

  @Autowired private NLPService2 nlp;
  @Autowired private HotelService hotelService;
  @Autowired private HotelRepository hotelRepo;
  @Autowired private BookingService bookingService;
  
  @Autowired private RoomTypeRepository roomTypeRepo;
  private static final Logger log = LoggerFactory.getLogger(ChatService2.class);

  public ChatResponse chat(ChatRequest req) {
    String user = req.getMessage();
    ConversationState st = Optional.ofNullable(req.getState())
                                   .orElse(new ConversationState());
    ParseResult pr = nlp.parse(user, st.getStage().name());
    mergeSlots(st, pr);

    switch (st.getStage()) {
      case START:
        st.setStage(Stage.ASK_CITY);
        return new ChatResponse(
          "Hi there! Welcome to Hotel Booking. Which city/state?", 
          false,
          hotelService.getAllCities(),
          st
        );

      case ASK_CITY:
        if (pr.getCity() != null) {
          st.setCity(pr.getCity());
          st.setStage(Stage.ASK_DATES);
          return new ChatResponse(
            "Great—" + st.getCity() +
            "! When will you check in/out? (e.g. May 5, 2025 to May 8, 2025)",
            false, List.of("May 5, 2025 to May 8, 2025"), st
          );
        }
        return new ChatResponse(
          "Please tell me a city or state:", 
          false,
          hotelService.getAllCities(),
          st
        );

      case ASK_DATES:
        if (pr.getCheckIn() != null && pr.getCheckOut() != null) {
          st.setCheckIn(pr.getCheckIn());
          st.setCheckOut(pr.getCheckOut());
          st.setStage(Stage.ASK_GUESTS);
          return new ChatResponse(
            "How many guests?", 
            false,
            List.of("2 adults"), 
            st
          );
        }
        return new ChatResponse(
          "Invalid dates. Try “May 5, 2025 to May 8, 2025.”", 
          false,
          List.of("May 5, 2025 to May 8, 2025"),
          st
        );

      case ASK_GUESTS:
        if (pr.getGuests() != null) {
          st.setGuests(pr.getGuests());
          st.setStage(Stage.ASK_FILTERS);
          return new ChatResponse(
            "Any filters? stars, price, amenities or “no filters”.", 
            false,
            List.of("4★","$200–$300","wifi"),
            st
          );
        }
        return new ChatResponse(
          "Please say number of guests, e.g. “2 adults”.",
          false,
          List.of("2 adults"),
          st
        );

      case ASK_FILTERS:
    	    // ─── 1) Did the user explicitly say “no filters”? ───
    	    String rawFilt = Optional.ofNullable(pr.getRawText())
    	                             .map(String::toLowerCase)
    	                             .orElse("");
    	    if (rawFilt.contains("no filters")) {
    	        // clear out any prior filters
    	        st.setMinStars(null);
    	        st.setMinPrice(null);
    	        st.setMaxPrice(null);
    	        st.getRequiredAmenities().clear();
    	    } else {
    	        // ─── 2) Merge any new filter slots they provided ───
    	        if (pr.getMinStars()   != null) st.setMinStars(pr.getMinStars());
    	        if (pr.getMinPrice()   != null) st.setMinPrice(pr.getMinPrice());
    	        if (pr.getMaxPrice()   != null) st.setMaxPrice(pr.getMaxPrice());
    	        if (pr.getAmenities()  != null) st.setRequiredAmenities(
    	                                             new HashSet<>(pr.getAmenities())
    	                                         );
    	    }

    	    // ─── 3) Fetch & apply filters (empty filters ⇒ all hotels in city) ───
    	    List<Hotel> base  = hotelService.searchByNameOrLocation(st.getCity());
    	    List<Hotel> matches = hotelService.filter(
    	      base,
    	      st.getMinStars(),
    	      st.getMinPrice(),
    	      st.getMaxPrice(),
    	      st.getRequiredAmenities()
    	    );

    	    // ─── 4a) No matches ⇒ ask if they want to remove filters ───
    	    if (matches.isEmpty()) {
    	        st.setStage(Stage.ASK_FILTER_REFINE);
    	        return new ChatResponse(
    	          "No hotels found with those filters. Remove filters? (yes/no)",
    	          false,
    	          List.of("yes","no"),
    	          st
    	        );
    	    }

    	    // ─── 4b) We have matches ⇒ immediately show them ───
    	    st.setLastHotels(matches);
    	    st.setStage(Stage.SHOW_HOTELS);

    	    StringBuilder list = new StringBuilder("Here are your options:\n");
    	    for (int i = 0; i < matches.size(); i++) {
    	        Hotel h = matches.get(i);
    	        list.append(String.format(
    	          "%d. %s — %d★, $%.2f/night\n",
    	          i+1, h.getHotelName(), h.getStarRating(), h.getAveragePrice()
    	        ));
    	    }
    	    return new ChatResponse(
    	      list.toString().trim(),
    	      false,
    	      matches.stream()
    	             .map(Hotel::getHotelName)
    	             .collect(Collectors.toList()),
    	      st
    	    );

    	// ────────────────────────────────────────────────────────────────────────

    	case ASK_FILTER_REFINE:
    	    // User chose whether to remove filters
    	    if (Boolean.TRUE.equals(pr.getConfirm())) {
    	        // ─── Yes: clear filters & re-search by city/state ───
    	        st.setMinStars(null);
    	        st.setMinPrice(null);
    	        st.setMaxPrice(null);
    	        st.getRequiredAmenities().clear();

    	        List<Hotel> all = hotelService.searchByNameOrLocation(st.getCity());
    	        if (all.isEmpty()) {
    	            // no hotels even without filters ⇒ ask for new city/state
    	            st.setStage(Stage.ASK_CITY);
    	            return new ChatResponse(
    	              "Okay, removed filters. Still no hotels in “" + st.getCity() + ".”\n" +
    	              "Which city or state would you like instead?",
    	              false,
    	              hotelService.getAllCities(),
    	              st
    	            );
    	        }

    	        // we have hotels ⇒ show them
    	        st.setLastHotels(all);
    	        st.setStage(Stage.SHOW_HOTELS);

    	        StringBuilder allList = new StringBuilder(
    	          "Showing all hotels in " + st.getCity() + ":\n"
    	        );
    	        for (int i = 0; i < all.size(); i++) {
    	            Hotel h = all.get(i);
    	            allList.append(String.format(
    	              "%d. %s — %d★, $%.2f/night\n",
    	              i+1, h.getHotelName(), h.getStarRating(), h.getAveragePrice()
    	            ));
    	        }
    	        return new ChatResponse(
    	          allList.toString().trim(),
    	          false,
    	          all.stream().map(Hotel::getHotelName).toList(),
    	          st
    	        );
    	    } else {
    	        // ─── No: keep their filters and let them adjust ───
    	        st.setStage(Stage.ASK_FILTERS);
    	        return new ChatResponse(
    	          "All right—keeping your filters. You can modify stars, price,\n" +
    	          "amenities, or say “no filters” to clear them completely.",
    	          false,
    	          List.of("no filters"),
    	          st
    	        );
    	    }


    	case SHOW_HOTELS: {
    	    // 1) Load or reload the list of hotels
    	    List<Hotel> hotels = st.getLastHotels();
    	    if (hotels == null || hotels.isEmpty()) {
    	        hotels = hotelService.searchByNameOrLocation(st.getCity());
    	        st.setLastHotels(hotels);
    	    }

    	    // 2) Debug
    	    log.debug(">> SHOW_HOTELS: raw='{}', hotelIndex={}, lastHotels={}",
    	              pr.getRawText(),
    	              pr.getHotelIndex(),
    	              hotels.stream().map(Hotel::getHotelName).toList());

    	    // 3) Clean & normalize the user’s raw input
    	    String raw = Optional.ofNullable(pr.getRawText()).orElse("").trim();
    	    // strip any leading number + punctuation/space
    	    raw = raw.replaceFirst("^(\\d+)\\s*\\.?\\s*", "").trim();

    	    Hotel chosen = null;

    	    // 4a) Try numeric index
    	    if (pr.getHotelIndex() != null) {
    	        int idx = pr.getHotelIndex();
    	        if (idx >= 1 && idx <= hotels.size()) {
    	            chosen = hotels.get(idx - 1);
    	            st.setSelectedHotelIndex(idx);
    	            log.debug("  picked by index → {}", chosen.getHotelName());
    	        } else {
    	            log.debug("  index {} out of range (1–{})", idx, hotels.size());
    	        }
    	    }

    	    // 4b) Then try exact‐name or substring match
    	    if (chosen == null && !raw.isBlank()) {
    	        String lowerRaw = raw.toLowerCase();
    	        for (int i = 0; i < hotels.size(); i++) {
    	            Hotel h = hotels.get(i);
    	            String name = h.getHotelName();
    	            String lowerName = name.toLowerCase();
    	            if (lowerName.equals(lowerRaw) || lowerName.contains(lowerRaw)) {
    	                chosen = h;
    	                st.setSelectedHotelIndex(i + 1);
    	                log.debug("  picked by name/substring → {}", name);
    	                break;
    	            }
    	        }
    	        if (chosen == null) {
    	            log.debug("  no name match for '{}'", raw);
    	        }
    	    }

    	    // 5) If we got one, advance
    	    if (chosen != null) {
    	        st.setChosenHotel(chosen);
    	        st.setHotelName(chosen.getHotelName());
    	        st.setHotelId(chosen.getHotelId());// record it!
    	       st.setStage(Stage.ASK_SERVICES);
    	        return buildAmenitiesResponse(chosen, st);
    	    }

    	    // 6) Otherwise re-prompt with full menu
    	    List<String> suggestions = new ArrayList<>();
    	    StringBuilder prompt = new StringBuilder("Which hotel would you like to book? Reply with its *number* or *full name*:\n");
    	    for (int i = 0; i < hotels.size(); i++) {
    	        int num = i + 1;
    	        Hotel h = hotels.get(i);
    	        prompt.append(String.format("%d. %s — %d★, $%.2f/night\n",
    	                          num, h.getHotelName(), h.getStarRating(), h.getAveragePrice()));
    	        suggestions.add(String.valueOf(num));
    	        suggestions.add(h.getHotelName());
    	    }

    	    return new ChatResponse(
    	      prompt.toString().trim(),
    	      false,
    	      suggestions,
    	      st
    	    );
    	}



    	case SHOW_AMENITIES: {
    	    // 1) Grab your chosen Hotel object
    		 if (st.getChosenHotel() == null && st.getHotelId() != null) {
    		        Hotel h = hotelRepo.findById(st.getHotelId()).orElse(null);
    		        st.setChosenHotel(h);
    		    }
    	    Hotel hotel = st.getChosenHotel();
    	    if (hotel == null) {
    	        // Fallback: we really did lose it, so go back to SHOW_HOTELS
    	        List<String> retry = new ArrayList<>();
    	        for (int i = 0; i < st.getLastHotels().size(); i++) {
    	            Hotel h = st.getLastHotels().get(i);
    	            retry.add(String.valueOf(i+1));
    	            retry.add(h.getHotelName());
    	        }
    	        st.setStage(Stage.SHOW_HOTELS);
    	        return new ChatResponse(
    	          "Oops—I lost which hotel you picked. Please choose again:",
    	          false,
    	          retry,
    	          st
    	        );
    	    }

    	    // 2) Fetch amenities & service options
    	    List<String> ams  = hotelService.getAmenityNamesForHotel(hotel.getHotelId());
    	    List<ServiceOption> opts = hotelService.getServiceOptions(hotel.getHotelId());

    	    // 3) Store them for the next turn
    	    st.setLastServiceOptions(opts);
    	    st.setStage(Stage.ASK_SERVICES);

    	    // 4) Build the reply
    	    StringBuilder out = new StringBuilder()
    	        .append("Excellent choice! Here are the key amenities at ")
    	        .append(hotel.getHotelName())
    	        .append(":\n");
    	    for (String a : ams) {
    	        out.append(" • ").append(a).append("\n");
    	    }
    	    out.append("Any add-on services?\n");
    	    for (int i = 0; i < opts.size(); i++) {
    	        ServiceOption s = opts.get(i);
    	        out.append(String.format(
    	          "%d. %s (+$%.2f%s)\n",
    	          i+1,
    	          s.getName(),
    	          s.getPrice(),
    	          s.getPerPerson() ? "/person" : ""
    	        ));
    	    }

    	    // 5) Suggestions should include both numbers and full names
    	    List<String> suggestions = new ArrayList<>();
    	    for (int i = 0; i < opts.size(); i++) {
    	        suggestions.add(String.valueOf(i+1));
    	        suggestions.add(opts.get(i).getName());
    	    }
    	    suggestions.add("No, thanks");

    	    return new ChatResponse(
    	      out.toString().trim(),
    	      false,
    	      suggestions,
    	      st
    	    );
    	}


    	case ASK_SERVICES: {
    	    List<ServiceOption> all = st.getLastServiceOptions();
    	    String raw = Optional.ofNullable(pr.getRawText()).orElse("").trim();

    	    // 1) Explicit “no, thanks” by intent or literal text
    	    if (pr.getIntent() == Intent.SERVICES_NONE
    	        || raw.equalsIgnoreCase("no")
    	        || raw.equalsIgnoreCase("no, thanks")) {

    	        st.setChosenServiceOptions(Collections.emptyList());
    	        st.setChosenServices(Collections.emptyList());

    	    } else {
    	        // 2) Parse numeric indices
    	        Set<Integer> numbers = new HashSet<>();
    	        Matcher m = Pattern.compile("\\b(\\d+)\\b").matcher(raw);
    	        while (m.find()) {
    	            numbers.add(Integer.parseInt(m.group(1)));
    	        }
    	        List<ServiceOption> picked = new ArrayList<>();
    	        for (int idx : numbers) {
    	            if (idx >= 1 && idx <= all.size()) {
    	                picked.add(all.get(idx - 1));
    	            }
    	        }

    	        // 3) Parse exact names
    	        for (String part : raw.split("(,| and )")) {
    	            String cand = part.trim();
    	            for (ServiceOption so : all) {
    	                if (so.getName().equalsIgnoreCase(cand)) {
    	                    if (!picked.contains(so)) picked.add(so);
    	                }
    	            }
    	        }

    	        st.setChosenServiceOptions(picked);
    	        st.setChosenServices(
    	            picked.stream()
    	                  .map(ServiceOption::getName)
    	                  .collect(Collectors.toList())
    	        );
    	    }

    	    // 4) If we still have nothing selected *and* user tried something else, re-prompt
    	    if (st.getChosenServiceOptions().isEmpty()
    	        && !raw.isBlank()
    	        && pr.getIntent() != Intent.SERVICES_NONE) {

    	        // Rebuild full menu text
    	        StringBuilder prompt = new StringBuilder()
    	            .append("Sorry, I didn’t catch that. Please pick one or more numbers ")
    	            .append("or exact service *names* (not description), or say “no, thanks.”\n");

    	        // Rebuild suggestions list
    	        List<String> suggestions = new ArrayList<>();
    	        for (int i = 0; i < all.size(); i++) {
    	            ServiceOption so = all.get(i);
    	            int num = i + 1;
    	            prompt.append(String.format("%d. %s\n", num, so.getName()));
    	            suggestions.add(String.valueOf(num));
    	            suggestions.add(so.getName());
    	        }
    	        // Add the no‐thanks option last
    	        int noneOpt = all.size() + 1;
    	        prompt.append(noneOpt).append(". No, thanks");
    	        suggestions.add("No, thanks");

    	        return new ChatResponse(
    	            prompt.toString().trim(),
    	            false,
    	            suggestions,
    	            st
    	        );
    	    }

    	    // 5) Otherwise, we have valid picks (or explicit “no”) → next stage
    	    st.setStage(Stage.ASK_ROOM_TYPE);
    	    // …build your room‐type menu here…
    	}



    	// ─── ASK_ROOM_TYPE ────────────────────────────────────────────────────────────
    	case ASK_ROOM_TYPE: {
    	    List<HotelRoom> rooms = hotelService.getRoomsForHotel(st.getChosenHotel().getHotelId());
    	    String raw = Optional.ofNullable(pr.getRawText()).orElse("").trim();
    	    HotelRoom chosenRoom = null;

    	    // 1) Pick by number
    	    if (pr.getHotelIndex() != null) {
    	      int idx = pr.getHotelIndex();
    	      if (idx >= 1 && idx <= rooms.size()) {
    	        chosenRoom = rooms.get(idx - 1);
    	      }
    	    }

    	    // 2) Or pick by exact name match
    	    if (chosenRoom == null && !raw.isBlank()) {
    	      for (HotelRoom r : rooms) {
    	        if (r.getType().getName().equalsIgnoreCase(raw)) {
    	          chosenRoom = r;
    	          break;
    	        }
    	      }
    	    }

    	    // 3) If valid, store it and ask for # of rooms
    	    if (chosenRoom != null) {
    	      st.setChosenRoom(chosenRoom);
    	      st.setStage(Stage.ASK_NUM_ROOMS);
    	      return new ChatResponse(
    	        "How many rooms would you like?",
    	        false,
    	        List.of("1","2","3","4"),
    	        st
    	      );
    	    }

    	    // 4) Otherwise re-prompt
    	    List<String> opts = new ArrayList<>();
    	    for (int i = 0; i < rooms.size(); i++) {
    	      opts.add(String.valueOf(i+1));
    	      opts.add(rooms.get(i).getType().getName());
    	    }
    	    return new ChatResponse(
    	      "Please pick a room type by number or name:",
    	      false,
    	      opts,
    	      st
    	    );
    	}

    	// ─── ASK_NUM_ROOMS ─────────────────────────────────────────────────────────────
    	case ASK_NUM_ROOMS: {
    	    // try to parse any integer from their reply
    	    Matcher m = Pattern.compile("(\\d+)").matcher(pr.getRawText());
    	    if (m.find()) {
    	      int noRooms = Integer.parseInt(m.group(1));
    	      st.setNoRooms(noRooms);
    	      st.setStage(Stage.ASK_CUSTOMER_NAME);
    	      return new ChatResponse(
    	        "Under what name should I reserve these rooms?",
    	        false,
    	        List.of("John Doe","Jane Smith"),
    	        st
    	      );
    	    }
    	    return new ChatResponse(
    	      "How many rooms? Please reply with a number.",
    	      false,
    	      List.of("1","2","3","4"),
    	      st
    	    );
    	}

    	// ─── ASK_CUSTOMER_NAME ─────────────────────────────────────────────────────────
    	case ASK_CUSTOMER_NAME: {
    	    String customer = Optional.ofNullable(pr.getRawText()).orElse("").trim();
    	    if (!customer.isBlank()) {
    	      st.setCustomerName(customer);
    	      Booking b = bookingService.create(
    	        st.getChosenHotel().getHotelId(),
    	        st.getChosenRoom().getHotelRoomId(),
    	        st.getNoRooms(),
    	        st.getGuests(),
    	        st.getCheckIn(),
    	        st.getCheckOut(),
    	        st.getCustomerName(),
    	        String.join(",", st.getChosenServices())
    	      );
    	      st.getBookingIds().add(b.getBookingId());

    	      st.setStage(Stage.SHOW_SUBTOTAL);
    	      double total = bookingService.computeTotal(st.getBookingIds());
    	      return new ChatResponse(
    	        "Booked! Your booking ID is " + b.getBookingId() +
    	        ". Current total (before taxes) is $" + total +
    	        "\nProceed to payment? (pay)",
    	        false,
    	        List.of("pay"),
    	        st
    	      );
    	    }

    	    return new ChatResponse(
    	      "Please tell me the name for the reservation.",
    	      false,
    	      Collections.emptyList(),
    	      st
    	    );
    	}


    	// ─── SHOW_SUBTOTAL ───────────────────────────────────────────────────────────
    	case SHOW_SUBTOTAL: {
    	    // 1) If they confirm, create the Booking with all fields
    	    if (Boolean.TRUE.equals(pr.getConfirm())) {
    	        Booking b = bookingService.create(
    	            // hotel and room
    	            st.getChosenHotel().getHotelId(),
    	            st.getChosenRoom().getHotelRoomId(),
    	            // number of rooms & guests
    	            st.getNoRooms(),
    	            st.getGuests(),
    	            // dates
    	            st.getCheckIn(),
    	            st.getCheckOut(),
    	            // customer & services
    	            st.getCustomerName(),
    	            String.join(",", st.getChosenServices())
    	        );
    	        st.getBookingIds().add(b.getBookingId());
    	    }

    	    // 2) Move on to asking if they want another hotel
    	    st.setStage(Stage.ASK_ANOTHER);
    	    return new ChatResponse(
    	      "Your reservation is provisionally held. Would you like to book another hotel? (yes/no)",
    	      false,
    	      List.of("yes","no"),
    	      st
    	    );
    	}



      case ASK_ANOTHER:
        if (Boolean.TRUE.equals(pr.getConfirm())) {
          st.setStage(Stage.ASK_CITY);
          return new ChatResponse(
            "Next city/state?", false,
            hotelService.getAllCities(), st
          );
        }
        st.setStage(Stage.ASK_NAME);
        return new ChatResponse(
          "Under what name?", false,
          List.of(), st
        );

      case ASK_PAYMENT:
        if (Boolean.FALSE.equals(pr.getConfirm())) {
          st = new ConversationState();
          return new ChatResponse(
            "Restarting. Which city/state?", false,
            hotelService.getAllCities(), st
          );
        }
        if (pr.getIntent() == Intent.PAY) {
          double total = bookingService.computeTotal(st.getBookingIds());
          st.setStage(Stage.ASK_FEEDBACK);
          return new ChatResponse(
            "Charged $"+ total +". Thanks! Rate us? (1–5)", false,
            List.of("1","2","3","4","5"), st
          );
        }
        return new ChatResponse(
          "Say “pay” or “no”.", false,
          List.of("pay","no"), st
        );

      case ASK_FEEDBACK:
        if (pr.getRating() != null) {
          st.setStage(Stage.DONE);
          return new ChatResponse(
            "Thank you! Have a great trip!", true,
            List.of(), st
          );
        }
        return new ChatResponse(
          "Rate 1–5 please.", false,
          List.of("1","2","3","4","5"), st
        );

      default:
        return new ChatResponse("Goodbye!", true, List.of(), st);
    }
  }

  private void mergeSlots(ConversationState st, ParseResult pr) {
    if (pr.getCity() != null) st.setCity(pr.getCity());
    if (pr.getCheckIn() != null) st.setCheckIn(pr.getCheckIn());
    if (pr.getCheckOut() != null) st.setCheckOut(pr.getCheckOut());
    if (pr.getGuests() != null) st.setGuests(pr.getGuests());
    if (pr.getMinStars() != null) st.setMinStars(pr.getMinStars());
    if (pr.getMinPrice() != null) st.setMinPrice(pr.getMinPrice());
    if (pr.getMaxPrice() != null) st.setMaxPrice(pr.getMaxPrice());
    if (pr.getAmenities() != null) st.setRequiredAmenities(new HashSet<>(pr.getAmenities()));
    if (pr.getServiceQuantityDays() != null)
      st.setServiceQuantityDays(pr.getServiceQuantityDays());
    if (pr.getRoomType() != null) st.setRoomType(pr.getRoomType());
  }
  
  private ChatResponse buildAmenitiesResponse(Hotel chosen, ConversationState st) {
	    // 1) fetch
	    List<String> amenNames    = hotelService.getAmenityNamesForHotel(chosen.getHotelId());
	    List<ServiceOption> opts  = hotelService.getServiceOptions(chosen.getHotelId());
	    st.setLastServiceOptions(opts);

	    // 2) build the body text
	    StringBuilder sb = new StringBuilder()
	        .append("Excellent choice! Here are the key amenities at ")
	        .append(chosen.getHotelName())
	        .append(":\n");
	    for (String a : amenNames) {
	        sb.append(" • ").append(a).append("\n");
	    }
	    sb.append("Any add-on services?\n");

	    // 3) list each service with number AND capture suggestions
	    List<String> suggestions = new ArrayList<>();
	    for (int i = 0; i < opts.size(); i++) {
	        ServiceOption so = opts.get(i);
	        int num = i + 1;
	        sb.append(String.format(
	            "%d. %s (+$%.2f%s)\n",
	            num,
	            so.getName(),
	            so.getPrice(),
	            so.getPerPerson() ? "/person" : ""
	        ));
	        // let the user reply by “1” or by “Golf Package”
	        suggestions.add(String.valueOf(num));
	        suggestions.add(so.getName());
	    }

	    // 4) add the “No, thanks” option
	    int noneOpt = opts.size() + 1;
	    sb.append(noneOpt).append(". No, thanks");
	    suggestions.add("No, thanks");

	    // 5) final state & return
	    //    (we already moved st → ASK_SERVICES in SHOW_HOTELS)
	    return new ChatResponse(
	        sb.toString().trim(),
	        false,
	        suggestions,
	        st
	    );
	}

  private ChatResponse buildServicePrompt(ConversationState st, String header) {
      List<ServiceOption> opts = st.getLastServiceOptions();
      StringBuilder sb = new StringBuilder(header).append("\n");
      List<String> suggestions = new ArrayList<>();

      for (int i = 0; i < opts.size(); i++) {
          ServiceOption so = opts.get(i);
          int num = i + 1;
          sb.append(String.format("%d. %s (+$%.2f%s)\n",
                   num,
                   so.getName(),
                   so.getPrice(),
                   so.getPerPerson() ? "/person" : ""));
          // allow replies by number or exact name
          suggestions.add(String.valueOf(num));
          suggestions.add(so.getName());
      }
      // no-thanks as option N+1, with both comma/no‐comma variants
      int noneOpt = opts.size() + 1;
      sb.append(noneOpt).append(". No, thanks");
      suggestions.add(String.valueOf(noneOpt));
      suggestions.add("No, thanks");
      suggestions.add("No thanks");

      return new ChatResponse(sb.toString().trim(), false, suggestions, st);
  }



}
