package com.synex.service;

import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import com.synex.domain.Feedback;
import com.synex.repository.FeedbackRepository;

@Service
public class FeedbackService {

	@Autowired
	private FeedbackRepository repo;

	/**
	 * Save a pre-constructed Feedback entity.
	 */
	public Feedback save(Feedback f) {
		return repo.save(f);
	}

	/**
	 * Create one Feedback row PER bookingId in the list. Each Feedback will get its
	 * own row (with a singleton bookingIds list).
	 */
	@Transactional
	public void save(List<Integer> bookingIds, Integer rating, String comments) {

		for (Integer bookingId : bookingIds) {
			Feedback fb = new Feedback();
			// since your entity maps bookingIds as an @ElementCollection,
			// we wrap the single ID in a singleton list:
			fb.setBookingIds(Collections.singletonList(bookingId));
			fb.setComments(comments);
			repo.save(fb);
		}
	}
}
