package com.synex.repository;

import com.synex.domain.HotelRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HotelRoomRepository extends JpaRepository<HotelRoom, Integer> {
	
	List<HotelRoom> findByHotel_HotelId(Integer hotelId);

}
