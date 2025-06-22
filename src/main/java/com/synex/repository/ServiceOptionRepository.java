// src/main/java/com/synex/repo/ServiceOptionRepository.java
package com.synex.repository;

import com.synex.domain.ServiceOption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceOptionRepository extends JpaRepository<ServiceOption, Integer> {
	List<ServiceOption> findByHotelId(Integer hotelId);
}
