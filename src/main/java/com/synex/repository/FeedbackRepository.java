// src/main/java/com/synex/repository/FeedbackRepository.java
package com.synex.repository;

import com.synex.domain.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
	
}
