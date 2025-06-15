package com.synex.domain;

import java.util.HashSet;
import java.util.Set;

public class DialogState {
	private String location;
	private Double maxPrice;
	private Integer minStars;
	private Set<String> amenities = new HashSet<>();
	private boolean inHotelFlow;

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Double getMaxPrice() {
		return maxPrice;
	}

	public void setMaxPrice(Double maxPrice) {
		this.maxPrice = maxPrice;
	}

	public Integer getMinStars() {
		return minStars;
	}

	public void setMinStars(Integer minStars) {
		this.minStars = minStars;
	}

	public Set<String> getAmenities() {
		return amenities;
	}

	public void setAmenities(Set<String> amenities) {
		this.amenities = amenities;
	}

	public boolean isInHotelFlow() {
		return inHotelFlow;
	}

	public void setInHotelFlow(boolean inHotelFlow) {
		this.inHotelFlow = inHotelFlow;
	}

}
