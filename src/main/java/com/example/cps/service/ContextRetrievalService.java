package com.example.cps.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 上下文检索服务
 */
@Service
@Slf4j
public class ContextRetrievalService {
    
    @Autowired
    private VectorStore vectorStore;
    
    @Autowired
    private DashScopeEmbeddingService dashScopeEmbeddingService;
    
    @Value("${context.retrieval.top-k:5}")
    private int topK;
    
    @Value("${context.retrieval.score-threshold:0.7}")
    private double scoreThreshold;
    
    /**
     * 检索相关历史上下文
     */
    public List<RetrievedContext> retrieveRelevantContext(
        String query,
        String userId,
        String currentSessionId
    ) {
        try {
            // 使用 DashScope 生成查询向量
            float[] queryEmbedding = dashScopeEmbeddingService.embed(query);
            
            // 构建搜索请求
            SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold((float) scoreThreshold)
                .build();
            
            // 执行检索
            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            
            // 过滤和转换
            return documents.stream()
                .filter(doc -> filterByUser(doc, userId))
                .filter(doc -> !isFromCurrentSession(doc, currentSessionId))
                .map(this::convertToRetrievedContext)
                .sorted(Comparator.comparing(RetrievedContext::getScore).reversed())
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Failed to retrieve context", e);
            return List.of();
        }
    }
    
    /**
     * 构建增强提示
     */
    public String buildAugmentedPrompt(String currentQuery, List<RetrievedContext> contexts) {
        if (contexts.isEmpty()) {
            return currentQuery;
        }
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("## 相关历史对话\n\n");
        
        for (int i = 0; i < contexts.size(); i++) {
            RetrievedContext ctx = contexts.get(i);
            prompt.append(String.format("""
                ### 历史片段 %d
                **时间**: %s
                **主题**: %s
                **摘要**: %s
                **关键信息**: %s
                **原文**: %s
                ---
                """,
                i + 1,
                ctx.getTimestamp(),
                ctx.getTopic(),
                truncate(ctx.getSummary(), 200),
                truncate(ctx.getKeyInfo(), 200),
                truncate(ctx.getOriginalText(), 300)
            ));
        }
        
        prompt.append("\n## 当前问题\n");
        prompt.append(currentQuery);
        prompt.append("\n\n请基于以上历史对话和当前问题回答。");
        
        return prompt.toString();
    }
    
    /**
     * 按用户过滤
     */
    private boolean filterByUser(Document doc, String userId) {
        Map<String, Object> metadata = doc.getMetadata();
        String docUserId = (String) metadata.get("userId");
        return docUserId != null && docUserId.equals(userId);
    }
    
    /**
     * 排除当前会话
     */
    private boolean isFromCurrentSession(Document doc, String currentSessionId) {
        Map<String, Object> metadata = doc.getMetadata();
        String sessionId = (String) metadata.get("sessionId");
        return sessionId != null && sessionId.equals(currentSessionId);
    }
    
    /**
     * 转换为检索结果
     */
    private RetrievedContext convertToRetrievedContext(Document doc) {
        Map<String, Object> metadata = doc.getMetadata();
        
        return RetrievedContext.builder()
            .id(doc.getId())
            .summary((String) metadata.get("summary"))
            .keyInfo((String) metadata.get("keyInfo"))
            .originalText((String) metadata.get("originalText"))
            .timestamp((String) metadata.get("timestamp"))
            .topic((String) metadata.getOrDefault("topic", "general"))
            .score((Double) metadata.getOrDefault("score", 0.0))
            .build();
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
    
    /**
     * 检索结果对象
     */
    @lombok.Data
    @lombok.Builder
    public static class RetrievedContext {
        private String id;
        private String summary;
        private String keyInfo;
        private String originalText;
        private String timestamp;
        private String topic;
        private Double score;
    }
}
