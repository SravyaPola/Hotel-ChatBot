// src/main/java/com/synex/domain/ServiceRequest.java
package com.synex.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_requests")
public class ServiceRequest {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(columnDefinition = "text", nullable = false)
	private String requestText;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public ServiceRequest() {
	}

	public ServiceRequest(String requestText) {
		this.requestText = requestText;
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getRequestText() {
		return requestText;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
