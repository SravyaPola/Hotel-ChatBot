package com.synex.service;

import com.synex.domain.HotelRoom;
import com.synex.repository.HotelRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HotelRoomService {

    @Autowired
    private HotelRoomRepository hotelRoomRepository;

    /**
     * Fetch all rooms for a given hotel id.
     */
    public List<HotelRoom> getRoomsByHotelId(Integer hotelId) {
        return hotelRoomRepository.findByHotel_HotelId(hotelId);
    }

    /**
     * Find a specific room for given hotel and roomType.
     * Returns Optional.empty() if none found.
     */
    public Optional<HotelRoom> findHotelRoomByHotelAndType(Integer hotelId, Integer roomTypeId) {
        return hotelRoomRepository.findByHotel_HotelIdAndType_TypeId(hotelId, roomTypeId);
    }
}