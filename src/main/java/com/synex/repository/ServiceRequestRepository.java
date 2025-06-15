// src/main/java/com/synex/repository/ServiceRequestRepository.java
package com.synex.repository;

import com.synex.domain.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
}
