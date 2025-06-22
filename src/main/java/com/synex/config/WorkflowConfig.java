package com.synex.config;

import com.synex.domain.*;
import org.springframework.context.annotation.*;
import java.util.*;

@Configuration
public class WorkflowConfig {
	@Bean
	public Map<ConversationState.Stage, StateConfig> workflow() {

		Map<ConversationState.Stage, StateConfig> m = new EnumMap<>(ConversationState.Stage.class);

		m.put(ConversationState.Stage.START, new StateConfig(List.of(), null,
				ctx -> ConversationState.Stage.ASK_LOCATION, ctx -> Collections.emptyList()));

		m.put(ConversationState.Stage.ASK_LOCATION, new StateConfig(List.of("city", "state"), null,
				ctx -> ConversationState.Stage.ASK_DATES, ctx -> Collections.emptyList()));

		m.put(ConversationState.Stage.ASK_DATES, new StateConfig(List.of("checkIn", "checkOut"), null,
				ctx -> ConversationState.Stage.ASK_GUESTS, ctx -> Collections.emptyList()));

		m.put(ConversationState.Stage.ASK_GUESTS, new StateConfig(List.of("guests"), null,
				ctx -> ConversationState.Stage.ASK_FILTERS, ctx -> Collections.emptyList()));

		m.put(ConversationState.Stage.ASK_FILTERS,
				new StateConfig(List.of("minStars", "minPrice", "maxPrice", "requiredAmenities"), null,
						ctx -> ConversationState.Stage.SHOW_HOTELS, ctx -> Collections.emptyList()));

		m.put(ConversationState.Stage.SHOW_HOTELS,
				new StateConfig(List.of("hotelId", "hotelName"), null,
						ctx -> ctx.getHotelId() != null ? ConversationState.Stage.ASK_SERVICES
								: ConversationState.Stage.SHOW_HOTELS,
						ctx -> Collections.emptyList()));

		m.put(ConversationState.Stage.ASK_SERVICES,
				new StateConfig(List.of("chosenServices"), null, ctx -> ConversationState.Stage.ASK_ROOM_TYPE,
						ctx -> Optional.ofNullable(ctx.getLastServiceOptions()).orElse(List.of()).stream()
								.map(ServiceOption::getName).toList()));

		m.put(ConversationState.Stage.ASK_ROOM_TYPE,
				new StateConfig(List.of("roomType"), null, ctx -> ConversationState.Stage.ASK_NUM_ROOMS,
						ctx -> Optional.ofNullable(ctx.getLastRoomTypes()).orElse(List.of()).stream()
								.map(RoomType::getName).toList()));

		m.put(ConversationState.Stage.ASK_NUM_ROOMS, new StateConfig(List.of("noRooms"), null,
				ctx -> ConversationState.Stage.ASK_CUSTOMER_NAME, ctx -> Collections.emptyList()));

		m.put(ConversationState.Stage.ASK_CUSTOMER_NAME, new StateConfig(List.of("customerName"), null,
				ctx -> ConversationState.Stage.SHOW_SUBTOTAL, ctx -> Collections.emptyList()));

		m.put(ConversationState.Stage.SHOW_SUBTOTAL, new StateConfig(List.of("confirmBooking"), null,
				ctx -> ConversationState.Stage.ASK_ANOTHER, ctx -> Collections.emptyList()));

		m.put(ConversationState.Stage.ASK_ANOTHER, new StateConfig(List.of("confirmAnother"), null,
				ctx -> ctx.getConfirmAnother() ? ConversationState.Stage.START : ConversationState.Stage.ASK_PAYMENT,
				ctx -> Collections.emptyList()));

		m.put(ConversationState.Stage.ASK_PAYMENT, new StateConfig(List.of("confirmPayment"), null,
				ctx -> ConversationState.Stage.ASK_FEEDBACK, ctx -> Collections.emptyList()));

		m.put(ConversationState.Stage.ASK_FEEDBACK, new StateConfig(List.of("feedbackRating", "feedbackComments"), null,
				ctx -> ConversationState.Stage.DONE, ctx -> Collections.emptyList()));

		return m;
	}

}
