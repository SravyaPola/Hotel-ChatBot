package com.synex.service;

import com.synex.domain.Booking;
import com.synex.domain.HotelRoom;
import com.synex.repository.BookingRepository;
import com.synex.repository.HotelRoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final HotelRoomRepository hotelRoomRepo;
    private final BookingRepository   bookingRepo;

    public BookingService(
        HotelRoomRepository hotelRoomRepo,
        BookingRepository   bookingRepo
    ) {
        this.hotelRoomRepo = hotelRoomRepo;
        this.bookingRepo   = bookingRepo;
    }

    /**
     * Return all room‐types for a given hotel that have at least
     * `roomsRequested` free units in the period [checkIn, checkOut).
     */
    public List<RoomOption> findAvailableRooms(
        Integer hotelId,
        LocalDate checkIn,
        LocalDate checkOut,
        int roomsRequested
    ) {
        // 1) load all room types for this hotel
        List<HotelRoom> rooms = hotelRoomRepo.findByHotel_HotelId(hotelId);

        // 2) filter down to only those with enough free units
        return rooms.stream()
            .filter(r -> {
                // how many already booked?
                int alreadyBooked = bookingRepo.countBookedRooms(
                    r.getHotelRoomId(), checkIn, checkOut
                );
                // total units of this room type
                int totalUnits = r.getNoRooms();
                // enough remaining?
                return (totalUnits - alreadyBooked) >= roomsRequested;
            })
            // 3) map to our DTO
            .map(r -> new RoomOption(
                r.getHotelRoomId(),
                r.getType().getName(),
                r.getPrice(),
                r.getNoRooms()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Persist a new booking record.
     */
    public Booking confirmBooking(
        Integer hotelId,
        Integer hotelRoomId,
        int noRooms,
        int noGuests,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        String customerName
    ) {
        Booking booking = new Booking();
        booking.setHotelId(hotelId);
        booking.setHotelRoomId(hotelRoomId);
        booking.setNoRooms(noRooms);
        booking.setNoGuests(noGuests);
        booking.setCheckInDate(checkInDate);
        booking.setCheckOutDate(checkOutDate);
        booking.setCustomerName(customerName);
        return bookingRepo.save(booking);
    }

    /** 
     * (Optional) If you ever just need to know whether *any* overlap exists,
     * you can also call this:
     *
     * bookingRepo.existsByHotelIdAndHotelRoomIdAndCheckInDateLessThanAndCheckOutDateGreaterThan(
     *     hotelId, hotelRoomId, desiredCheckOut, desiredCheckIn);
     */

    /** DTO for chat/display purposes */
    public static class RoomOption {
        private final Integer roomId;
        private final String  typeName;
        private final double  price;
        private final int     totalUnits;

        public RoomOption(Integer roomId, String typeName, double price, int totalUnits) {
            this.roomId     = roomId;
            this.typeName   = typeName;
            this.price      = price;
            this.totalUnits = totalUnits;
        }
        public Integer getRoomId()     { return roomId;     }
        public String  getTypeName()   { return typeName;   }
        public double  getPrice()      { return price;      }
        public int     getTotalUnits() { return totalUnits; }
    }
}
