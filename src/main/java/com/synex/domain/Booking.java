package com.synex.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "bookings")
public class Booking {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "booking_id")
	private Integer bookingId;

	@Column(name = "hotel_id", nullable = false)
	private Integer hotelId;

	@Column(name = "hotel_room_id", nullable = false)
	private Integer hotelRoomId;

	@Column(name = "no_rooms", nullable = false)
	private Integer noRooms;

	@Column(name = "no_guests", nullable = false)
	private Integer noGuests;

	@Column(name = "checkin_date", nullable = false)
	private LocalDate checkInDate;

	@Column(name = "checkout_date", nullable = false)
	private LocalDate checkOutDate;

	@Column(name = "customer_name", nullable = false)
	private String customerName;

	public Integer getBookingId() {
		return bookingId;
	}

	public void setBookingId(Integer bookingId) {
		this.bookingId = bookingId;
	}

	public Integer getHotelId() {
		return hotelId;
	}

	public void setHotelId(Integer hotelId) {
		this.hotelId = hotelId;
	}

	public Integer getHotelRoomId() {
		return hotelRoomId;
	}

	public void setHotelRoomId(Integer hotelRoomId) {
		this.hotelRoomId = hotelRoomId;
	}

	public Integer getNoRooms() {
		return noRooms;
	}

	public void setNoRooms(Integer noRooms) {
		this.noRooms = noRooms;
	}

	public Integer getNoGuests() {
		return noGuests;
	}

	public void setNoGuests(Integer noGuests) {
		this.noGuests = noGuests;
	}

	public LocalDate getCheckInDate() {
		return checkInDate;
	}

	public void setCheckInDate(LocalDate checkInDate) {
		this.checkInDate = checkInDate;
	}

	public LocalDate getCheckOutDate() {
		return checkOutDate;
	}

	public void setCheckOutDate(LocalDate checkOutDate) {
		this.checkOutDate = checkOutDate;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}
}
