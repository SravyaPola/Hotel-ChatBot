package com.synex.repository;

import com.synex.domain.AgentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentRequestRepository extends JpaRepository<AgentRequest, Long> {
	// no extra methods needed for simple save(...)
}
