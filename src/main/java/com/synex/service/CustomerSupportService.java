package com.synex.service;

import com.synex.domain.AgentRequest;
import com.synex.repository.AgentRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerSupportService {

	private final AgentRequestRepository repo;

	@Autowired
	public CustomerSupportService(AgentRequestRepository repo) {
		this.repo = repo;
	}

	public AgentRequest save(AgentRequest req) {
		return repo.save(req);
	}
}
