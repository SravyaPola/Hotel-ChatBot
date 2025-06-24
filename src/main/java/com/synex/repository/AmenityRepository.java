package com.synex.repository;

import com.synex.domain.Amenity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Integer> {

	@Query("SELECT DISTINCT a.name FROM Amenity a ORDER BY a.name")
	List<String> findDistinctNames();

	@Query(value = """
			   SELECT a.*
			     FROM amenities a
			     JOIN hotel_rooms_amenities hra ON a.a_id = hra.a_id
			     JOIN hotel_rooms hr            ON hra.hotel_room_id = hr.hotel_room_id
			    WHERE hr.hotel_id = :hotelId
			""", nativeQuery = true)
	List<Amenity> findByHotelId(@Param("hotelId") Integer hotelId);
}
