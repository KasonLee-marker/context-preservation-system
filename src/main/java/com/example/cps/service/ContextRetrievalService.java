package com.example.cps.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 上下文检索服务
 */
@Service
@Slf4j
public class ContextRetrievalService {
    
    @Autowired
    private MilvusService milvusService;
    
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
            // 使用 MilvusService 直接搜索
            List<String> contents = milvusService.search(query, topK);
            
            log.info("Retrieved {} results from Milvus", contents.size());
            
            // 转换为 RetrievedContext
            List<RetrievedContext> result = new ArrayList<>();
            for (String content : contents) {
                RetrievedContext ctx = RetrievedContext.builder()
                    .originalText(content)
                    .score(0.8)
                    .build();
                result.add(ctx);
            }
            return result;
                
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
