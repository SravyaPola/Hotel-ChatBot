package com.synex.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "amenities")
public class Amenity {
	@Id
	@Column(name = "a_id")
	private Integer id;

	private String name;

	@JsonCreator
	public static Amenity from(String value) {
		Amenity a = new Amenity();
		a.setName(value);
		return a;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
