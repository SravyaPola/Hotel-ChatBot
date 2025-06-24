package com.synex.service;

import com.synex.repository.AmenityRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AmenityService {

	private final AmenityRepository amenityRepository;

	public AmenityService(AmenityRepository amenityRepository) {
		this.amenityRepository = amenityRepository;
	}

	/**
	 * @return all distinct amenity names, alphabetically ordered
	 */
	public List<String> getAllAmenities() {
		return amenityRepository.findDistinctNames();
	}
}
