// src/main/java/com/synex/repository/DocumentRepository.java
package com.synex.repository;

import com.synex.domain.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

	@Query(value = """
			 SELECT *
			   FROM knowledge_documents
			  ORDER BY embedding <-> CAST(:emb AS vector)
			  LIMIT :k
			""", nativeQuery = true)
	List<KnowledgeDocument> findTopKSimilar(@Param("emb") float[] emb, @Param("k") int k);
}
