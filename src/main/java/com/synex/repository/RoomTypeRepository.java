package com.synex.repository;

import com.synex.domain.RoomType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Integer> {

	@Query("""
			   select distinct hr.type
			     from HotelRoom hr
			    where hr.hotel.hotelId = :hotelId
			""")
	List<RoomType> findAllByHotelId(@Param("hotelId") int hotelId);

	/**
	 * Lookup by name for the “pick one exactly as shown” step.
	 */
	RoomType findByNameIgnoreCase(String name);

}
