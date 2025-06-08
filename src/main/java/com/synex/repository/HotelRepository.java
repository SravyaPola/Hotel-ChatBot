package com.synex.repository;

import com.synex.domain.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Integer> {
	List<Hotel> findByHotelNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrStateContainingIgnoreCase(
			String hotelName, String city, String state);
}
