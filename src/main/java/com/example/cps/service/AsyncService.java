package com.example.cps.service;

import com.example.cps.entity.ConversationChunk;
import com.example.cps.entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 异步服务
 */
@Service
@Slf4j
public class AsyncService {
    
    @Autowired
    private DashScopeEmbeddingService embeddingService;
    
    @Autowired
    private ContextPreservationService preservationService;
    
    /**
     * 异步生成 Embedding
     */
    @Async
    public CompletableFuture<float[]> embedAsync(String text) {
        log.info("[Async] 开始生成 Embedding，文本长度: {}", text.length());
        try {
            float[] embedding = embeddingService.embed(text);
            log.info("[Async] Embedding 生成成功，维度: {}", embedding.length);
            return CompletableFuture.completedFuture(embedding);
        } catch (Exception e) {
            log.error("[Async] Embedding 生成失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 异步保存上下文
     */
    @Async
    public CompletableFuture<Void> preserveAsync(List<Message> messages, String sessionId, String userId) {
        log.info("[Async] 开始保存上下文，消息数: {}", messages.size());
        try {
            preservationService.preserve(messages, sessionId, userId);
            log.info("[Async] 上下文保存成功");
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("[Async] 上下文保存失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 异步调用 LLM
     */
    @Async
    public CompletableFuture<String> generateLlmResponseAsync(String prompt, LLMService llmService) {
        log.info("[Async] 开始调用 LLM，Prompt 长度: {}", prompt.length());
        try {
            String response = llmService.generate(prompt);
            log.info("[Async] LLM 调用成功，响应长度: {}", response.length());
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            log.error("[Async] LLM 调用失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
