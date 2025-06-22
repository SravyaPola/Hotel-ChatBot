package com.synex.repository;

import com.synex.domain.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Integer> {
	List<Hotel> findByHotelNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrStateContainingIgnoreCase(
			String hotelName, String city, String state);

	Hotel findByHotelNameIgnoreCase(String hotelName);

	List<Hotel> findByCityIgnoreCase(String city);

	List<Hotel> findByCityContainingIgnoreCase(String cityPart);

	@Query("SELECT DISTINCT h.city FROM Hotel h ORDER BY h.city")
	List<String> findDistinctCities();

	@Query("SELECT DISTINCT h.state FROM Hotel h ORDER BY h.state")
	List<String> findDistinctStates();

	@Query("SELECT DISTINCT h.starRating FROM Hotel h ORDER BY h.starRating")
	List<Integer> findDistinctStarRatings();

	@Query("SELECT MIN(h.averagePrice) FROM Hotel h")
	Double findMinPrice();

	@Query("SELECT MAX(h.averagePrice) FROM Hotel h")
	Double findMaxPrice();

	List<Hotel> findByStateIgnoreCase(String state);

	@Query("SELECT a.name FROM Hotel h JOIN h.amenities a WHERE h.hotelId = :hotelId")
	List<String> findAmenityNamesByHotelId(Integer hotelId);
	
	List<Hotel> findByCityIgnoreCaseOrStateIgnoreCase(String city, String state);

	List<Hotel> findByCityAndState(String city, String state);
}