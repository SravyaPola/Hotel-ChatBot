// src/main/java/com/synex/domain/ServiceOption.java
package com.synex.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "service_options")
public class ServiceOption {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "so_id")
	private Integer id;

	@Column(name = "hotel_id", nullable = false)
	private Integer hotelId;

	@Column(nullable = false)
	private String name;

	@Column(length = 500)
	private String description;

	@Column(nullable = false)
	private Double price;

	@Column(name = "per_person", nullable = false)
	private Boolean perPerson;

	// getters & setters
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getHotelId() {
		return hotelId;
	}

	public void setHotelId(Integer hotelId) {
		this.hotelId = hotelId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public Boolean getPerPerson() {
		return perPerson;
	}

	public void setPerPerson(Boolean perPerson) {
		this.perPerson = perPerson;
	}
}
