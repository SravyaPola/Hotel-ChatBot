package com.synex.domain;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Feedback {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 1–5 star rating provided by the user. */
	private Long rating;

	/** Free-form comments from the user. */
	private String comments;

	private List<Integer> bookingIds;

	public Feedback() {
	}

	public Feedback(Long rating, String comments) {
		this.rating = rating;
		this.comments = comments;
	}

	public Long getId() {
		return id;
	}

	public Long getRating() {
		return rating;
	}

	public void setRating(Long rating2) {
		this.rating = rating2;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public List<Integer> getBookingIds() {
		return bookingIds;
	}

	public void setBookingIds(List<Integer> bookingIds) {
		this.bookingIds = bookingIds;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
