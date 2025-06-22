package com.synex.service;

import java.util.*;

public class AmenityMatcher {
	public static Set<String> matchUserAmenitiesToDb(String userAmenityCsv, List<String> dbAmenities) {
		if (userAmenityCsv == null || userAmenityCsv.isBlank())
			return Set.of();
		Set<String> matched = new HashSet<>();
		String[] userAms = userAmenityCsv.split(",");
		for (String given : userAms) {
			String clean = given.trim().toLowerCase().replaceAll("[^a-z]", "");
			for (String dbAm : dbAmenities) {
				String dbClean = dbAm.toLowerCase().replaceAll("[^a-z]", "");
				// Use contains or is contained (simple fuzzy)
				if (clean.contains(dbClean) || dbClean.contains(clean)) {
					matched.add(dbAm); // Use DB/canonical name!
				}
			}
		}
		return matched;
	}
}
