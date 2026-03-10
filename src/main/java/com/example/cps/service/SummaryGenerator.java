package com.example.cps.service;

import com.example.cps.entity.Message;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 摘要生成服务
 */
@Service
public class SummaryGenerator {
    
    @Autowired(required = false)
    private ChatClient chatClient;
    
    private static final String SUMMARY_PROMPT = """
        请对以下对话生成简洁的结构化摘要，包含：
        1. **主题**: 一句话概括讨论主题
        2. **关键决策**: 列出做出的重要决定（如有）
        3. **待办事项**: 列出分配的任务和截止时间（如有）
        4. **关键信息**: 记录重要的数据、配置、代码等
        
        对话内容：
        {conversation}
        
        请用中文输出，保持简洁。
        """;
    
    /**
     * 生成对话摘要
     */
    public String generateSummary(List<Message> messages) {
        // 如果没有配置 LLM，使用简单摘要
        if (chatClient == null) {
            return generateSimpleSummary(messages);
        }
        
        String conversation = formatMessages(messages);
        
        String prompt = SUMMARY_PROMPT.replace("{conversation}", conversation);
        
        try {
            return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        } catch (Exception e) {
            // 降级到简单摘要
            return generateSimpleSummary(messages);
        }
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
