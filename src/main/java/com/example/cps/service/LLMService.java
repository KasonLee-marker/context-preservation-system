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
 * LLM 服务 - 使用阿里云灵积 Qwen 模型
 */
@Service
@Slf4j
public class LLMService {
    
    @Value("${dashscope.api-key:}")
    private String apiKey;
    
    private static final String MODEL = "qwen-turbo";
    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 生成回复
     */
    public String generate(String prompt) {
        // 如果没有配置 API Key，使用模拟回复
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("DASHSCOPE_API_KEY not configured, using simulated response");
            return simulateResponse(prompt);
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL);
            
            // 构建消息
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            
            Map<String, Object> input = new HashMap<>();
            input.put("messages", messages);
            requestBody.put("input", input);
            
            // 可选参数
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("result_format", "message");
            requestBody.put("parameters", parameters);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, request, Map.class);
            
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("output")) {
                Map<String, Object> output = (Map<String, Object>) body.get("output");
                Map<String, Object> choices = (Map<String, Object>) output.get("choices");
                if (choices != null && choices.containsKey("message")) {
                    Map<String, String> messageResponse = (Map<String, String>) choices.get("message");
                    return messageResponse.get("content");
                }
            }
            
            // 如果解析失败，返回模拟回复
            return simulateResponse(prompt);
            
        } catch (Exception e) {
            log.error("Failed to call LLM API", e);
            return simulateResponse(prompt);
        }
    }
    
    /**
     * 模拟回复（降级方案）
     */
    private String simulateResponse(String prompt) {
        if (prompt.contains("历史对话") || prompt.contains("上下文")) {
            return "基于历史对话，我理解您之前讨论过相关话题。我来继续回答您的问题...";
        }
        return "这是一个模拟回复。实际应用中会调用 LLM API。";
    }
}
