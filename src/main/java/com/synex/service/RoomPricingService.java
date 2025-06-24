package com.synex.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.synex.domain.ConversationState;
import com.synex.domain.HotelRoom;
import com.synex.repository.HotelRoomRepository;

@Service
public class RoomPricingService {

	@Autowired
	private HotelRoomRepository hotelRoomRepository;

	public double getRoomPriceForSelection(ConversationState st) {
		Integer roomId = st.getHotelRoomId();
		if (roomId == null) {
			// No room selected; return 0 or handle as needed
			return 0.0;
		}
		return hotelRoomRepository.findById(roomId).map(HotelRoom::getPrice).map(Float::doubleValue).orElse(0.0);
	}
}
