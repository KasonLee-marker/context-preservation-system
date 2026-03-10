package com.example.cps.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话片段实体 - 保存到向量数据库
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "conversation_chunks")
public class ConversationChunk {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String sessionId;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(length = 2000)
    private String summary;
    
    @Column(length = 2000)
    private String keyInfo;
    
    @Column(length = 4000)
    private String originalText;
    
    private String topic;
    
    @ElementCollection
    @CollectionTable(name = "chunk_entities", joinColumns = @JoinColumn(name = "chunk_id"))
    @Column(name = "entity")
    private List<String> entities;
    
    private Double importance;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
