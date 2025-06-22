// src/main/java/com/synex/domain/ParseResult.java
package com.synex.domain;

import java.time.LocalDate;
import java.util.*;

public class ParseResult {
	public enum Intent {
		HOTEL_GIVEN, CITY_GIVEN, DATES_GIVEN, GUESTS_GIVEN, FILTERS_GIVEN, HOTEL_INDEX, SERVICES_GIVEN, SERVICES_NONE,
		ROOM_TYPE_GIVEN, YES, NO, PAY, LIST_HOTELS, FEEDBACK, UNKNOWN, NUM_ROOMS_GIVEN, CUSTOMER_NAME_GIVEN,

	}

	private Intent intent = Intent.UNKNOWN;

	// ─── slots from user ───
	private String hotelName;
	private String city;
	private String state;
	private LocalDate checkIn;
	private LocalDate checkOut;
	private Integer guests;
	private Integer minStars;
	private Double minPrice;
	private Double maxPrice;
	private List<String> amenities = new ArrayList<>();
	private Integer hotelIndex;
	private List<Integer> serviceIndices = new ArrayList<>();
	private Boolean confirm;
	private Integer rating;
	private String comments;

	// ─── NEW SLOTS ───
	private Integer serviceQuantityDays;
	private String roomType;

	// ─── raw text for relative dates if needed ───
	private String rawText;

	public ParseResult() {
	}

	// ─── getters & setters for existing slots ───
	// ... (keep all your originals) ...

	// ─── new getters & setters ───

	public Integer getServiceQuantityDays() {
		return serviceQuantityDays;
	}

	public Intent getIntent() {
		return intent;
	}

	public void setIntent(Intent intent) {
		this.intent = intent;
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

	public List<String> getAmenities() {
		return amenities;
	}

	public void setAmenities(List<String> amenities) {
		this.amenities = amenities;
	}

	public Integer getHotelIndex() {
		return hotelIndex;
	}

	public void setHotelIndex(Integer hotelIndex) {
		this.hotelIndex = hotelIndex;
	}

	public List<Integer> getServiceIndices() {
		return serviceIndices;
	}

	public void setServiceIndices(List<Integer> serviceIndices) {
		this.serviceIndices = serviceIndices;
	}

	public Boolean getConfirm() {
		return confirm;
	}

	public void setConfirm(Boolean confirm) {
		this.confirm = confirm;
	}

	public Integer getRating() {
		return rating;
	}

	public void setRating(Integer rating) {
		this.rating = rating;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public void setServiceQuantityDays(Integer serviceQuantityDays) {
		this.serviceQuantityDays = serviceQuantityDays;
	}

	public String getRoomType() {
		return roomType;
	}

	public void setRoomType(String roomType) {
		this.roomType = roomType;
	}

	public String getRawText() {
		return rawText;
	}

	public void setRawText(String rawText) {
		this.rawText = rawText;
	}
}
