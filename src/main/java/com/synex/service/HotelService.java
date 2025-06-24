package com.synex.service;

import com.synex.domain.Hotel;
import com.synex.domain.HotelRoom;
import com.synex.domain.ServiceOption;
import com.synex.domain.RoomType;
import java.util.List;
import java.util.Set;

public interface HotelService {

	// ─── suggestion endpoints ───
	List<String> getAllCities();

	List<String> getAllStates();

	List<Integer> getAllStarRatings();

	List<String> getAllAmenityNames();

	double getMinPrice();

	double getMaxPrice();

	// ─── search & lookup ───
	List<Hotel> searchByNameOrLocation(String query);

	List<Hotel> findByCity(String city);

	List<Hotel> findByState(String state);

	Hotel findByName(String name);

	// ─── filtering ───
	List<Hotel> filter(List<Hotel> hotels, Integer minStars, Double minPrice, Double maxPrice,
			Set<String> requiredAmenities);

	// ─── per‐hotel details ───
	List<String> getAmenityNamesForHotel(Integer hotelId);

	List<ServiceOption> getServiceOptions(Integer hotelId);

	double computeServiceCost(List<ServiceOption> opts, int guests, long nights);

	List<RoomType> getAllRoomTypes();

	List<Hotel> findAll();

	List<HotelRoom> getRoomsForHotel(Integer hotelId);

	List<Hotel> findAllInCityOrState(String city, String state);

	Hotel findById(int id);

	List<Hotel> findByCityAndState(String city, String state);

	List<Hotel> getAllHotels();

}
