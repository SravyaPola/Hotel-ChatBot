package com.synex.service;

import java.util.ArrayList;
import java.util.List;
import com.synex.domain.ServiceOption;

public class MatchServicesUtil {

	public static List<ServiceOption> fuzzyMatchServices(List<String> userInputs, List<ServiceOption> allOpts) {
		org.apache.commons.text.similarity.LevenshteinDistance ld = org.apache.commons.text.similarity.LevenshteinDistance
				.getDefaultInstance();
		List<ServiceOption> results = new ArrayList<>();
		for (String input : userInputs) {
			input = input.toLowerCase();
			ServiceOption best = null;
			int bestScore = 3; // Accept up to 3 typos
			for (ServiceOption opt : allOpts) {
				String name = opt.getName().toLowerCase();
				int score = ld.apply(input, name);
				if (name.contains(input) || input.contains(name) || score < bestScore) {
					best = opt;
					bestScore = score;
				}
			}
			if (best != null && !results.contains(best))
				results.add(best);
		}
		return results;
	}

}
