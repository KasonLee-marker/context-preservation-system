package com.example.cps.controller;

import com.example.cps.entity.Message;
import com.example.cps.service.ContextPreservationService;
import com.example.cps.service.ContextRetrievalService;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 对话控制器
 */
@RestController
@RequestMapping("/api/conversation")
@Slf4j
public class ConversationController {
    
    @Autowired
    private ContextPreservationService preservationService;
    
    @Autowired
    private ContextRetrievalService retrievalService;
    
    @Autowired
    private com.example.cps.service.LLMService llmService;
    
    @Autowired
    private com.example.cps.service.AsyncService asyncService;
    
    // 模拟内存中的对话存储（实际应用应使用 Redis 或数据库）
    private final List<Message> currentMessages = new ArrayList<>();
    private String currentSessionId = UUID.randomUUID().toString();
    private String currentUserId = "user-001";
    
    /**
     * 发送消息（异步版本，10秒超时）
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        log.info("[API] 收到消息: {}", request.getContent());
        
        // 1. 添加用户消息
        Message userMessage = Message.user(request.getContent());
        currentMessages.add(userMessage);
        
        // 2. 异步保存上下文（不阻塞）
        asyncService.preserveAsync(currentMessages, currentSessionId, currentUserId)
            .thenRun(() -> log.info("[Async] 上下文保存完成"))
            .exceptionally(e -> {
                log.error("[Async] 上下文保存失败: {}", e.getMessage());
                return null;
            });
        
        // 3. 同步检索（10秒超时）
        List<ContextRetrievalService.RetrievedContext> relevantContexts = List.of();
        try {
            relevantContexts = retrievalService.retrieveRelevantContext(
                request.getContent(), 
                currentUserId, 
                currentSessionId
            );
            log.info("[API] 检索完成，找到 {} 条相关上下文", relevantContexts.size());
        } catch (Exception e) {
            log.error("[API] 检索失败: {}", e.getMessage());
        }
        
        // 4. 构建增强提示
        String augmentedPrompt = retrievalService.buildAugmentedPrompt(
            request.getContent(), 
            relevantContexts
        );
        
        // 5. 异步调用 LLM（10秒超时）
        String response;
        try {
            response = asyncService.generateLlmResponseAsync(augmentedPrompt, llmService)
                .get(10, java.util.concurrent.TimeUnit.SECONDS);
            log.info("[API] LLM 响应生成成功");
        } catch (Exception e) {
            log.error("[API] LLM 调用超时或失败: {}", e.getMessage());
            response = "抱歉，服务响应超时，请稍后重试。";
        }
        
        Message assistantMessage = Message.assistant(response);
        currentMessages.add(assistantMessage);
        
        return ResponseEntity.ok(ChatResponse.builder()
            .message(response)
            .sessionId(currentSessionId)
            .contextRetrieved(!relevantContexts.isEmpty())
            .retrievedCount(relevantContexts.size())
            .build());
    }
    
    /**
     * 获取当前对话历史
     */
    @GetMapping("/history")
    public ResponseEntity<List<Message>> getHistory() {
        return ResponseEntity.ok(currentMessages);
    }
    
    /**
     * 手动触发保存（异步版本，10秒超时）
     */
    @PostMapping("/preserve")
    public ResponseEntity<String> preserve() {
        log.info("[API] 收到保存请求");
        try {
            asyncService.preserveAsync(currentMessages, currentSessionId, currentUserId)
                .get(10, java.util.concurrent.TimeUnit.SECONDS);
            log.info("[API] 保存成功");
            return ResponseEntity.ok("Context preserved successfully");
        } catch (Exception e) {
            log.error("[API] 保存失败或超时: {}", e.getMessage());
            return ResponseEntity.status(504).body("保存超时: " + e.getMessage());
        }
    }
    
    /**
     * 检索相关历史（异步版本，10秒超时）
     */
    @PostMapping("/retrieve")
    public ResponseEntity<List<ContextRetrievalService.RetrievedContext>> retrieve(
        @RequestBody RetrieveRequest request
    ) {
        log.info("[API] 收到检索请求: {}", request.getQuery());
        try {
            // 注意：检索本身是同步的，但内部 Embedding 是异步的
            List<ContextRetrievalService.RetrievedContext> contexts = 
                retrievalService.retrieveRelevantContext(
                    request.getQuery(), 
                    currentUserId, 
                    currentSessionId
                );
            log.info("[API] 检索完成，找到 {} 条结果", contexts.size());
            return ResponseEntity.ok(contexts);
        } catch (Exception e) {
            log.error("[API] 检索失败: {}", e.getMessage());
            return ResponseEntity.status(504).body(List.of());
        }
    }
    
    /**
     * 清空对话
     */
    @PostMapping("/clear")
    public ResponseEntity<String> clear() {
        currentMessages.clear();
        currentSessionId = UUID.randomUUID().toString();
        return ResponseEntity.ok("Conversation cleared, new session: " + currentSessionId);
    }
    
    // LLM 服务已注入，删除旧的模拟方法
    
    // 请求/响应对象
    @Data
    public static class ChatRequest {
        private String content;
    }
    
    @Data
    @Builder
    public static class ChatResponse {
        private String message;
        private String sessionId;
        private boolean contextRetrieved;
        private int retrievedCount;
    }
    
    @Data
    public static class RetrieveRequest {
        private String query;
    }
}
