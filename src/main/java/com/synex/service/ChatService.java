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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ChatService {

	@Autowired
	private NLPService nlp;
	@Autowired
	private HotelService hotelService;
	@Autowired
	private BookingService bookingService;
	@Autowired
	private HotelRepository hotelRepo;
	@Autowired
	private RoomTypeRepository roomTypeRepo;

	private static final Logger log = LoggerFactory.getLogger(ChatService.class);

	public ChatResponse chat(ChatRequest req) {
		String user = req.getMessage();
		ConversationState st = Optional.ofNullable(req.getState()).orElse(new ConversationState());
		ParseResult pr = nlp.parse(user, st.getStage().name());
		mergeSlots(st, pr);

		// Universal user requests (cancel, restart, etc) can go here...

		// "Smart" stage logic — fill all available slots at once, moving as far as
		// possible
		// Keep looping forward as long as possible
		outer: while (true) {
			switch (st.getStage()) {
			case START:
				st.setStage(Stage.ASK_CITY);
				return new ChatResponse("Hi there! Welcome to StaySmart Hotels. Which city/state?", false,
						hotelService.getAllCities(), st);

			case ASK_CITY:
				if (st.getCity() == null)
					break outer;
				st.setStage(Stage.ASK_DATES);
				// but...maybe user gave dates too, so fall through!

			case ASK_DATES:
				if (st.getCheckIn() == null || st.getCheckOut() == null) {
					return new ChatResponse("When will you check in/out? (e.g. May 5, 2025 to May 8, 2025)", false,
							List.of("May 5, 2025 to May 8, 2025"), st);
				}
				st.setStage(Stage.ASK_GUESTS);

			case ASK_GUESTS:
				if (st.getGuests() == null) {
					return new ChatResponse("How many guests?", false, List.of("2 adults"), st);
				}
				st.setStage(Stage.ASK_FILTERS);

			case ASK_FILTERS:
				// Slot filling for filters
				String rawFilt = Optional.ofNullable(pr.getRawText()).map(String::toLowerCase).orElse("");
				if (rawFilt.contains("no filters")) {
					st.setMinStars(null);
					st.setMinPrice(null);
					st.setMaxPrice(null);
					st.getRequiredAmenities().clear();
				} else {
					if (pr.getMinStars() != null)
						st.setMinStars(pr.getMinStars());
					if (pr.getMinPrice() != null)
						st.setMinPrice(pr.getMinPrice());
					if (pr.getMaxPrice() != null)
						st.setMaxPrice(pr.getMaxPrice());
					if (pr.getAmenities() != null)
						st.setRequiredAmenities(new HashSet<>(pr.getAmenities()));
				}

				List<Hotel> base = hotelService.searchByNameOrLocation(st.getCity());
				List<Hotel> matches = hotelService.filter(base, st.getMinStars(), st.getMinPrice(), st.getMaxPrice(),
						st.getRequiredAmenities());
				if (matches.isEmpty()) {
					st.setStage(Stage.ASK_FILTER_REFINE);
					return new ChatResponse("No hotels found with those filters. Remove filters? (yes/no)", false,
							List.of("yes", "no"), st);
				}

				st.setLastHotels(matches);
				st.setStage(Stage.SHOW_HOTELS);

			case SHOW_HOTELS: {
				List<Hotel> hotels = st.getLastHotels();
				if (hotels == null || hotels.isEmpty()) {
					hotels = hotelService.searchByNameOrLocation(st.getCity());
					st.setLastHotels(hotels);
				}
				String raw = Optional.ofNullable(pr.getRawText()).orElse("").trim();
				Hotel chosen = null;
				// Try numeric index
				if (pr.getHotelIndex() != null) {
					int idx = pr.getHotelIndex();
					if (idx >= 1 && idx <= hotels.size()) {
						chosen = hotels.get(idx - 1);
						st.setSelectedHotelIndex(idx);
					}
				}
				// Try exact-name/substring match
				if (chosen == null && !raw.isBlank()) {
					String lowerRaw = raw.toLowerCase();
					for (int i = 0; i < hotels.size(); i++) {
						Hotel h = hotels.get(i);
						String lowerName = h.getHotelName().toLowerCase();
						if (lowerName.equals(lowerRaw) || lowerName.contains(lowerRaw)) {
							chosen = h;
							st.setSelectedHotelIndex(i + 1);
							break;
						}
					}
				}
				if (chosen != null) {
					st.setChosenHotel(chosen);
					st.setHotelName(chosen.getHotelName());
					st.setHotelId(chosen.getHotelId());
					st.setStage(Stage.SHOW_AMENITIES);
					continue outer; // try next stage immediately (for slot-filling)
				}
				// Prompt for choice if not chosen
				List<String> suggestions = new ArrayList<>();
				StringBuilder prompt = new StringBuilder(
						"Which hotel would you like to book? Reply with its *number* or *full name*:\n");
				for (int i = 0; i < hotels.size(); i++) {
					Hotel h = hotels.get(i);
					prompt.append(String.format("%d. %s — %d★, $%.2f/night\n", i + 1, h.getHotelName(),
							h.getStarRating(), h.getAveragePrice()));
					suggestions.add(String.valueOf(i + 1));
					suggestions.add(h.getHotelName());
				}
				return new ChatResponse(prompt.toString().trim(), false, suggestions, st);
			}

			case SHOW_AMENITIES: {
				Hotel hotel = st.getChosenHotel();
				if (hotel == null && st.getHotelId() != null) {
					hotel = hotelRepo.findById(st.getHotelId()).orElse(null);
					st.setChosenHotel(hotel);
				}
				if (hotel == null) {
					st.setStage(Stage.SHOW_HOTELS);
					continue outer;
				}
				List<String> ams = hotelService.getAmenityNamesForHotel(hotel.getHotelId());
				List<ServiceOption> opts = hotelService.getServiceOptions(hotel.getHotelId());
				st.setLastServiceOptions(opts);
				st.setStage(Stage.ASK_SERVICES);
				StringBuilder out = new StringBuilder().append("Excellent choice! Here are the key amenities at ")
						.append(hotel.getHotelName()).append(":\n");
				for (String a : ams)
					out.append(" • ").append(a).append("\n");
				out.append("Any add-on services?\n");
				for (int i = 0; i < opts.size(); i++) {
					ServiceOption s = opts.get(i);
					out.append(String.format("%d. %s (+$%.2f%s)\n", i + 1, s.getName(), s.getPrice(),
							s.getPerPerson() ? "/person" : ""));
				}
				List<String> suggestions = new ArrayList<>();
				for (int i = 0; i < opts.size(); i++) {
					suggestions.add(String.valueOf(i + 1));
					suggestions.add(opts.get(i).getName());
				}
				suggestions.add("No, thanks");
				return new ChatResponse(out.toString().trim(), false, suggestions, st);
			}

			case ASK_SERVICES: {
				List<ServiceOption> all = st.getLastServiceOptions();
				List<ServiceOption> picked = new ArrayList<>();
				String raw = Optional.ofNullable(pr.getRawText()).orElse("").trim();
				// No services
				if (pr.getIntent() == Intent.SERVICES_NONE || raw.equalsIgnoreCase("no")
						|| raw.equalsIgnoreCase("no, thanks")
						|| (pr.getServiceIndices() != null && pr.getServiceIndices().isEmpty())) {
					st.setChosenServiceOptions(Collections.emptyList());
					st.setChosenServices(Collections.emptyList());
				} else {
					Set<Integer> numbers = new HashSet<>();
					Matcher numMatch = Pattern.compile("\\b(\\d+)\\b").matcher(raw);
					while (numMatch.find()) {
						numbers.add(Integer.parseInt(numMatch.group(1)));
					}
					for (Integer idx : numbers) {
						if (idx >= 1 && idx <= all.size()) {
							picked.add(all.get(idx - 1));
						}
					}
					String[] parts = raw.split("(,| and )");
					for (String part : parts) {
						String candidate = part.trim();
						for (ServiceOption so : all) {
							if (so.getName().equalsIgnoreCase(candidate)) {
								if (!picked.contains(so))
									picked.add(so);
							}
						}
					}
					st.setChosenServiceOptions(picked);
					st.setChosenServices(picked.stream().map(ServiceOption::getName).collect(Collectors.toList()));
				}
				// If valid selection, move forward, else reprompt
				if (st.getChosenServiceOptions().isEmpty() && !raw.isEmpty()
						&& pr.getIntent() != Intent.SERVICES_NONE) {
					List<String> opts = IntStream.rangeClosed(1, all.size())
							.mapToObj(i -> i + ". " + all.get(i - 1).getName()).collect(Collectors.toList());
					return new ChatResponse(
							"Please pick one or more numbers or exact service *names* (not description), or say “no, thanks.”\n"
									+ opts.stream().collect(Collectors.joining("\n")),
							false, all.stream().map(ServiceOption::getName).collect(Collectors.toList()), st);
				}
				st.setStage(Stage.ASK_ROOM_TYPE);
				continue outer;
			}

			case ASK_ROOM_TYPE: {
				List<HotelRoom> rooms = hotelService.getRoomsForHotel(st.getChosenHotel().getHotelId());
				String raw = Optional.ofNullable(pr.getRawText()).orElse("").trim();
				HotelRoom chosenRoom = null;
				if (pr.getHotelIndex() != null) {
					int idx = pr.getHotelIndex();
					if (idx >= 1 && idx <= rooms.size()) {
						chosenRoom = rooms.get(idx - 1);
					}
				}
				if (chosenRoom == null && !raw.isBlank()) {
					for (HotelRoom r : rooms) {
						if (r.getType().getName().equalsIgnoreCase(raw)) {
							chosenRoom = r;
							break;
						}
					}
				}
				if (chosenRoom != null) {
					st.setChosenRoom(chosenRoom);
					st.setStage(Stage.ASK_NUM_ROOMS);
					continue outer;
				}
				List<String> opts = new ArrayList<>();
				for (int i = 0; i < rooms.size(); i++) {
					opts.add(String.valueOf(i + 1));
					opts.add(rooms.get(i).getType().getName());
				}
				return new ChatResponse("Please pick a room type by number or name:", false, opts, st);
			}

			case ASK_NUM_ROOMS: {
				Matcher m = Pattern.compile("(\\d+)").matcher(pr.getRawText());
				if (m.find()) {
					int noRooms = Integer.parseInt(m.group(1));
					st.setNoRooms(noRooms);
					st.setStage(Stage.ASK_CUSTOMER_NAME);
					continue outer;
				}
				return new ChatResponse("How many rooms? Please reply with a number.", false,
						List.of("1", "2", "3", "4"), st);
			}

			case ASK_CUSTOMER_NAME: {
				String customer = Optional.ofNullable(pr.getRawText()).orElse("").trim();
				if (!customer.isBlank()) {
					st.setCustomerName(customer);
					Booking b = bookingService.create(st.getChosenHotel().getHotelId(),
							st.getChosenRoom().getHotelRoomId(), st.getNoRooms(), st.getGuests(), st.getCheckIn(),
							st.getCheckOut(), st.getCustomerName(), String.join(",", st.getChosenServices()));
					st.getBookingIds().add(b.getBookingId());
					st.setStage(Stage.SHOW_SUBTOTAL);
					continue outer;
				}
				return new ChatResponse("Please tell me the name for the reservation.", false, Collections.emptyList(),
						st);
			}

			case SHOW_SUBTOTAL: {
				if (Boolean.TRUE.equals(pr.getConfirm())) {
					Booking b = bookingService.create(st.getChosenHotel().getHotelId(),
							st.getChosenRoom().getHotelRoomId(), st.getNoRooms(), st.getGuests(), st.getCheckIn(),
							st.getCheckOut(), st.getCustomerName(), String.join(",", st.getChosenServices()));
					st.getBookingIds().add(b.getBookingId());
				}
				st.setStage(Stage.ASK_ANOTHER);
				continue outer;
			}

			case ASK_ANOTHER:
				if (Boolean.TRUE.equals(pr.getConfirm())) {
					st.setStage(Stage.ASK_CITY);
					return new ChatResponse("Next city/state?", false, hotelService.getAllCities(), st);
				}
				st.setStage(Stage.ASK_PAYMENT);
				continue outer;

			case ASK_PAYMENT:
				if (Boolean.FALSE.equals(pr.getConfirm())) {
					st = new ConversationState();
					return new ChatResponse("Restarting. Which city/state?", false, hotelService.getAllCities(), st);
				}
				if (pr.getIntent() == Intent.PAY) {
					double total = bookingService.computeTotal(st.getBookingIds());
					st.setStage(Stage.ASK_FEEDBACK);
					return new ChatResponse("Charged $" + total + ". Thanks! Rate us? (1–5)", false,
							List.of("1", "2", "3", "4", "5"), st);
				}
				return new ChatResponse("Say “pay” or “no”.", false, List.of("pay", "no"), st);

			case ASK_FEEDBACK:
				if (pr.getRating() != null) {
					st.setStage(Stage.DONE);
					return new ChatResponse("Thank you! Have a great trip!", true, List.of(), st);
				}
				return new ChatResponse("Rate 1–5 please.", false, List.of("1", "2", "3", "4", "5"), st);

			default:
				return new ChatResponse("Goodbye!", true, List.of(), st);
			}
		} // end while
			// Fallback: prompt for missing info (shouldn't reach here)
		return new ChatResponse("Please continue.", false, List.of(), st);
	}

	private void mergeSlots(ConversationState st, ParseResult pr) {
		// Overwrite slot with any new values (enables corrections and out-of-order
		// info)
		if (pr.getCity() != null)
			st.setCity(pr.getCity());
		if (pr.getState() != null)
			st.setState(pr.getState());
		if (pr.getCheckIn() != null)
			st.setCheckIn(pr.getCheckIn());
		if (pr.getCheckOut() != null)
			st.setCheckOut(pr.getCheckOut());
		if (pr.getGuests() != null)
			st.setGuests(pr.getGuests());
		if (pr.getMinStars() != null)
			st.setMinStars(pr.getMinStars());
		if (pr.getMinPrice() != null)
			st.setMinPrice(pr.getMinPrice());
		if (pr.getMaxPrice() != null)
			st.setMaxPrice(pr.getMaxPrice());
		if (pr.getAmenities() != null)
			st.setRequiredAmenities(new HashSet<>(pr.getAmenities()));
		if (pr.getServiceQuantityDays() != null)
			st.setServiceQuantityDays(pr.getServiceQuantityDays());
		if (pr.getRoomType() != null)
			st.setRoomType(pr.getRoomType());
		// if (pr.getNoRooms() != null) st.setNoRooms(pr.getNoRooms());
		// if (pr.getCustomerName() != null) st.setCustomerName(pr.getCustomerName());
		// add payment fields as needed
	}
}
