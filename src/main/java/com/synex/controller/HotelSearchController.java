package com.synex.controller;

import com.synex.domain.Booking;
import com.synex.domain.Hotel;
import com.synex.domain.HotelRoom;
import com.synex.domain.SearchHotelRoomDetails;
import com.synex.repository.BookingRepository;
import com.synex.repository.HotelRepository;
import com.synex.repository.HotelRoomRepository;
import com.synex.repository.RoomTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
public class HotelSearchController {

	private final HotelRepository hotelRepository;
	private final HotelRoomRepository hotelRoomRepository;
	private final RoomTypeRepository roomTypeRepository;
	private final BookingRepository bookingRepository;

	public HotelSearchController(HotelRepository hotelRepository, HotelRoomRepository hotelRoomRepository,
			RoomTypeRepository roomTypeRepository, BookingRepository bookingRepository) {
		this.hotelRepository = hotelRepository;
		this.hotelRoomRepository = hotelRoomRepository;
		this.roomTypeRepository = roomTypeRepository;
		this.bookingRepository = bookingRepository;
	}

	@GetMapping("/search-hotels")
	public ResponseEntity<List<Hotel>> searchHotels(@RequestParam("searchHotel") String searchTerm,
			@RequestParam("checkIn") String checkIn, @RequestParam("checkOut") String checkOut,
			@RequestParam("noOfRooms") int noOfRooms, @RequestParam("noOfGuests") int noOfGuests) {
		List<Hotel> hotels = hotelRepository
				.findByHotelNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrStateContainingIgnoreCase(searchTerm,
						searchTerm, searchTerm);
		return ResponseEntity.ok(hotels);
	}

	@PostMapping("/search-hotel-rooms")
	public ResponseEntity<List<HotelRoom>> searchHotelRooms(@RequestBody SearchHotelRoomDetails details) {
		List<HotelRoom> rooms = hotelRoomRepository.findByHotel_HotelId(details.getHotelId());
		return ResponseEntity.ok(rooms);
	}

	@GetMapping("/room-types")
	public ResponseEntity<?> getAllRoomTypes() {
		return ResponseEntity.ok(roomTypeRepository.findAll());
	}

	@PostMapping("/booking")
	public ResponseEntity<?> bookRoom(@RequestBody Booking booking) {
		HotelRoom room = hotelRoomRepository.findById(booking.getHotelRoomId())
				.orElseThrow(() -> new IllegalArgumentException("Unknown room ID"));
		int alreadyBooked = bookingRepository.countBookedRooms(booking.getHotelRoomId(), booking.getCheckInDate(),
				booking.getCheckOutDate());

		int available = room.getNoRooms() - alreadyBooked;
		if (available < booking.getNoRooms()) {
			String msg = available == 0
					? "This Room Types are fully booked for the selected dates. Please try other room types."
					: "Only " + available + " room(s) available for those dates.";
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap("error", msg));
		}
		Booking saved = bookingRepository.save(booking);
		return ResponseEntity.ok(Collections.singletonMap("confirmationId", saved.getBookingId()));
	}

	@GetMapping("/hotels/{id}")
	public ResponseEntity<Hotel> getHotelById(@PathVariable("id") Integer id) {
		return hotelRepository.findById(id).map(h -> ResponseEntity.ok(h))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping("/hotels")
	public ResponseEntity<List<Hotel>> getAllHotels() {
		return ResponseEntity.ok(hotelRepository.findAll());
	}
}
