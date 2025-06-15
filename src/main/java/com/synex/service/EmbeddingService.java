// src/main/java/com/synex/service/EmbeddingService.java
package com.synex.service;

import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {
    private final OpenAiService openAi;
    private final String model;

    public EmbeddingService(OpenAiService openAi,
                            @Value("${chatbot.embedding-model}") String model) {
        this.openAi = openAi;
        this.model  = model;
    }

    /** Call OpenAI and return a primitive float[] for Hibernate Vector. */
    public float[] embed(String text) {
        var req = EmbeddingRequest.builder()
                     .model(model)
                     .input(List.of(text))
                     .build();
        var data = openAi.createEmbeddings(req).getData().get(0).getEmbedding();

        float[] vec = new float[data.size()];
        for (int i = 0; i < data.size(); i++) {
            vec[i] = data.get(i).floatValue();
        }
        return vec;
    }
}

