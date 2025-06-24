package com.synex.repository;

import com.synex.domain.HotelRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface HotelRoomRepository extends JpaRepository<HotelRoom, Integer> {

	// Find all hotel rooms belonging to a specific hotel
	List<HotelRoom> findByHotel_HotelId(Integer hotelId);

	// Optional: find one HotelRoom by hotel id and room type id (custom query)
	Optional<HotelRoom> findByHotel_HotelIdAndType_TypeId(Integer hotelId, Integer roomTypeId);
}