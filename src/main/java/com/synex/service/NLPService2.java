package com.synex.service;

import com.synex.domain.ParseResult;
import com.synex.domain.ParseResult.Intent;
import com.synex.domain.ConversationState.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

@Service
public class NLPService2 {
    private static final DateTimeFormatter ISO =
        DateTimeFormatter.ofPattern("yyyy[-][MM][-][dd]");
    private static final DateTimeFormatter LONG =
        DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private static final Pattern DATE_ISO =
        Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s*(?:to|–|-)\\s*(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern DATE_LONG =
        Pattern.compile("([A-Za-z]+ \\d{1,2}, \\d{4})\\s*(?:to|–|-)\\s*([A-Za-z]+ \\d{1,2}, \\d{4})");
    private static final Pattern GUESTS =
        Pattern.compile("(\\d+)\\s*(?:adults?|guests?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STARS =
        Pattern.compile("(\\d)\\s*(?:star|★)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE =
        Pattern.compile("\\$(\\d+)(?:-(\\d+))?/?night", Pattern.CASE_INSENSITIVE);
    private static final Pattern HOTEL_NAME =
        Pattern.compile("hotel\\s+([A-Za-z0-9 ]+)", Pattern.CASE_INSENSITIVE);

    @Autowired
    private HotelService hotelService; // Use this to fetch amenities from DB

    public ParseResult parse(String text, String currentStageName) {
        String lower = text.toLowerCase();
        ParseResult pr = new ParseResult();
        pr.setRawText(text);

        // CITY / HOTEL-FALLBACK
        if (Stage.ASK_CITY.name().equals(currentStageName)) {
          pr.setCity(text.trim());
          pr.setIntent(Intent.CITY_GIVEN);
        }

        // DATES
        Matcher m1 = DATE_ISO.matcher(text);
        if (m1.find()) {
          pr.setCheckIn(LocalDate.parse(m1.group(1), ISO));
          pr.setCheckOut(LocalDate.parse(m1.group(2), ISO));
          pr.setIntent(Intent.DATES_GIVEN);
        } else {
          Matcher m2 = DATE_LONG.matcher(text);
          if (m2.find()) {
            pr.setCheckIn(LocalDate.parse(m2.group(1), LONG));
            pr.setCheckOut(LocalDate.parse(m2.group(2), LONG));
            pr.setIntent(Intent.DATES_GIVEN);
          }
        }

        // GUESTS
        Matcher mg = GUESTS.matcher(text);
        if (mg.find()) {
          pr.setGuests(Integer.valueOf(mg.group(1)));
          pr.setIntent(Intent.GUESTS_GIVEN);
        }

        // FILTERS (including dynamic amenities from DB)
        Matcher ms = STARS.matcher(text);
        if (ms.find()) {
          pr.setMinStars(Integer.valueOf(ms.group(1)));
          pr.setIntent(Intent.FILTERS_GIVEN);
        }
        Matcher mp = PRICE.matcher(text);
        if (mp.find()) {
          pr.setMinPrice(Double.valueOf(mp.group(1)));
          if (mp.group(2) != null) pr.setMaxPrice(Double.valueOf(mp.group(2)));
          pr.setIntent(Intent.FILTERS_GIVEN);
        }

        // ── DYNAMIC AMENITIES (fetch from DB, not static) ──
        List<String> dynamicAmenities = hotelService.getAllAmenityNames();
        var found = new ArrayList<String>();
        for (String amenity : dynamicAmenities) {
          if (lower.contains(amenity.toLowerCase())) {
            found.add(amenity);
          }
        }
        if (!found.isEmpty()) {
          pr.setAmenities(found);
          pr.setIntent(Intent.FILTERS_GIVEN);
        }

        // YES/NO
        if (lower.matches("\\b(yes|y|sure|yep)\\b")) {
          pr.setConfirm(true); pr.setIntent(Intent.YES);
        }
        if (lower.matches("\\b(no|nah|nope)\\b")) {
          pr.setConfirm(false); pr.setIntent(Intent.NO);
        }

        // HOTEL INDEX
        if (Stage.SHOW_HOTELS.name().equals(currentStageName)) {
          Matcher idx = Pattern.compile("\\b(\\d+)\\b").matcher(text);
          if (idx.find()) {
            pr.setHotelIndex(Integer.valueOf(idx.group(1)));
            pr.setIntent(Intent.HOTEL_INDEX);
          }
        }

        // SERVICES
        if (Stage.ASK_SERVICES.name().equals(currentStageName)) {
          if (lower.contains("none")) {
            pr.setServiceIndices(List.of());
            pr.setIntent(Intent.SERVICES_NONE);
          } else {
            var nums = new ArrayList<Integer>();
            Matcher mn = Pattern.compile("(\\d+)").matcher(text);
            while (mn.find()) nums.add(Integer.valueOf(mn.group(1)));
            if (!nums.isEmpty()) {
              pr.setServiceIndices(nums);
              pr.setIntent(Intent.SERVICES_GIVEN);
            }
          }
          Matcher mq = Pattern.compile("for\\s+(\\d+)\\s+days?", Pattern.CASE_INSENSITIVE)
                         .matcher(text);
          if (mq.find()) pr.setServiceQuantityDays(Integer.valueOf(mq.group(1)));
        }

        // ROOM TYPE
        if (Stage.ASK_ROOM_TYPE.name().equals(currentStageName)) {
          pr.setRoomType(text.trim());
          pr.setIntent(Intent.ROOM_TYPE_GIVEN);
        }

        // PAY
        if (lower.contains("pay")) pr.setIntent(Intent.PAY);

        // FEEDBACK
        if (Stage.ASK_FEEDBACK.name().equals(currentStageName)) {
          Matcher mr = Pattern.compile("(\\d)\\s*(?:stars?)?").matcher(text);
          if (mr.find()) {
            pr.setRating(Integer.valueOf(mr.group(1)));
            pr.setComments(text.replaceFirst(mr.group(1), "").trim());
            pr.setIntent(Intent.FEEDBACK);
          }
        }

        return pr;
    }
}
