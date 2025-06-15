// src/main/java/com/synex/service/RecommendationService.java
package com.synex.service;

import com.synex.domain.KnowledgeDocument;
import com.synex.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

	private final DocumentRepository docRepo;
	private final EmbeddingService embedSvc;
	private final int topK;

	public RecommendationService(DocumentRepository docRepo, EmbeddingService embedSvc,
			@Value("${chatbot.topk}") int topK) {
		this.docRepo = docRepo;
		this.embedSvc = embedSvc;
		this.topK = topK;
	}

	/**
	 * Return the contents of the top-K nearest documents as “recommendations.”
	 */
	public List<String> suggestFor(String userMessage) {
		float[] vec = embedSvc.embed(userMessage);
		List<KnowledgeDocument> docs = docRepo.findTopKSimilar(vec, topK);
		return docs.stream().map(KnowledgeDocument::getContent).collect(Collectors.toList());
	}
}
