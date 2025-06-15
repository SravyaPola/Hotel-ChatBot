// src/main/java/com/synex/domain/KnowledgeDocument.java
package com.synex.domain;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;

@Entity
@Table(name = "knowledge_documents")
public class KnowledgeDocument {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(columnDefinition = "text")
	private String content;

	@Column(columnDefinition = "vector")
	@JdbcTypeCode(SqlTypes.VECTOR)
	private float[] embedding;

	public KnowledgeDocument() {
	}

	public KnowledgeDocument(String content, float[] embedding) {
		this.content = content;
		this.embedding = embedding;
	}

	public Long getId() {
		return id;
	}

	public String getContent() {
		return content;
	}

	public float[] getEmbedding() {
		return embedding;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setEmbedding(float[] embedding) {
		this.embedding = embedding;
	}
}
