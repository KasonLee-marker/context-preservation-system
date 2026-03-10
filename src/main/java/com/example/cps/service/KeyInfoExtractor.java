package com.example.cps.service;

import com.example.cps.entity.Message;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 关键信息提取服务
 */
@Service
public class KeyInfoExtractor {
    
    @Autowired(required = false)
    private ChatClient chatClient;
    
    private static final String EXTRACTION_PROMPT = """
        从以下对话中提取关键信息，以结构化格式输出：
        
        对话内容：
        {conversation}
        
        请提取以下信息（如无则写"无"）：
        - **实体**: 提到的重要名词、工具、技术、人名等
        - **决策**: 做出的决定或选择
        - **任务**: 需要完成的任务
        - **时间**: 提到的时间节点、截止日期
        - **数值**: 重要的数字、配置参数、代码片段
        - **问题**: 待解决的问题
        
        用简洁的中文输出。
        """;
    
    /**
     * 提取关键信息
     */
    public String extractKeyInfo(List<Message> messages) {
        // 如果没有配置 LLM，使用简单提取
        if (chatClient == null) {
            return extractSimpleKeyInfo(messages);
        }
        
        String conversation = formatMessages(messages);
        
        String prompt = EXTRACTION_PROMPT.replace("{conversation}", conversation);
        
        try {
            return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        } catch (Exception e) {
            // 降级到简单提取
            return extractSimpleKeyInfo(messages);
        }
    }
    
    /**
     * 简单关键信息提取（无需 LLM）
     */
    private String extractSimpleKeyInfo(List<Message> messages) {
        StringBuilder info = new StringBuilder();
        info.append("实体: 对话中的关键内容\n");
        info.append("任务: 无\n");
        info.append("决策: 无\n");
        info.append("消息数: ").append(messages.size()).append("\n");
        return info.toString();
    }
    
    /**
     * 提取重要原文片段
     */
    public String extractImportantText(List<Message> messages) {
        // 提取最近的几条消息作为原文保留
        int keepCount = Math.min(3, messages.size());
        List<Message> recentMessages = messages.subList(
            messages.size() - keepCount, 
            messages.size()
        );
        
        return recentMessages.stream()
            .map(m -> String.format("[%s] %s", 
                m.getRole(), 
                truncate(m.getContent(), 200)))
            .collect(Collectors.joining("\n"));
    }
    
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
    
    private String formatMessages(List<Message> messages) {
        return messages.stream()
            .map(m -> String.format("%s: %s", 
                m.getRole().toString(), 
                m.getContent()))
            .collect(Collectors.joining("\n\n"));
    }
}
