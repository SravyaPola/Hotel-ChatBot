package com.synex.config;

import com.synex.domain.KnowledgeDocument;
import com.synex.repository.DocumentRepository;
import com.synex.service.EmbeddingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class DataLoader {

	@Bean
	CommandLineRunner loadDocs(DocumentRepository repo, EmbeddingService embeddingService) {
		return args -> {
			if (repo.count() > 0)
				return;
			List<String> faqs = List.of("Our check-in time is 3 PM…", "We offer free Wi-Fi…", "The spa is open…");
			for (var text : faqs) {
				float[] vec = embeddingService.embed(text);
				repo.save(new KnowledgeDocument(text, vec));
			}
		};
	}

}
