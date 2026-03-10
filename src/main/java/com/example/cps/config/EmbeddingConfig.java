package com.example.cps.config;

import com.example.cps.service.DashScopeEmbeddingService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Embedding 配置类
 * 使用阿里云灵积作为 Embedding 服务
 */
@Configuration
public class EmbeddingConfig {
    
    /**
     * 自定义 EmbeddingModel，使用阿里云灵积
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel(DashScopeEmbeddingService dashScopeEmbeddingService) {
        return new DashScopeEmbeddingModel(dashScopeEmbeddingService);
    }
}
