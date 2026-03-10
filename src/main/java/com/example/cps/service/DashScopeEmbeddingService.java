package com.example.cps.service;

import com.alibaba.dashscope.embeddings.Embedding;
import com.alibaba.dashscope.embeddings.EmbeddingList;
import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 阿里云灵积 Embedding 服务
 * 免费额度：100万次调用/月
 */
@Service
@Slf4j
public class DashScopeEmbeddingService {
    
    @Value("${dashscope.api-key:}")
    private String apiKey;
    
    private static final String EMBEDDING_MODEL = "tongyi-embedding-vision-plus";
    private static final int DIMENSION = 1536;
    
    /**
     * 单文本嵌入
     */
    public float[] embed(String text) {
        try {
            TextEmbeddingParam param = TextEmbeddingParam.builder()
                .apiKey(apiKey)
                .model(EMBEDDING_MODEL)
                .text(text)
                .build();
            
            TextEmbedding textEmbedding = new TextEmbedding();
            EmbeddingList<Embedding> result = textEmbedding.call(param);
            
            if (result.getOutput().getEmbeddings().isEmpty()) {
                throw new RuntimeException("Embedding result is empty");
            }
            
            List<Double> embedding = result.getOutput().getEmbeddings().get(0).getEmbedding();
            return toFloatArray(embedding);
            
        } catch (ApiException | NoApiKeyException e) {
            log.error("Failed to get embedding from DashScope", e);
            // 降级：返回零向量
            return new float[DIMENSION];
        }
    }
    
    /**
     * 批量嵌入
     */
    public List<float[]> embedBatch(List<String> texts) {
        return texts.stream()
            .map(this::embed)
            .toList();
    }
    
    /**
     * Double List 转 float array
     */
    private float[] toFloatArray(List<Double> list) {
        float[] result = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i).floatValue();
        }
        return result;
    }
    
    /**
     * 获取维度
     */
    public int getDimension() {
        return DIMENSION;
    }
}
