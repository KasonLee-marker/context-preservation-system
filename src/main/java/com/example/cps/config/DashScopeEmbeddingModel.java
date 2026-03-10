package com.example.cps.config;

import com.example.cps.service.DashScopeEmbeddingService;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;

/**
 * 阿里云灵积 EmbeddingModel 实现
 * 适配 Spring AI 接口
 */
public class DashScopeEmbeddingModel implements EmbeddingModel {
    
    private final DashScopeEmbeddingService embeddingService;
    
    public DashScopeEmbeddingModel(DashScopeEmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }
    
    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> texts = request.getInstructions();
        List<float[]> embeddings = embeddingService.embedBatch(texts);
        
        return new EmbeddingResponse(
            embeddings.stream()
                .map(e -> new org.springframework.ai.embedding.Embedding(e, 0))
                .toList()
        );
    }
    
    @Override
    public float[] embed(Document document) {
        return embeddingService.embed(document.getText());
    }
    
    @Override
    public int dimensions() {
        return embeddingService.getDimension();
    }
}
