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
    
    // 模拟内存中的对话存储（实际应用应使用 Redis 或数据库）
    private final List<Message> currentMessages = new ArrayList<>();
    private String currentSessionId = UUID.randomUUID().toString();
    private String currentUserId = "user-001";
    
    /**
     * 发送消息
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        log.info("Received message: {}", request.getContent());
        
        // 1. 添加用户消息
        Message userMessage = Message.user(request.getContent());
        currentMessages.add(userMessage);
        
        // 2. 检查是否需要保存上下文
        preservationService.preserveIfNeeded(
            currentMessages, 
            currentSessionId, 
            currentUserId
        );
        
        // 3. 检索相关历史
        List<ContextRetrievalService.RetrievedContext> relevantContexts = 
            retrievalService.retrieveRelevantContext(
                request.getContent(), 
                currentUserId, 
                currentSessionId
            );
        
        // 4. 构建增强提示
        String augmentedPrompt = retrievalService.buildAugmentedPrompt(
            request.getContent(), 
            relevantContexts
        );
        
        // 5. 模拟 LLM 回复（实际应调用 LLM）
        String response = simulateLLMResponse(augmentedPrompt);
        
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
     * 手动触发保存
     */
    @PostMapping("/preserve")
    public ResponseEntity<String> preserve() {
        preservationService.preserve(currentMessages, currentSessionId, currentUserId);
        return ResponseEntity.ok("Context preserved successfully");
    }
    
    /**
     * 检索相关历史
     */
    @PostMapping("/retrieve")
    public ResponseEntity<List<ContextRetrievalService.RetrievedContext>> retrieve(
        @RequestBody RetrieveRequest request
    ) {
        List<ContextRetrievalService.RetrievedContext> contexts = 
            retrievalService.retrieveRelevantContext(
                request.getQuery(), 
                currentUserId, 
                currentSessionId
            );
        return ResponseEntity.ok(contexts);
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
    
    /**
     * 模拟 LLM 响应
     */
    private String simulateLLMResponse(String prompt) {
        // 实际应用中这里调用 LLM
        if (prompt.contains("历史对话")) {
            return "基于历史对话，我理解您之前讨论过相关话题。我来继续回答您的问题...";
        }
        return "这是一个模拟回复。实际应用中会调用 LLM API。";
    }
    
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
