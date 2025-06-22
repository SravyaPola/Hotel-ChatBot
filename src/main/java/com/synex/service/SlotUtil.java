package com.synex.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.synex.domain.ConversationState;

public class SlotUtil {
	

	
	public static void applySlots(ConversationState st, Map<String,String> slots, HotelService hotelService) {
        List<String> dbAmenities = hotelService.getAllAmenityNames();
        slots.forEach((k, v) -> {
            switch (k) {
                case "city"               -> st.setCity(v);
                case "state"              -> st.setState(v);
                case "checkIn" -> {
                    try { st.setCheckIn(LocalDate.parse(v)); } catch (Exception ignored) {}
                }
                case "checkOut" -> {
                    try { st.setCheckOut(LocalDate.parse(v)); } catch (Exception ignored) {}
                }
                case "guests" -> {
                    try { st.setGuests(Integer.valueOf(v.trim())); } catch (Exception ignored) {}
                }
                case "minStars" -> {
                    try {
                        int stars = Integer.parseInt(v.trim());
                        st.setMinStars((stars >= 1 && stars <= 5) ? stars : null);
                    } catch (Exception ignored) { st.setMinStars(null); }
                }
                case "minPrice" -> {
                    try {
                        double price = Double.parseDouble(v.trim());
                        st.setMinPrice(price >= 0 ? price : null);
                    } catch (Exception ignored) { st.setMinPrice(null); }
                }
                case "maxPrice" -> {
                    try {
                        double price = Double.parseDouble(v.trim());
                        st.setMaxPrice((price >= 1 && price <= 500) ? price : null);
                    } catch (Exception ignored) { st.setMaxPrice(null); }
                }
                case "requiredAmenities" -> {
                    Set<String> matched = AmenityMatcher.matchUserAmenitiesToDb(v, dbAmenities);
                    st.setRequiredAmenities(matched);
                }
                case "hotelId"            -> { try { st.setHotelId(Integer.valueOf(v)); } catch (Exception ignored) {} }
                case "chosenServices"     -> st.setChosenServices(List.of(v.split(",")));
                case "roomType"           -> st.setRoomType(v);
                case "noRooms"            -> { try { st.setNoRooms(Integer.valueOf(v)); } catch (Exception ignored) {} }
                case "customerName"       -> st.setCustomerName(v);
                case "confirmBooking"     -> { try { st.setConfirmBook(Boolean.valueOf(v)); } catch (Exception ignored) {} }
                case "confirmAnother"     -> { try { st.setConfirmAnother(Boolean.valueOf(v)); } catch (Exception ignored) {} }
                case "confirmAddAnother"  -> { try { st.setConfirmAddAnother(Boolean.valueOf(v)); } catch (Exception ignored) {} }
                case "confirmPayment"     -> { try { st.setPaymentDone(Boolean.valueOf(v)); } catch (Exception ignored) {} }
                case "feedbackRating"     -> { try { st.setFeedbackRating(Integer.valueOf(v)); } catch (Exception ignored) {} }
                case "feedbackComments"   -> st.setFeedbackComments(v);
            }
        });
    }

}
