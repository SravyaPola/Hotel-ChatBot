package com.synex.service;

import com.synex.domain.Booking;
import com.synex.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class BookingService {

    @Autowired private BookingRepository bookingRepo;

    public Booking create(Integer hotelId, Integer hotelRoomId, Integer noRooms, Integer noGuests,
            LocalDate checkInDate, LocalDate checkOutDate, String customerName, String serviceList,
            double subtotal) {  // pass subtotal as price including room + services

Booking booking = new Booking();
booking.setHotelId(hotelId);
booking.setHotelRoomId(hotelRoomId);
booking.setNoRooms(noRooms);
booking.setNoGuests(noGuests);
booking.setCheckInDate(checkInDate);
booking.setCheckOutDate(checkOutDate);
booking.setCustomerName(customerName);
booking.setServiceList(serviceList);
booking.setPaymentDone(false);

booking.setTotalPrice(subtotal);  // set the total price (room + services)

// save booking to DB (assuming you have a BookingRepository)
return bookingRepo.save(booking);
}

    public void assignCustomerName(List<Integer> bookingIds, String customerName) {
        var list = bookingRepo.findByBookingIdIn(bookingIds);
        list.forEach(b -> b.setCustomerName(customerName));
        bookingRepo.saveAll(list);
    }
    public double computeTotal(List<Integer> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) {
            return 0.0;
        }
        List<Booking> bookings = bookingRepo.findAllById(bookingIds);
        return bookings.stream()
                .map(Booking::getTotalPrice)
                .filter(price -> price != null)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    public void chargeAll(List<Integer> bookingIds) {
        // 1) Load all the bookings by ID
        List<Booking> bookings = bookingRepo.findAllById(bookingIds);

        // 2) Mark each one as paid
        for (Booking b : bookings) {
            b.setPaymentDone(true);
        }

        // 3) Save them all in one batch
        bookingRepo.saveAll(bookings);
    }
}
