package com.synex.repository;

import com.synex.domain.Booking;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

	boolean existsByHotelIdAndHotelRoomIdAndCheckInDateLessThanAndCheckOutDateGreaterThan(Integer hotelId,
			Integer hotelRoomId, LocalDate checkOutDate, LocalDate checkInDate);

	@Query("""
			  SELECT COALESCE(SUM(b.noRooms),0)
			    FROM Booking b
			   WHERE b.hotelRoomId   = :roomId
			     AND b.checkInDate   <  :checkOut
			     AND b.checkOutDate  >  :checkIn
			""")
	int countBookedRooms(@Param("roomId") Integer hotelRoomId, @Param("checkIn") LocalDate checkInDate,
			@Param("checkOut") LocalDate checkOutDate);
}
