package com.example.cps.service;

import com.example.cps.entity.Message;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Token 估算服务
 */
@Service
public class TokenEstimator {
    
    // 粗略估算：英文约 4 字符/token，中文约 1 字符/token
    private static final double ENGLISH_CHARS_PER_TOKEN = 4.0;
    private static final double CHINESE_CHARS_PER_TOKEN = 1.0;
    
    /**
     * 估算消息列表的 Token 数
     */
    public int estimateTokens(List<Message> messages) {
        int totalTokens = 0;
        
        for (Message message : messages) {
            totalTokens += estimateTokens(message.getContent());
            // 每条消息额外开销（角色标识等）
            totalTokens += 4;
        }
        
        return totalTokens;
    }
    
    /**
     * 估算单条文本的 Token 数
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        int chineseCount = 0;
        int englishCount = 0;
        
        for (char c : text.toCharArray()) {
            if (isChinese(c)) {
                chineseCount++;
            } else {
                englishCount++;
            }
        }
        
        double tokens = chineseCount / CHINESE_CHARS_PER_TOKEN 
            + englishCount / ENGLISH_CHARS_PER_TOKEN;
        
        return (int) Math.ceil(tokens);
    }
    
    private boolean isChinese(char c) {
        return Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A;
    }
}
