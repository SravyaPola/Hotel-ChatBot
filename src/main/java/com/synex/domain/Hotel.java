package com.synex.domain;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.JoinColumn;

@Entity
@Table(name = "hotels")
public class Hotel {
	@Id
	@Column(name = "hotel_id")
	private int hotelId;
	private String hotelName;
	private String address;
	private String city;
	private String state;
	private int starRating;
	private double averagePrice;
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "hotels_amenities", joinColumns = @JoinColumn(name = "hotel_id"), inverseJoinColumns = @JoinColumn(name = "a_id"))
	private Set<Amenity> amenities = new HashSet<>();
	private double discount;
	private String description;
	private String email;
	private String mobile;
	private String imageURL;
	private int timesBooked;

	@OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL)
	@JsonIgnore
	private Set<HotelRoom> hotelRooms = new HashSet<>();

	@Transient
	private Set<String> amenityNames = new HashSet<>();

	@PostLoad
	private void initAmenityNames() {
		this.amenityNames = amenities.stream().map(Amenity::getName).collect(Collectors.toSet());
	}

	@JsonProperty("amenities")
	public Set<String> getAmenityNames() {
		return amenityNames;
	}

	public Hotel() {
		super();
	}

	public Hotel(int hotelId, String hotelName, String address, String city, String state, int starRating,
			double averagePrice, Set<Amenity> amenities, double discount, String description, String email,
			String mobile, String imageURL, int timesBooked, Set<HotelRoom> hotelRooms, Set<String> amenityNames) {
		super();
		this.hotelId = hotelId;
		this.hotelName = hotelName;
		this.address = address;
		this.city = city;
		this.state = state;
		this.starRating = starRating;
		this.averagePrice = averagePrice;
		this.amenities = amenities;
		this.discount = discount;
		this.description = description;
		this.email = email;
		this.mobile = mobile;
		this.imageURL = imageURL;
		this.timesBooked = timesBooked;
		this.hotelRooms = hotelRooms;
		this.amenityNames = amenityNames;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public int getTimesBooked() {
		return timesBooked;
	}

	public void setTimesBooked(int timesBooked) {
		this.timesBooked = timesBooked;
	}

	public Set<HotelRoom> getHotelRooms() {
		return hotelRooms;
	}

	public void setHotelRooms(Set<HotelRoom> hotelRooms) {
		this.hotelRooms = hotelRooms;
	}

	public Set<Amenity> getAmenities() {
		return amenities;
	}

	public void setAmenities(Set<Amenity> hotelAmenities) {
		this.amenities = hotelAmenities;
	}

	public Set<String> getHotelAmenityNames() {
		return amenityNames;
	}

	public void setHotelAmenityNames(Set<String> amenityNames) {
		this.amenityNames = amenityNames;
	}

	public String getHotelName() {
		return hotelName;
	}

	public void setHotelName(String hotelName) {
		this.hotelName = hotelName;
	}

	public int getHotelId() {
		return hotelId;
	}

	public void setHotelId(int hotelId) {
		this.hotelId = hotelId;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getStarRating() {
		return starRating;
	}

	public void setStarRating(int starRating) {
		this.starRating = starRating;
	}

	public double getAveragePrice() {
		return averagePrice;
	}

	public void setAveragePrice(double averagePrice) {
		this.averagePrice = averagePrice;
	}

	public double getDiscount() {
		return discount;
	}

	public void setDiscount(double discount) {
		this.discount = discount;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getImageURL() {
		return imageURL;
	}

	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}
}
