// src/main/java/com/synex/service/impl/HotelServiceImpl.java
package com.synex.service;

import com.synex.domain.Amenity;
import com.synex.domain.Hotel;
import com.synex.domain.HotelRoom;
import com.synex.domain.ServiceOption;
import com.synex.domain.RoomType;
import com.synex.repository.AmenityRepository;
import com.synex.repository.HotelRepository;
import com.synex.repository.HotelRoomRepository;
import com.synex.repository.RoomTypeRepository;
import com.synex.repository.ServiceOptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HotelServiceImpl implements HotelService {

	@Autowired
	private HotelRepository hotelRepo;
	@Autowired
	private AmenityRepository amenityRepo;
	@Autowired
	private ServiceOptionRepository serviceOptionRepo;
	@Autowired
	private RoomTypeRepository roomTypeRepo;

	@Autowired
	private HotelRoomRepository roomRepo;

	// ─── suggestion endpoints ───

	@Override
	public List<String> getAllCities() {
		return hotelRepo.findDistinctCities();
	}

	@Override
	public List<String> getAllStates() {
		return hotelRepo.findDistinctStates();
	}

	@Override
	public List<Integer> getAllStarRatings() {
		return hotelRepo.findDistinctStarRatings();
	}

	@Override
	public List<String> getAllAmenityNames() {
		return amenityRepo.findDistinctNames();
	}

	@Override
	public double getMinPrice() {
		Double v = hotelRepo.findMinPrice();
		return v == null ? 0.0 : v;
	}

	@Override
	public double getMaxPrice() {
		Double v = hotelRepo.findMaxPrice();
		return v == null ? 0.0 : v;
	}

	// ─── search & lookup ───

	@Override
	public List<Hotel> searchByNameOrLocation(String q) {
		return hotelRepo.findByHotelNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrStateContainingIgnoreCase(q, q,
				q);
	}

	@Override
	public List<Hotel> findByCity(String city) {
		return hotelRepo.findByCityIgnoreCase(city);
	}

	@Override
	public List<Hotel> findByState(String state) {
		return hotelRepo.findByStateIgnoreCase(state);
	}

	@Override
	public Hotel findByName(String name) {
		return hotelRepo.findByHotelNameIgnoreCase(name);
	}

	// ─── filtering ───

	@Override
	public List<Hotel> filter(List<Hotel> hotels, Integer minStars, Double minPrice, Double maxPrice,
			Set<String> requiredAmenities) {
		return hotels.stream()
				// star‐rating filter
				.filter(h -> minStars == null || h.getStarRating() >= minStars)
				// min‐price filter
				.filter(h -> minPrice == null || h.getAveragePrice() >= minPrice)
				// max‐price filter
				.filter(h -> maxPrice == null || h.getAveragePrice() <= maxPrice)
				// amenity filter via JPA many‐to‐many
				.filter(h -> {
					// capture hotel amenities as lowercase names
					Set<String> has = h.getAmenities().stream().map(Amenity::getName).map(String::toLowerCase)
							.collect(Collectors.toSet());

					// requested amenities lowercased
					Set<String> want = requiredAmenities == null
						    ? Collections.emptySet()
						    : requiredAmenities.stream().map(String::toLowerCase).collect(Collectors.toSet());

					return has.containsAll(want);
				}).collect(Collectors.toList());
	}

	// ─── per‐hotel details ───
	@Override
	public List<String> getAmenityNamesForHotel(Integer hotelId) {
		return hotelRepo.findAmenityNamesByHotelId(hotelId);
	}

	@Override
	public List<ServiceOption> getServiceOptions(Integer hotelId) {
		return serviceOptionRepo.findByHotelId(hotelId);
	}

	@Override
	public double computeServiceCost(List<ServiceOption> opts, int guests, long nights) {
		return opts.stream().mapToDouble(o -> o.getPrice() * nights * (o.getPerPerson() ? guests : 1)).sum();
	}

	@Override
	public List<RoomType> getAllRoomTypes() {
		return roomTypeRepo.findAll();
	}

	@Override
	public List<Hotel> findAll() {
		return hotelRepo.findAll();
	}

	@Override
	public List<HotelRoom> getRoomsForHotel(Integer hotelId) {
		return roomRepo.findByHotel_HotelId(hotelId);
	}

	/** For listing hotels, show the cheapest room’s rate */
	public double getStartingPrice(Integer hotelId) {
		return getRoomsForHotel(hotelId).stream().mapToDouble(HotelRoom::getPrice).min().orElse(0.0);
	}

	@Override
	public List<Hotel> findAllInCityOrState(String city, String state) {
		if (city != null && !city.isBlank() && state != null && !state.isBlank()) {
			return hotelRepo.findByCityIgnoreCaseOrStateIgnoreCase(city, state);
		} else if (city != null && !city.isBlank()) {
			return hotelRepo.findByCityIgnoreCase(city);
		} else if (state != null && !state.isBlank()) {
			return hotelRepo.findByStateIgnoreCase(state);
		}
		return hotelRepo.findAll();
	}

	/**
	 * Lookup by ID, returning null if not found. (You can also throw an exception
	 * if you prefer.)
	 */
	@Override
	public Hotel findById(int id) {
		return hotelRepo.findById(id).orElse(null);
	}

	@Override
	public List<Hotel> findByCityAndState(String city, String state) {
		return hotelRepo.findByCityAndState(city, state);
	}

	@Override
	public List<Hotel> getAllHotels() {
	    return hotelRepo.findAll();
	}
}