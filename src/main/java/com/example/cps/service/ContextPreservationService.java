package com.example.cps.service;

import com.example.cps.entity.ConversationChunk;
import com.example.cps.entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 上下文保存服务 - 核心服务
 */
@Service
@Slf4j
public class ContextPreservationService {
    
    @Autowired
    private TokenEstimator tokenEstimator;
    
    @Autowired
    private SummaryGenerator summaryGenerator;
    
    @Autowired
    private KeyInfoExtractor keyInfoExtractor;
    
    @Autowired
    private VectorStore vectorStore;
    
    @Autowired
    private DashScopeEmbeddingService dashScopeEmbeddingService;
    
    @Value("${context.preservation.threshold:3000}")
    private int tokenThreshold;
    
    /**
     * 检查是否需要保存，并执行保存
     */
    public void preserveIfNeeded(List<Message> messages, String sessionId, String userId) {
        int tokenCount = tokenEstimator.estimateTokens(messages);
        
        if (tokenCount > tokenThreshold) {
            log.info("Token count {} exceeds threshold {}, triggering preservation", 
                tokenCount, tokenThreshold);
            
            preserve(messages, sessionId, userId);
            
            // 压缩上下文 - 保留最近的消息
            compressContext(messages);
        }
    }
    
    /**
     * 保存上下文到向量数据库
     */
    public void preserve(List<Message> messages, String sessionId, String userId) {
        try {
            // 1. 生成摘要
            String summary = summaryGenerator.generateSummary(messages);
            log.debug("Generated summary: {}", summary);
            
            // 2. 提取关键信息
            String keyInfo = keyInfoExtractor.extractKeyInfo(messages);
            log.debug("Extracted key info: {}", keyInfo);
            
            // 3. 保留重要原文
            String importantText = keyInfoExtractor.extractImportantText(messages);
            
            // 4. 创建对话片段
            ConversationChunk chunk = ConversationChunk.builder()
                .id(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .userId(userId)
                .summary(summary)
                .keyInfo(keyInfo)
                .originalText(importantText)
                .topic(extractTopic(summary))
                .importance(calculateImportance(messages))
                .timestamp(LocalDateTime.now())
                .build();
            
            // 5. 保存到向量数据库
            saveToVectorStore(chunk);
            
            log.info("Successfully preserved context chunk: {}", chunk.getId());
            
        } catch (Exception e) {
            log.error("Failed to preserve context", e);
        }
    }
    
    /**
     * 保存到向量数据库
     */
    private void saveToVectorStore(ConversationChunk chunk) {
        // 使用 DashScope 生成 Embedding
        String embeddingText = combineForEmbedding(chunk);
        float[] embedding = dashScopeEmbeddingService.embed(embeddingText);
        
        // 创建 Document 对象
        Document document = new Document(
            chunk.getId(),
            embeddingText,
            java.util.Map.of(
                "sessionId", chunk.getSessionId(),
                "userId", chunk.getUserId(),
                "summary", chunk.getSummary(),
                "keyInfo", chunk.getKeyInfo(),
                "originalText", chunk.getOriginalText(),
                "timestamp", chunk.getTimestamp().toString(),
                "embedding", embedding  // 存储向量
            )
        );
        
        vectorStore.add(List.of(document));
    }
    
    /**
     * 组合用于嵌入的文本
     */
    private String combineForEmbedding(ConversationChunk chunk) {
        return String.format("""
            Summary: %s
            
            Key Information: %s
            
            Original Text: %s
            """,
            chunk.getSummary(),
            chunk.getKeyInfo(),
            chunk.getOriginalText()
        );
    }
    
    /**
     * 压缩上下文 - 保留最近的消息
     */
    private void compressContext(List<Message> messages) {
        int keepCount = 5; // 保留最近5条
        
        if (messages.size() > keepCount) {
            List<Message> toRemove = messages.subList(0, messages.size() - keepCount);
            toRemove.clear();
            log.info("Compressed context, retained {} recent messages", keepCount);
        }
    }
    
    /**
     * 提取主题
     */
    private String extractTopic(String summary) {
        // 简单提取第一行作为主题
        if (summary.contains("主题")) {
            int start = summary.indexOf("主题");
            int end = summary.indexOf("\n", start);
            if (end > start) {
                return summary.substring(start, end).replace("主题:", "").trim();
            }
        }
        return "general";
    }
    
    /**
     * 计算重要度
     */
    private Double calculateImportance(List<Message> messages) {
        // 基于消息数量和长度计算
        int totalLength = messages.stream()
            .mapToInt(m -> m.getContent().length())
            .sum();
        
        // 归一化到 0-1
        return Math.min(1.0, totalLength / 1000.0);
    }
}
