package com.synex.domain;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "hotel_rooms")
public class HotelRoom {
	@Id
	@Column(name = "hotel_room_id")
	private Integer hotelRoomId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "hotel_id", nullable = false)
	@JsonIgnore
	private Hotel hotel;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "type_id", nullable = false)
	private RoomType type;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "hotel_rooms_amenities", joinColumns = @JoinColumn(name = "hotel_room_id"), inverseJoinColumns = @JoinColumn(name = "a_id"))
	private Set<Amenity> amenities = new HashSet<>();

	@Column(name = "no_rooms")
	private Integer noRooms;

	private float price;
	private float discount;
	private String description;
	private String policies;

	@Transient
	private String hotelName;

	@Transient
	private String roomTypeName;

	@Transient
	private Set<String> amenityNames = new HashSet<>();

	@PostLoad
	private void initTransients() {
		this.hotelName = hotel.getHotelName();
		this.roomTypeName = type.getName();
		this.amenityNames = amenities.stream().map(Amenity::getName).collect(Collectors.toSet());
	}

	@JsonProperty("hotelName")
	public String getHotelName() {
		return hotelName;
	}

	@JsonProperty("roomType")
	public String getRoomTypeName() {
		return roomTypeName;
	}

	@JsonProperty("amenities")
	public Set<String> getAmenityNames() {
		return amenityNames;
	}

	public Integer getHotelRoomId() {
		return hotelRoomId;
	}

	public void setHotelRoomId(Integer hotelRoomId) {
		this.hotelRoomId = hotelRoomId;
	}

	public Hotel getHotel() {
		return hotel;
	}

	public void setHotel(Hotel hotel) {
		this.hotel = hotel;
	}

	public RoomType getType() {
		return type;
	}

	public void setType(RoomType type) {
		this.type = type;
	}

	public Set<Amenity> getAmenities() {
		return amenities;
	}

	public void setAmenities(Set<Amenity> amenities) {
		this.amenities = amenities;
	}

	public Integer getNoRooms() {
		return noRooms;
	}

	public void setNoRooms(Integer noRooms) {
		this.noRooms = noRooms;
	}

	public Float getPrice() {
		return price;
	}

	public void setPrice(Float price) {
		this.price = price;
	}

	public Float getDiscount() {
		return discount;
	}

	public void setDiscount(Float discount) {
		this.discount = discount;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPolicies() {
		return policies;
	}

	public void setPolicies(String policies) {
		this.policies = policies;
	}

	public void setPrice(float price) {
		this.price = price;
	}

	public void setDiscount(float discount) {
		this.discount = discount;
	}

}
