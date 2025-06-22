// src/main/java/com/synex/config/OpenAiConfig.java
package com.synex.config;

import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

//@Configuration
//public class OpenAiConfig {
//
//    /**
//     * Exposes com.theokanning.openai.service.OpenAiService for your EmbeddingService/ChatService.
//     * Reads the API key from application.properties (openai.api-key).
//     */
//    @Bean
//    public OpenAiService openAiService(
//            @Value("${openai.api.key}") String apiKey,
//            @Value("${openai.timeout-seconds:30}") long timeoutSeconds
//    ) {
//        // you can adjust the timeout as needed
//        return new OpenAiService(apiKey, Duration.ofSeconds(timeoutSeconds));
//    }
//}
@Configuration
public class OpenAiConfig {
	@Value("${openai.api.key}")
	private String apiKey;

	@Bean
	public OpenAiService openAiService() {
		return new OpenAiService(apiKey);
	}
}
