package com.example.cps.service;

import com.example.cps.entity.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 摘要生成服务
 */
@Service
public class SummaryGenerator {
    
    /**
     * 生成对话摘要
     */
    public String generateSummary(List<Message> messages) {
        return generateSimpleSummary(messages);
    }
    
    /**
     * 生成简单摘要（无需 LLM）
     */
    private String generateSimpleSummary(List<Message> messages) {
        StringBuilder summary = new StringBuilder();
        summary.append("主题: 对话包含 ").append(messages.size()).append(" 条消息\n");
        summary.append("关键信息:\n");
        
        // 提取前3条消息作为关键信息
        int count = Math.min(3, messages.size());
        for (int i = 0; i < count; i++) {
            Message msg = messages.get(i);
            String content = msg.getContent();
            if (content.length() > 100) {
                content = content.substring(0, 100) + "...";
            }
            summary.append("- ").append(msg.getRole()).append(": ")
                   .append(content).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * 格式化消息列表
     */
    private String formatMessages(List<Message> messages) {
        return messages.stream()
            .map(m -> String.format("%s: %s", 
                m.getRole().toString(), 
                m.getContent()))
            .collect(Collectors.joining("\n\n"));
    }
}
