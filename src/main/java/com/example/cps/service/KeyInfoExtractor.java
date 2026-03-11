package com.example.cps.service;

import com.example.cps.entity.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 关键信息提取服务
 */
@Service
public class KeyInfoExtractor {
    
    /**
     * 提取关键信息
     */
    public String extractKeyInfo(List<Message> messages) {
        return extractSimpleKeyInfo(messages);
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
