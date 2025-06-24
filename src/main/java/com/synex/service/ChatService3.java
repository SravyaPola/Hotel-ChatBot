package com.synex.service;

import com.synex.domain.*;
import com.synex.domain.StateConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class ChatService3 {

	@Autowired
	private NLPService3 nlp;
	@Autowired
	private HotelService hotelService;
	@Autowired
	private Map<ConversationState.Stage, StateConfig> workflow;
	@Autowired
	RoomTypeService roomTypeService;
	@Autowired
	private BookingService bookingService;
	@Autowired
	private FeedbackService feedbackService;
	@Autowired
	private HotelRoomService hotelRoomService;
	@Autowired
	private AmenityService amenityService;

	public ChatResponse chat(ChatRequest req) {
		ConversationState st = Optional.ofNullable(req.getState()).orElse(new ConversationState());
		if (st.getBookingIds() == null) {
			st.setBookingIds(new ArrayList<>());
		}
		String userMsg = req.getMessage();
		ConversationState.Stage stage = st.getStage();

		// ========== 1. ASK_LOCATION ===============
		if (stage == ConversationState.Stage.ASK_LOCATION) {
			StateConfig cfg = workflow.get(stage);
			Map<String, String> slots = nlp.fillSlots(cfg.slotsNeeded(), userMsg);

			String citySlot = slots.get("city");
			String stateSlot = slots.get("state");
			if (citySlot != null && !citySlot.isBlank())
				st.setCity(citySlot.trim());
			if (stateSlot != null && !stateSlot.isBlank())
				st.setState(stateSlot.trim());

			boolean hasCity = st.getCity() != null && !st.getCity().isBlank();
			boolean hasState = st.getState() != null && !st.getState().isBlank();

			List<String> suggestions = Stream.of(hotelService.getAllCities(), hotelService.getAllStates())
					.flatMap(Collection::stream).collect(Collectors.toUnmodifiableList());

			// neither → ask both
			if (!hasCity && !hasState) {
				return new ChatResponse(nlp.generateQuestion(List.of("city", "state"), st), false, suggestions, st);
			}

			// search by whichever you have
			List<Hotel> found = hasCity && hasState ? hotelService.findByCityAndState(st.getCity(), st.getState())
					: hasCity ? hotelService.findByCity(st.getCity()) : hotelService.findByState(st.getState());
			st.setLastHotels(found);

			// no hotels → re-ask only what they gave
			if (found.isEmpty()) {
				ActionResult none = new ActionResult("no_hotels_location",
						Map.of("city", hasCity ? st.getCity() : "", "state", hasState ? st.getState() : ""));
				String reply = nlp.renderActionReply(none, st);

				// **** RESET city and state when no results found! ****
				st.setCity(null);
				st.setState(null);

				// Always re-ask for BOTH city and state
				String follow = nlp.generateQuestion(List.of("city", "state"), st);
				return new ChatResponse(reply + "\n\n" + follow, false, suggestions, st);
			}

			// hotels found → list & advance to ASK_DATES
			ActionResult list = new ActionResult("hotelList_location", Map.of("hotels",
					found.stream()
							.map(h -> Map.<String, Object>of("id", h.getHotelId(), "name", h.getHotelName(), "stars",
									h.getStarRating(), "price", h.getAveragePrice(), "amenities",
									hotelService.getAmenityNamesForHotel(h.getHotelId())))
							.toList(),
					"city", st.getCity() == null ? "" : st.getCity(), "state",
					st.getState() == null ? "" : st.getState()));
			String reply = nlp.renderActionReply(list, st);
			st.setStage(ConversationState.Stage.ASK_DATES);
			String follow = nlp.generateQuestion(List.of("checkIn", "checkOut"), st);
			return new ChatResponse(reply + "\n\n" + follow, false,
					getSuggestionsForStage(ConversationState.Stage.ASK_DATES, st), st);
		}

		if (stage == ConversationState.Stage.ASK_DATES) {
			StateConfig cfg = workflow.get(stage);
			Map<String, String> slots = nlp.fillSlots(cfg.slotsNeeded(), userMsg);
			String in = slots.get("checkIn");
			String out = slots.get("checkOut");
			if (in != null && !in.isBlank())
				st.setCheckIn(LocalDate.parse(in.trim()));
			if (out != null && !out.isBlank())
				st.setCheckOut(LocalDate.parse(out.trim()));

			st.setStage(ConversationState.Stage.ASK_GUESTS);
			String follow = nlp.generateQuestion(workflow.get(ConversationState.Stage.ASK_GUESTS).slotsNeeded(), st);
			return new ChatResponse(follow, false, getSuggestionsForStage(ConversationState.Stage.ASK_GUESTS, st), st);
		}

		// ========== 2. ASK_GUESTS ===============
		if (stage == ConversationState.Stage.ASK_GUESTS) {
			StateConfig cfg = workflow.get(stage);
			Map<String, String> slots = nlp.fillSlots(cfg.slotsNeeded(), userMsg);
			String raw = slots.get("guests");
			Integer guests = (raw == null || raw.isBlank()) ? null : Integer.valueOf(raw.trim());
			st.setGuests(guests);

			// missing or invalid → re-ask
			if (guests == null || guests <= 0) {
				ActionResult askAgain = new ActionResult("askGuests", Map.of());
				String reply = nlp.renderActionReply(askAgain, st);
				return new ChatResponse(reply, false, getSuggestionsForStage(ConversationState.Stage.ASK_GUESTS, st),
						st);
			}

			// valid → advance to filters
			st.setStage(ConversationState.Stage.ASK_FILTERS);
			String follow = nlp.generateQuestion(workflow.get(ConversationState.Stage.ASK_FILTERS).slotsNeeded(), st);
			return new ChatResponse(follow, false, getSuggestionsForStage(ConversationState.Stage.ASK_FILTERS, st), st);
		}

		// ========== 2. ASK_FILTERS ===============
		if (stage == ConversationState.Stage.ASK_FILTERS) {
			StateConfig cfg = workflow.get(stage);
			Map<String, String> slots = nlp.fillSlots(cfg.slotsNeeded(), userMsg);

			List<Hotel> base = Optional.ofNullable(st.getLastHotels()).orElse(List.of());
			if (base.isEmpty()) {
				ActionResult none = new ActionResult("no_hotels", Map.of("city",
						st.getCity() == null ? "" : st.getCity(), "state", st.getState() == null ? "" : st.getState()));
				String reply = nlp.renderActionReply(none, st);

				boolean hadCity = st.getCity() != null && !st.getCity().isBlank();
				boolean hadState = st.getState() != null && !st.getState().isBlank();
				List<String> toReask = hadCity && hadState ? List.of("city", "state")
						: hadCity ? List.of("city") : List.of("state");

				String follow = nlp.generateQuestion(toReask, st);
				return new ChatResponse(reply + "\n\n" + follow, false,
						getSuggestionsForStage(ConversationState.Stage.ASK_FILTERS, st), st);
			}

			boolean anyFilter = slots.values().stream().anyMatch(v -> v != null && !v.isBlank());

			// CASE A: no filters → list base
			if (!anyFilter) {
				return listHotels(base, st, ConversationState.Stage.SHOW_HOTELS, "hotelList_location");
			}

			// CASE B/C: apply filters
			SlotUtil.applySlots(st, slots, hotelService);
			List<Hotel> filtered = hotelService.filter(base, st.getMinStars(), st.getMinPrice(), st.getMaxPrice(),
					st.getRequiredAmenities());

			// CASE C: filters but none matched → re-list base
			// CASE C: filters but none matched → re-list base with "filter_failed"
			// ActionResult
			if (filtered.isEmpty()) {
				ActionResult fail = new ActionResult("filter_failed", Map.of("filters", slots, // what user requested
						"city", st.getCity() == null ? "" : st.getCity(), "state",
						st.getState() == null ? "" : st.getState(), "hotels",
						base.stream()
								.map(h -> Map.<String, Object>of("id", h.getHotelId(), "name", h.getHotelName(),
										"stars", h.getStarRating(), "price", h.getAveragePrice(), "amenities",
										hotelService.getAmenityNamesForHotel(h.getHotelId())))
								.toList()));
				String reply = nlp.renderActionReply(fail, st);
				st.setLastHotels(base); // Show the city/state hotels for selection
				st.setStage(ConversationState.Stage.SHOW_HOTELS);
				String follow = nlp.generateQuestion(workflow.get(ConversationState.Stage.SHOW_HOTELS).slotsNeeded(),
						st);
				return new ChatResponse(reply + "\n\n" + follow, false,
						getSuggestionsForStage(ConversationState.Stage.SHOW_HOTELS, st), st);
			}

			// CASE B: filters + matches → show filtered
			st.setLastHotels(filtered);
			return listHotels(filtered, st, ConversationState.Stage.SHOW_HOTELS, "hotelList_filtered");
		}

		// ========== 3. SHOW_HOTELS ===============
		// ---- 1. Select hotel using LLM/NLP extraction (SHOW_HOTELS) ----
		if (stage == ConversationState.Stage.SHOW_HOTELS) {
			List<Hotel> hotels = Optional.ofNullable(st.getLastHotels()).orElse(List.of());
			if (hotels.isEmpty()) {
				ActionResult none = new ActionResult("no_hotels_location", Map.of("city",
						st.getCity() == null ? "" : st.getCity(), "state", st.getState() == null ? "" : st.getState()));
				String reply = nlp.renderActionReply(none, st);
				return new ChatResponse(reply, false, getSuggestionsForStage(ConversationState.Stage.SHOW_HOTELS, st),
						st);
			}
			if (hotels.size() == 1) {
				Hotel h = hotels.get(0);
				st.setHotelId(h.getHotelId());
				st.setHotelName(h.getHotelName());
				st.setChosenHotel(h);
				List<RoomType> availableRoomTypes = roomTypeService.getRoomTypesForHotel(h.getHotelId());
				st.setLastRoomTypes(availableRoomTypes);
				return askServices(h, st);
			}

			List<String> hotelNames = hotels.stream().map(Hotel::getHotelName).toList();
			String hotelPrompt = "Available hotels:\n" + IntStream.range(0, hotelNames.size())
					.mapToObj(i -> (i + 1) + ". " + hotelNames.get(i)).collect(Collectors.joining("\n"))
					+ "\n\nUser input: " + userMsg;

			Map<String, String> slots = nlp.fillSlots(List.of("hotelName"), hotelPrompt);
			String pickedHotelName = slots.get("hotelName");
			Hotel chosen = null;
			if (pickedHotelName != null && !pickedHotelName.trim().isEmpty()) {
				for (Hotel h : hotels) {
					if (h.getHotelName().equalsIgnoreCase(pickedHotelName.trim())) {
						chosen = h;
						break;
					}
				}
			}
			System.out.println("LLM extracted hotelName: '" + pickedHotelName + "'");
			System.out.println("Hotel options: " + hotelNames);
			if (chosen != null) {
				System.out.println("IN SHOW_HOTELS: about to check if chosen != null for hotel selection...");
				st.setHotelId(chosen.getHotelId());
				st.setHotelName(chosen.getHotelName());
				st.setChosenHotel(chosen);

				System.out.println("[DEBUG] In hotel selection branch, chosen hotel = " + chosen.getHotelName()
						+ ", id = " + chosen.getHotelId());
				List<RoomType> availableRoomTypes = roomTypeService.getRoomTypesForHotel(chosen.getHotelId());
				System.out.println("DEBUG: Room types for hotel " + chosen.getHotelId() + ": "
						+ availableRoomTypes.stream().map(RoomType::getName).collect(Collectors.joining(", ")));
				st.setLastRoomTypes(availableRoomTypes);

				return askServices(chosen, st);
			}
			// Otherwise: relist with prompt
			return listHotelsWithIndex(hotels, st);
		}

		// ---- 2. Select (multiple) services using LLM/NLP extraction (ASK_SERVICES)
		// ----
		// … inside public ChatResponse chat(...)
		if (stage == ConversationState.Stage.ASK_SERVICES) {
			// 1) get the menu
			List<ServiceOption> allOpts = Optional.ofNullable(st.getLastServiceOptions()).orElse(List.of());
			List<String> menuNames = allOpts.stream().map(ServiceOption::getName).toList();

			// 2) build an extraction action
			ActionResult extractAct = new ActionResult("extractServices",
					Map.of("availableServices", menuNames, "userInput", userMsg));
			// 3) render only the JSON‐prompt
			String jsonPrompt = nlp.renderActionReply(extractAct, st);

			// 4) ask GPT to fill only the serviceNames array
			Map<String, String> slots = nlp.fillSlots(List.of("serviceNames"), jsonPrompt);
			String chosenRaw = slots.get("serviceNames"); // e.g. "Pool Access, Breakfast"

			// 5) none → skip all services
			if (chosenRaw == null || chosenRaw.isBlank() || chosenRaw.equalsIgnoreCase("none")) {
				st.setChosenServiceOptions(List.of());
				st.setStage(ConversationState.Stage.ASK_ROOM_TYPE);
				String q = nlp.generateQuestion(List.of("roomType"), st);
				return new ChatResponse(q, false, getSuggestionsForStage(ConversationState.Stage.ASK_ROOM_TYPE, st),
						st);
			}

			// 6) map back to your objects
			Set<String> pickedNames = Arrays.stream(chosenRaw.split(",")).map(String::trim).collect(Collectors.toSet());
			List<ServiceOption> picked = allOpts.stream().filter(o -> pickedNames.contains(o.getName())).toList();

			// 7) if nothing matched, re-ask exactly as before
			if (picked.isEmpty()) {
				ActionResult fail = new ActionResult("serviceNotMatched",
						Map.of("invalidServices", List.of(chosenRaw), "availableServices", menuNames));
				String rep = nlp.renderActionReply(fail, st);
				st.setStage(ConversationState.Stage.ASK_SERVICES);
				String follow = nlp.generateQuestion(List.of("serviceNames"), st);
				return new ChatResponse(rep + "\n\n" + follow, false,
						getSuggestionsForStage(ConversationState.Stage.ASK_SERVICES, st), st);
			}

			// 8) save and subtotal
			st.setChosenServiceOptions(picked);
			long nights = ChronoUnit.DAYS.between(st.getCheckIn(), st.getCheckOut());
			int guests = Optional.ofNullable(st.getGuests()).orElse(1);
			double sub = 0;
			for (ServiceOption so : picked) {
				double price = so.getPrice();
				sub += Boolean.TRUE.equals(so.getPerPerson()) ? price * guests * Math.max(1, nights)
						: price * Math.max(1, nights);
			}
			st.setSubtotal(sub);

			ActionResult ok = new ActionResult("serviceSubtotal",
					Map.of("picked", picked.stream().map(ServiceOption::getName).toList(), "subtotal", sub));
			String rep = nlp.renderActionReply(ok, st);
			st.setStage(ConversationState.Stage.ASK_ROOM_TYPE);
			String follow = nlp.generateQuestion(List.of("roomType"), st);
			return new ChatResponse(rep + "\n\n" + follow, false,
					getSuggestionsForStage(ConversationState.Stage.ASK_ROOM_TYPE, st), st);
		}

		// ---- 3. Select (single) room type using LLM/NLP extraction (ASK_ROOM_TYPE)
		// ----
		if (stage == ConversationState.Stage.ASK_ROOM_TYPE) {
			List<RoomType> allTypes = Optional.ofNullable(st.getLastRoomTypes()).orElse(List.of());
			List<String> typeNames = allTypes.stream().map(RoomType::getName).toList();

			String roomTypePrompt = "Available room types:\n" + IntStream.range(0, typeNames.size())
					.mapToObj(i -> (i + 1) + ". " + typeNames.get(i)).collect(Collectors.joining("\n"))
					+ "\n\nUser input: " + userMsg;

			Map<String, String> slots = nlp.fillSlots(List.of("roomType"), roomTypePrompt);
			String chosenRaw = slots.get("roomType");

			if (chosenRaw == null || chosenRaw.trim().isEmpty()) {
				String reply = nlp.generateQuestion(List.of("roomType"), st);
				return new ChatResponse(reply, false, getSuggestionsForStage(ConversationState.Stage.ASK_ROOM_TYPE, st),
						st);
			}

			// Find RoomType entity by name
			RoomType pickedType = null;
			for (RoomType rt : allTypes) {
				if (rt.getName().equalsIgnoreCase(chosenRaw.trim())) {
					pickedType = rt;
					break;
				}
			}

			if (pickedType == null) {
				// RoomType not found, re-prompt user
				ActionResult fail = new ActionResult("roomTypeNotMatched",
						Map.of("triedName", chosenRaw, "availableRooms", typeNames));
				String reply = nlp.renderActionReply(fail, st);
				String follow = nlp.generateQuestion(List.of("roomType"), st);
				return new ChatResponse(reply + "\n\n" + follow, false,
						getSuggestionsForStage(ConversationState.Stage.ASK_ROOM_TYPE, st), st);
			}

			// Now fetch HotelRoom with matching hotelId and RoomType.typeId
			Integer hotelId = st.getHotelId();
			Integer roomTypeId = pickedType.getTypeId();

			// Your service to find HotelRoom (implement in HotelRoomService or use repo)
			Optional<HotelRoom> optRoom = hotelRoomService.findHotelRoomByHotelAndType(hotelId, roomTypeId);

			if (pickedType == null || optRoom.isEmpty()) {
				ActionResult roomTypeNoMatch = new ActionResult("roomTypeNotMatched", Map.of("triedName", chosenRaw,
						"availableRooms", allTypes.stream().map(RoomType::getName).toList()));
				String reply = nlp.renderActionReply(roomTypeNoMatch, st);
				String follow = nlp.generateQuestion(List.of("roomType"), st);
				return new ChatResponse(reply + "\n\n" + follow, false,
						getSuggestionsForStage(ConversationState.Stage.ASK_ROOM_TYPE, st), st);
			}

			HotelRoom hotelRoom = optRoom.get();

			// Save selection in conversation state
			st.setRoomType(pickedType.getName());
			st.setHotelRoomId(hotelRoom.getHotelRoomId());
			st.setRoomPrice((double) hotelRoom.getPrice());
			// You can also save the room price for subtotal calculation later if you want
			// e.g., st.setRoomPrice(hotelRoom.getPrice());

			st.setStage(ConversationState.Stage.ASK_NUM_ROOMS);
			String reply = nlp.generateQuestion(List.of("noRooms"), st);
			return new ChatResponse(reply, false, getSuggestionsForStage(ConversationState.Stage.ASK_NUM_ROOMS, st),
					st);
		}
		if (stage == ConversationState.Stage.ASK_NUM_ROOMS) {
			StateConfig cfg = workflow.get(stage);
			Map<String, String> slots = nlp.fillSlots(cfg.slotsNeeded(), userMsg);
			String noRoomsRaw = slots.get("noRooms");

			// LLM-parsed number (string), convert to Integer if present
			Integer noRooms = noRoomsRaw == null || noRoomsRaw.isBlank() ? null : Integer.valueOf(noRoomsRaw.trim());
			st.setNoRooms(noRooms);

			if (noRooms == null || noRooms <= 0) {
				// Slot missing or invalid, return an ActionResult that will prompt user to
				// re-enter number of rooms
				ActionResult askAgain = new ActionResult("askNumRooms", Map.of());
				String reply = nlp.renderActionReply(askAgain, st);
				return new ChatResponse(reply, false, getSuggestionsForStage(ConversationState.Stage.ASK_NUM_ROOMS, st),
						st);
			}

			// Valid input — advance state
			st.setStage(ConversationState.Stage.ASK_CUSTOMER_NAME);

			// Return an ActionResult that confirms the number of rooms and prompts for
			// customer name
			ActionResult confirm = new ActionResult("confirmRooms", Map.of("noRooms", noRooms));
			String reply = nlp.renderActionReply(confirm, st);

			String followUp = nlp.generateQuestion(List.of("customerName"), st);

			// Return combined response
			return new ChatResponse(reply + "\n\n" + followUp, false,
					getSuggestionsForStage(ConversationState.Stage.ASK_CUSTOMER_NAME, st), st);
		}

		if (stage == ConversationState.Stage.ASK_CUSTOMER_NAME) {
			StateConfig cfg = workflow.get(stage);
			Map<String, String> slots = nlp.fillSlots(cfg.slotsNeeded(), userMsg);
			String customerName = slots.get("customerName");
			st.setCustomerName(customerName);

			// Get service list for summary
			List<String> services = st.getChosenServiceOptions() != null
					? st.getChosenServiceOptions().stream().map(ServiceOption::getName).toList()
					: List.of();

			int guests = Optional.ofNullable(st.getGuests()).orElse(1);
			int noRooms = Optional.ofNullable(st.getNoRooms()).orElse(1);
			long nights = java.time.temporal.ChronoUnit.DAYS.between(st.getCheckIn(), st.getCheckOut());
			double roomPrice = st.getRoomPrice();
			double serviceTotal = 0.0;
			if (st.getChosenServiceOptions() != null) {
				for (ServiceOption so : st.getChosenServiceOptions()) {
					double price = so.getPrice();
					if (Boolean.TRUE.equals(so.getPerPerson()))
						price *= guests * nights;
					else
						price *= nights;
					serviceTotal += price;
				}
			}
			double subtotal = roomPrice * noRooms + serviceTotal;
			st.setSubtotal(subtotal);

			// Put all booking summary data in ActionResult; no reply formatting here
			Map<String, Object> data = new HashMap<>();
			if (st.getHotelName() != null)
				data.put("hotelName", st.getHotelName());
			if (st.getCheckIn() != null)
				data.put("checkIn", st.getCheckIn());
			if (st.getCheckOut() != null)
				data.put("checkOut", st.getCheckOut());
			data.put("guests", guests);
			data.put("rooms", noRooms);
			if (services != null)
				data.put("services", services);
			data.put("roomPrice", roomPrice);
			data.put("serviceTotal", serviceTotal);
			data.put("subtotal", subtotal);
			if (customerName != null)
				data.put("customerName", customerName);

			ActionResult summary = new ActionResult("showSubtotal", data);
			String reply = nlp.renderActionReply(summary, st); // LLM will construct the actual prompt

			st.setStage(ConversationState.Stage.SHOW_SUBTOTAL);
			return new ChatResponse(reply, false, getSuggestionsForStage(ConversationState.Stage.SHOW_SUBTOTAL, st),
					st);
		}
		if (stage == ConversationState.Stage.SHOW_SUBTOTAL) {
			StateConfig cfg = workflow.get(stage);
			Map<String, String> slots = nlp.fillSlots(cfg.slotsNeeded(), userMsg);

			Boolean confirmBooking = Boolean.valueOf(slots.get("confirmBooking"));
			st.setConfirmBook(confirmBooking);

			if (confirmBooking == null || !confirmBooking) {
				st.setStage(ConversationState.Stage.ASK_ANOTHER);
				ActionResult another = new ActionResult("askAnotherBooking",
						Map.of("customerName", st.getCustomerName()));
				String reply = nlp.renderActionReply(another, st);
				return new ChatResponse(reply, false, getSuggestionsForStage(ConversationState.Stage.ASK_ANOTHER, st),
						st);
			} else {
				// Only now create the booking
				List<String> services = st.getChosenServiceOptions() != null
						? st.getChosenServiceOptions().stream().map(ServiceOption::getName).toList()
						: List.of();
				int guests = Optional.ofNullable(st.getGuests()).orElse(1);
				int noRooms = Optional.ofNullable(st.getNoRooms()).orElse(1);
				double subtotal = st.getSubtotal();

				Booking booking = bookingService.create(st.getHotelId(), st.getHotelRoomId(), noRooms, guests,
						st.getCheckIn(), st.getCheckOut(), st.getCustomerName(), String.join(",", services), subtotal);
				st.getBookingIds().add(booking.getBookingId());

				// Send all booking info through ActionResult
				ActionResult confirmed = new ActionResult("bookingConfirmed",
						Map.of("bookingId", booking.getBookingId(), "subtotal", subtotal, "hotelName",
								st.getHotelName(), "customerName", st.getCustomerName(), "room", st.getHotelRoomId(),
								"checkIn", st.getCheckIn(), "checkOut", st.getCheckOut(), "guests", guests, "rooms",
								noRooms, "services", services));
				String reply = nlp.renderActionReply(confirmed, st);

				st.setStage(cfg.nextState().apply(st)); // To ASK_ANOTHER
				return new ChatResponse(reply, false, getSuggestionsForStage(st.getStage(), st), st);
			}
		}

		if (stage == ConversationState.Stage.ASK_ANOTHER) {
			StateConfig cfg = workflow.get(stage);
			Map<String, String> slots = nlp.fillSlots(cfg.slotsNeeded(), userMsg);

			Boolean confirmAnother = Boolean.valueOf(slots.get("confirmAnother"));
			st.setConfirmAnother(confirmAnother);

			ActionResult next;
			if (Boolean.TRUE.equals(confirmAnother)) {

				List<Integer> kept = new ArrayList<>(st.getBookingIds());
				// 1) wipe out everything
				st.reset();
				st.setBookingIds(kept);

				// 2) use your workflow to ask GPT to craft the fresh prompt
				next = new ActionResult("startNewBooking", Map.of());
				String reply = nlp.renderActionReply(next, st);

				// 3) suggestions for ASK_LOCATION
				List<String> suggestions = getSuggestionsForStage(st.getStage(), st);
				return new ChatResponse(reply, false, suggestions, st);
			} else {
				st.setStage(cfg.nextState().apply(st)); // Could be END, ASK_FEEDBACK, etc.
				next = new ActionResult("proceedToPayment", Map.of("customerName", st.getCustomerName()));
			}
			String reply = nlp.renderActionReply(next, st);
			return new ChatResponse(reply, false, getSuggestionsForStage(st.getStage(), st), st);
		}

		if (stage == ConversationState.Stage.ASK_PAYMENT) {
			StateConfig cfg = workflow.get(stage);
			Map<String, String> slots = nlp.fillSlots(cfg.slotsNeeded(), userMsg);

			Boolean confirmPayment = Boolean.parseBoolean(slots.get("confirmPayment"));
			if (confirmPayment) {
				bookingService.chargeAll(st.getBookingIds());
				st.setPaymentDone(true);
			}

			ActionResult payResult = new ActionResult("payment", Map.of("paid", confirmPayment));
			String reply = nlp.renderActionReply(payResult, st);

			st.setStage(cfg.nextState().apply(st)); // e.g. ASK_FEEDBACK
			String follow = nlp.generateQuestion(workflow.get(st.getStage()).slotsNeeded(), st);

			return new ChatResponse(reply + "\n\n" + follow, false, getSuggestionsForStage(st.getStage(), st), st);
		}

		if (stage == ConversationState.Stage.ASK_FEEDBACK) {
			StateConfig cfg = workflow.get(stage);
			Map<String, String> slots = nlp.fillSlots(cfg.slotsNeeded(), userMsg);

			String ratingRaw = slots.get("feedbackRating");
			// re-ask if they didn’t actually give a rating
			if (ratingRaw == null || ratingRaw.isBlank()) {
				// prompt only for the missing rating
				String question = nlp.generateQuestion(List.of("feedbackRating"), st);
				return new ChatResponse(question, false,
						getSuggestionsForStage(ConversationState.Stage.ASK_FEEDBACK, st), st);
			}

			Integer rating = Integer.parseInt(slots.get("feedbackRating"));
			String comments = slots.get("feedbackComments");

			st.setFeedbackRating(rating);
			st.setFeedbackComments(comments);

			feedbackService.save(st.getBookingIds(), rating, comments);

			ActionResult feedbackSaved = new ActionResult("endBookingSession", Map.of());
			String reply = nlp.renderActionReply(feedbackSaved, st);

			st.setStage(cfg.nextState().apply(st)); // DONE
			return new ChatResponse(reply, true, getSuggestionsForStage(st.getStage(), st), st);
		}
		// ========== 4. All Other Stages ===============
		StateConfig cfg = workflow.get(stage);

		// Slot‐fill any missing slots
		List<String> missing = cfg.slotsNeeded().stream().filter(f -> !hasField(st, f))
				.filter(f -> stage == ConversationState.Stage.ASK_LOCATION || (!f.equals("city") && !f.equals("state")))
				.toList();
		if (!missing.isEmpty()) {
			Map<String, String> extracted = nlp.fillSlots(missing, userMsg);
			SlotUtil.applySlots(st, extracted, hotelService);

			List<String> still = cfg.slotsNeeded().stream().filter(f -> !hasField(st, f)).toList();
			if (!still.isEmpty()) {
				String q = nlp.generateQuestion(still, st);
				return new ChatResponse(q, false, getSuggestionsForStage(stage, st), st);
			}
		}

		ActionResult result = cfg.action() == null ? null : cfg.action().apply(st, Map.of());
		String actionReply = result == null ? "" : nlp.renderActionReply(result, st);

		ConversationState.Stage next = cfg.nextState().apply(st);
		st.setStage(next);
		String followUp = nlp.generateQuestion(workflow.get(next).slotsNeeded(), st);

		String full = actionReply.isBlank() ? followUp : actionReply + "\n\n" + followUp;
		boolean done = next == ConversationState.Stage.DONE;
		return new ChatResponse(full, done, getSuggestionsForStage(next, st), st);
	}

	// ========== Helper Methods ==============

	private ChatResponse listHotels(List<Hotel> list, ConversationState st, ConversationState.Stage nextStage,
			String actionName) {
		ActionResult r = new ActionResult(actionName,
				Map.of("hotelList_filtered",
						list.stream()
								.map(h -> Map.<String, Object>of("id", h.getHotelId(), "name", h.getHotelName(),
										"stars", h.getStarRating(), "price", h.getAveragePrice(), "amenities",
										hotelService.getAmenityNamesForHotel(h.getHotelId())))
								.toList(),
						"city", st.getCity() == null ? "" : st.getCity(), "state",
						st.getState() == null ? "" : st.getState()));
		String reply = nlp.renderActionReply(r, st);
		st.setStage(nextStage);
		String follow = nlp.generateQuestion(workflow.get(nextStage).slotsNeeded(), st);
		return new ChatResponse(reply + "\n\n" + follow, false, List.of(), st);
	}

	private ChatResponse listHotelsWithIndex(List<Hotel> list, ConversationState st) {
		List<Map<String, Object>> data = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			Hotel h = list.get(i);
			data.add(Map.of("index", i + 1, "id", h.getHotelId(), "name", h.getHotelName(), "stars", h.getStarRating(),
					"price", h.getAveragePrice(), "amenities", hotelService.getAmenityNamesForHotel(h.getHotelId())));
		}
		ActionResult r = new ActionResult("hotelList", Map.of("hotels", data, "city",
				st.getCity() == null ? "" : st.getCity(), "state", st.getState() == null ? "" : st.getState()));
		String reply = nlp.renderActionReply(r, st);
		String follow = nlp.generateQuestion(List.of("hotelId", "hotelName"), st);
		return new ChatResponse(reply + "\n\n" + follow, false, List.of(), st);
	}

	private ChatResponse askServices(Hotel h, ConversationState st) {
		List<ServiceOption> options = hotelService.getServiceOptions(h.getHotelId());
		st.setLastServiceOptions(options);

		List<Map<String, Object>> serviceList = options == null ? new ArrayList<>()
				: options.stream().map(s -> Map.<String, Object>of("name", s.getName(), "price", s.getPrice()))
						.toList();

		ActionResult details = new ActionResult("hotelDetails",
				Map.of("id", h.getHotelId(), "name", h.getHotelName(), "stars", h.getStarRating(), "price",
						h.getAveragePrice(), "amenities", hotelService.getAmenityNamesForHotel(h.getHotelId()),
						"services", serviceList));

		// Only ask for services here (not room type yet)!
		String reply = nlp.renderActionReply(details, st);
		st.setStage(ConversationState.Stage.ASK_SERVICES);
		String follow = nlp.generateQuestion(List.of("chosenServices"), st);
		return new ChatResponse(reply + "\n\n" + follow, false,
				getSuggestionsForStage(ConversationState.Stage.ASK_SERVICES, st), st);
	}

	private boolean hasField(ConversationState st, String f) {
		return switch (f) {
		case "city" -> st.getCity() != null;
		case "state" -> st.getState() != null;
		case "checkIn" -> st.getCheckIn() != null;
		case "checkOut" -> st.getCheckOut() != null;
		case "guests" -> st.getGuests() != null;
		case "minStars" -> st.getMinStars() != null;
		case "minPrice" -> st.getMinPrice() != null;
		case "maxPrice" -> st.getMaxPrice() != null;
		case "requiredAmenities" -> !st.getRequiredAmenities().isEmpty();
		case "hotelId" -> st.getHotelId() != null;
		case "chosenServices" -> !st.getChosenServices().isEmpty();
		case "roomType" -> st.getRoomType() != null;
		case "noRooms" -> st.getNoRooms() != null;
		case "customerName" -> st.getCustomerName() != null;
		case "confirmBooking" -> st.getConfirmBook() != null;
		case "confirmAnother" -> st.getConfirmAnother() != null;
		case "confirmAddAnother" -> st.getConfirmAddAnother() != null;
		case "confirmPayment" -> st.getPaymentDone() != null;
		case "feedbackRating" -> st.getFeedbackRating() != null;
		case "feedbackComments" -> st.getFeedbackComments() != null;
		default -> false;
		};
	}

	private List<String> getSuggestionsForStage(ConversationState.Stage stage, ConversationState st) {
		return switch (stage) {
		case ASK_LOCATION -> Stream.of(hotelService.getAllCities(), hotelService.getAllStates())
				.flatMap(Collection::stream).collect(Collectors.toUnmodifiableList());

		case ASK_DATES -> {
			// pick a “base” check-in: either what they already set, or tomorrow
			LocalDate baseIn = st.getCheckIn() != null ? st.getCheckIn() : LocalDate.now().plusDays(1);
			// build two or three examples of (checkIn → checkOut)
			String option1 = baseIn.toString() + " → " + baseIn.plusDays(1);
			String option2 = baseIn.plusDays(2).toString() + " → " + baseIn.plusDays(3);
			String option3 = "Next Monday → Next Wednesday";
			yield List.of(option1, option2, option3);
		}

		case ASK_GUESTS -> List.of("1", "2", "3", "4", "5");

		case ASK_FILTERS -> {
			List<String> suggestions = new ArrayList<>();
			// star‐rating range
			suggestions.add("1–5 stars");
			// price range
			suggestions.add("0–500$");
			// all amenities
			suggestions.addAll(amenityService.getAllAmenities());
			yield suggestions;
		}

		case SHOW_HOTELS ->
			Optional.ofNullable(st.getLastHotels()).orElse(List.of()).stream().map(Hotel::getHotelName).toList();

		case ASK_SERVICES -> Optional.ofNullable(st.getLastServiceOptions()).orElse(List.of()).stream()
				.map(opt -> opt.getName() + " – $" + String.format("%.2f", opt.getPrice())).toList();

		case ASK_ROOM_TYPE -> st.getLastRoomTypes().stream().map(RoomType::getName).toList();

		case ASK_NUM_ROOMS -> List.of("1", "2", "3", "4");

		case ASK_CUSTOMER_NAME -> List.of("Provide the name as in the ID card");

		case SHOW_SUBTOTAL, ASK_PAYMENT, ASK_ANOTHER -> List.of("Yes", "No");

		case ASK_FEEDBACK -> List.of("1", "2", "3", "4", "5");

		default -> List.of();
		};
	}

}
