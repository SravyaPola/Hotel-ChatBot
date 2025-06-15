// src/main/java/com/synex/service/HotelServiceRequestService.java
package com.synex.service;

import com.synex.domain.ServiceRequest;
import com.synex.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;

@Service
public class HotelServiceRequestService {

	private final ServiceRequestRepository reqRepo;

	public HotelServiceRequestService(ServiceRequestRepository reqRepo) {
		this.reqRepo = reqRepo;
	}

	/** Persist a room‐service / spa / housekeeping request */
	public void placeRequest(String requestText) {
		reqRepo.save(new ServiceRequest(requestText));
	}
}
