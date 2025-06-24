package com.synex.domain;

import java.time.LocalDate;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ConversationState {
	public enum Stage {
		START, ASK_LOCATION, ASK_CITY, SHOW_ALL_HOTELS, ASK_FILTERS, SHOW_FILTERED_HOTELS, SHOW_HOTEL_DETAILS,
		ASK_SERVICES, ASK_CONFIRM_BOOK, REVIEW, ASK_PAYMENT, ASK_FEEDBACK, DONE, ASK_DATES, ASK_GUESTS, SHOW_HOTELS,
		SHOW_AMENITIES, SHOW_SUBTOTAL, ASK_ANOTHER, LIST_HOTELS, ASK_ROOM_TYPE, ASK_NAME, ASK_FILTER_REFINE,
		ASK_NUM_ROOMS, ASK_CUSTOMER_NAME, FILTER_HOTELS, ASK_AGENT_INFO, AGENT_INFO_RECEIVED, ASK_CONTINUE_BOOKING
	}

	private Stage stage = Stage.START;

	// ─── user inputs ───
	private String hotelName;
	private Integer hotelId;
	private double Subtotal;
	private String city;
	private String state;
	private LocalDate checkIn;
	private LocalDate checkOut;
	private Integer guests;
	private Integer minStars;
	private Double minPrice;
	private Double maxPrice;
	private String language;
	private Set<String> requiredAmenities = new HashSet<>();
	private String roomType; // holds the name the user types
	private Integer noRooms; // how many rooms they want
	// the name to put on the booking
	// + getters/setters…
	private double RoomPrice;

	private List<Hotel> lastHotels = new ArrayList<>();
	@JsonIgnore
	private List<Hotel> filteredHotels = new ArrayList<>();
	@JsonIgnore
	private Integer selectedHotelIndex;
	@JsonIgnore
	private Hotel chosenHotel;
	private Boolean confirmBook;
	private Boolean confirmAnother;
	@JsonIgnore
	private List<String> chosenServices = new ArrayList<>();
	private Boolean confirmAddAnother;
	@JsonIgnore
	private List<Integer> bookingIds = new ArrayList<>();
	@JsonIgnore
	private Boolean paymentDone;
	@JsonIgnore
	private Integer feedbackRating;
	@JsonIgnore
	private String feedbackComments;

	// ─── NEW FIELDS for ChatService ───

	private List<ServiceOption> lastServiceOptions = new ArrayList<>();

	private List<ServiceOption> chosenServiceOptions = new ArrayList<>();

	private Integer serviceQuantityDays;

	private List<RoomType> lastRoomTypes = new ArrayList<>();

	@JsonIgnore
	private HotelRoom chosenRoom;

	private Integer hotelRoomId;
	private Boolean confirmRefine;
	private String agentRequestName;
	private String agentRequestPhone;
	private Stage previousStage;

	private String customerName;
	private Boolean confirmContinue; // <— new
	// … getters & setters …

	public Boolean getConfirmContinue() {
		return confirmContinue;
	}

	public void setConfirmContinue(Boolean c) {
		this.confirmContinue = c;
	}

	public ConversationState() {
	}

	// ─── getters & setters for all existing fields ───
	// ... (omit for brevity; keep all your originals) ...

	// ─── getters & setters for new fields ───

	public List<ServiceOption> getLastServiceOptions() {
		return lastServiceOptions;
	}

	public Integer getNoRooms() {
		return noRooms;
	}

	public void setNoRooms(Integer noRooms) {
		this.noRooms = noRooms;
	}

	public void setLastServiceOptions(List<ServiceOption> lastServiceOptions) {
		this.lastServiceOptions = lastServiceOptions;
	}

	public List<ServiceOption> getChosenServiceOptions() {
		return chosenServiceOptions;
	}

	public void setChosenServiceOptions(List<ServiceOption> chosenServiceOptions) {
		this.chosenServiceOptions = chosenServiceOptions;
	}

	public Integer getServiceQuantityDays() {
		return serviceQuantityDays;
	}

	public void setServiceQuantityDays(Integer serviceQuantityDays) {
		this.serviceQuantityDays = serviceQuantityDays;
	}

	public List<RoomType> getLastRoomTypes() {
		return lastRoomTypes;
	}

	public void setLastRoomTypes(List<RoomType> lastRoomTypes) {
		this.lastRoomTypes = lastRoomTypes;
	}

	public Integer getHotelId() {
		return hotelId;
	}

	public void setHotelId(Integer hotelId) {
		this.hotelId = hotelId;
	}

	public String getRoomType() {
		return roomType;
	}

	public void setRoomType(String roomType) {
		this.roomType = roomType;
	}

	public Integer getHotelRoomId() {
		return hotelRoomId;
	}

	public void setHotelRoomId(Integer hotelRoomId) {
		this.hotelRoomId = hotelRoomId;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	// ─── helper ───
	/** Clear only the filter‐related slots. */
	public void clearFilters() {
		this.minStars = null;
		this.minPrice = null;
		this.maxPrice = null;
		this.requiredAmenities.clear();
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public String getHotelName() {
		return hotelName;
	}

	public void setHotelName(String hotelName) {
		this.hotelName = hotelName;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public LocalDate getCheckIn() {
		return checkIn;
	}

	public void setCheckIn(LocalDate checkIn) {
		this.checkIn = checkIn;
	}

	public LocalDate getCheckOut() {
		return checkOut;
	}

	public void setCheckOut(LocalDate checkOut) {
		this.checkOut = checkOut;
	}

	public Integer getGuests() {
		return guests;
	}

	public void setGuests(Integer guests) {
		this.guests = guests;
	}

	public Integer getMinStars() {
		return minStars;
	}

	public void setMinStars(Integer minStars) {
		this.minStars = minStars;
	}

	public Double getMinPrice() {
		return minPrice;
	}

	public void setMinPrice(Double minPrice) {
		this.minPrice = minPrice;
	}

	public Double getMaxPrice() {
		return maxPrice;
	}

	public void setMaxPrice(Double maxPrice) {
		this.maxPrice = maxPrice;
	}

	public Set<String> getRequiredAmenities() {
		return requiredAmenities;
	}

	public void setRequiredAmenities(Set<String> requiredAmenities) {
		this.requiredAmenities = requiredAmenities;
	}

	public List<Hotel> getLastHotels() {
		return lastHotels;
	}

	public void setLastHotels(List<Hotel> lastHotels) {
		this.lastHotels = lastHotels;
	}

	public List<Hotel> getFilteredHotels() {
		return filteredHotels;
	}

	public void setFilteredHotels(List<Hotel> filteredHotels) {
		this.filteredHotels = filteredHotels;
	}

	public Integer getSelectedHotelIndex() {
		return selectedHotelIndex;
	}

	public void setSelectedHotelIndex(Integer selectedHotelIndex) {
		this.selectedHotelIndex = selectedHotelIndex;
	}

	public Hotel getChosenHotel() {
		return chosenHotel;
	}

	public void setChosenHotel(Hotel chosenHotel) {
		this.chosenHotel = chosenHotel;
	}

	public HotelRoom getChosenRoom() {
		return chosenRoom;
	}

	public void setChosenRoom(HotelRoom chosenRoom) {
		this.chosenRoom = chosenRoom;
	}

	public Boolean getConfirmBook() {
		return confirmBook;
	}

	public void setConfirmBook(Boolean confirmBook) {
		this.confirmBook = confirmBook;
	}

	public Boolean getConfirmAnother() {
		return confirmAnother;
	}

	public void setConfirmAnother(Boolean confirmAnother) {
		this.confirmAnother = confirmAnother;
	}

	public List<String> getChosenServices() {
		return chosenServices;
	}

	public void setChosenServices(List<String> chosenServices) {
		this.chosenServices = chosenServices;
	}

	public Boolean getConfirmAddAnother() {
		return confirmAddAnother;
	}

	public void setConfirmAddAnother(Boolean confirmAddAnother) {
		this.confirmAddAnother = confirmAddAnother;
	}

	public List<Integer> getBookingIds() {
		return bookingIds;
	}

	public void setBookingIds(List<Integer> bookingIds) {
		this.bookingIds = bookingIds;
	}

	public Boolean getPaymentDone() {
		return paymentDone;
	}

	public void setPaymentDone(Boolean paymentDone) {
		this.paymentDone = paymentDone;
	}

	public Integer getFeedbackRating() {
		return feedbackRating;
	}

	public void setFeedbackRating(Integer feedbackRating) {
		this.feedbackRating = feedbackRating;
	}

	public String getFeedbackComments() {
		return feedbackComments;
	}

	public void setFeedbackComments(String feedbackComments) {
		this.feedbackComments = feedbackComments;
	}

	public Boolean getConfirmRefine() {
		return confirmRefine;
	}

	public void setConfirmRefine(Boolean confirmRefine) {
		this.confirmRefine = confirmRefine;
	}

	public double getRoomPrice() {
		return RoomPrice;
	}

	public void setRoomPrice(double roomPrice) {
		RoomPrice = roomPrice;
	}

	public double getSubtotal() {
		return Subtotal;
	}

	public void setSubtotal(double subtotal) {
		Subtotal = subtotal;
	}
	// in ConversationState.java, after all your getters/setters:

	/** Completely clear out this state and go back to the very beginning. */
	public void reset() {
		this.stage = Stage.START;
		this.city = null;
		this.state = null;
		this.checkIn = null;
		this.checkOut = null;
		this.guests = null;
		clearFilters(); // your helper to clear minStars, minPrice, maxPrice, requiredAmenities
		this.roomType = null;
		this.noRooms = null;
		this.customerName = null;
		this.hotelName = null;
		this.hotelId = null;
		this.Subtotal = 0.0;
		this.RoomPrice = 0.0;
		this.lastHotels.clear();
		this.filteredHotels.clear();
		this.selectedHotelIndex = null;
		this.chosenHotel = null;
		this.lastServiceOptions.clear();
		this.chosenServiceOptions.clear();
		this.lastRoomTypes.clear();
		this.chosenRoom = null;
		this.hotelRoomId = null;
		this.confirmBook = null;
		this.confirmAnother = null;
		this.confirmAddAnother = null;
		this.confirmRefine = null;
		this.paymentDone = null;
		this.feedbackRating = null;
		this.feedbackComments = null;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public Stage getPreviousStage() {
		return previousStage;
	}

	public void setPreviousStage(Stage previousStage) {
		this.previousStage = previousStage;
	}

	public String getAgentRequestName() {
		return agentRequestName;
	}

	public void setAgentRequestName(String agentRequestName) {
		this.agentRequestName = agentRequestName;
	}

	public String getAgentRequestPhone() {
		return agentRequestPhone;
	}

	public void setAgentRequestPhone(String agentRequestPhone) {
		this.agentRequestPhone = agentRequestPhone;
	}

}
