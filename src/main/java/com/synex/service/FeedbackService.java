// src/main/java/com/synex/service/FeedbackService.java
package com.synex.service;

import com.synex.domain.Feedback;
import com.synex.repository.FeedbackRepository;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {

	private final FeedbackRepository feedbackRepo;

	public FeedbackService(FeedbackRepository feedbackRepo) {
		this.feedbackRepo = feedbackRepo;
	}

	/** Persist guest feedback */
	public void saveFeedback(String message) {
		feedbackRepo.save(new Feedback(message));
	}
}
