package com.example.cps.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云灵积 Embedding 服务 - HTTP 直接调用
 * 使用官方 multimodal-embedding API
 * 免费额度：100万次调用/月
 */
@Service
@Slf4j
public class DashScopeEmbeddingService {
    
    @Value("${dashscope.api-key:}")
    private String apiKey;
    
    private static final String EMBEDDING_MODEL = "tongyi-embedding-vision-plus";
    private static final int DIMENSION = 1152;  // tongyi-embedding-vision-plus 实际维度
    // 使用官方 multimodal-embedding API
    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding";
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 单文本嵌入
     */
    public float[] embed(String text) {
        try {
            log.info("Embedding text: {}, API Key length: {}", text.substring(0, Math.min(20, text.length())), apiKey != null ? apiKey.length() : 0);
            
            if (apiKey == null || apiKey.isEmpty()) {
                log.error("DASHSCOPE_API_KEY is not set!");
                return new float[DIMENSION];
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 使用官方推荐的请求格式
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", EMBEDDING_MODEL);
            
            // 构建 contents 数组
            List<Map<String, String>> contents = new ArrayList<>();
            Map<String, String> content = new HashMap<>();
            content.put("text", text);
            contents.add(content);
            
            Map<String, Object> input = new HashMap<>();
            input.put("contents", contents);
            requestBody.put("input", input);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, request, Map.class);
            
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("output")) {
                Map<String, Object> output = (Map<String, Object>) body.get("output");
                List<Map<String, Object>> embeddings = (List<Map<String, Object>>) output.get("embeddings");
                
                if (!embeddings.isEmpty()) {
                    List<Double> embedding = (List<Double>) embeddings.get(0).get("embedding");
                    return toFloatArray(embedding);
                }
            }
            
            // 降级：返回零向量
            log.warn("Embedding response empty, returning zero vector");
            return new float[DIMENSION];
            
        } catch (Exception e) {
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
