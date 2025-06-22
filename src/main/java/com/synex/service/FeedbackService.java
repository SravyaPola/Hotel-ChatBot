package com.synex.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.synex.domain.Feedback;
import com.synex.repository.FeedbackRepository;

@Service
public class FeedbackService {
	@Autowired
	private FeedbackRepository repo;

	public Feedback save(Feedback f) {
		return repo.save(f);
	}

	public void save(List<Integer> bookingIds, Integer feedbackRating, String feedbackComments) {

	}
}
