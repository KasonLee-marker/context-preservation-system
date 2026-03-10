package com.example.cps.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    public enum Role {
        USER, ASSISTANT, SYSTEM
    }
    
    private String id;
    private Role role;
    private String content;
    private LocalDateTime timestamp;
    
    public static Message user(String content) {
        return Message.builder()
            .id(java.util.UUID.randomUUID().toString())
            .role(Role.USER)
            .content(content)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public static Message assistant(String content) {
        return Message.builder()
            .id(java.util.UUID.randomUUID().toString())
            .role(Role.ASSISTANT)
            .content(content)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
