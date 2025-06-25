package com.synex.domain;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.*;

@Entity
public class Feedback {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ElementCollection
	@CollectionTable(name = "feedback_booking_ids", joinColumns = @JoinColumn(name = "feedback_id"))
	@Column(name = "booking_id")
	private List<Integer> bookingIds = new ArrayList<>();

	@Column(nullable = false)
	private Long rating;

	@Column(length = 1000)
	private String comments;

	public Feedback() {
		super();
	}

	public Feedback(Long id, List<Integer> bookingIds, Long rating, String comments) {
		super();
		this.id = id;
		this.bookingIds = bookingIds;
		this.rating = rating;
		this.comments = comments;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List<Integer> getBookingIds() {
		return bookingIds;
	}

	public void setBookingIds(List<Integer> bookingIds) {
		this.bookingIds = bookingIds;
	}

	public Long getRating() {
		return rating;
	}

	public void setRating(Long rating) {
		this.rating = rating;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

}
