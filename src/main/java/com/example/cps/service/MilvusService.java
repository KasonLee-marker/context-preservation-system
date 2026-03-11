package com.example.cps.service;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Milvus 向量数据库服务
 */
@Service
@Slf4j
public class MilvusService {

    @Autowired
    private MilvusServiceClient milvusClient;

    @Autowired
    private DashScopeEmbeddingService embeddingService;

    @Value("${milvus.collection.name:conversation_chunks}")
    private String collectionName;

    private static final int DIMENSION = 1152;

    /**
     * 创建集合（如果不存在）
     */
    private void createCollectionIfNotExists() {
        try {
            // 检查集合是否存在
            R<Boolean> hasCollection = milvusClient.hasCollection(
                io.milvus.param.collection.HasCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build()
            );

            if (hasCollection.getData() != null && hasCollection.getData()) {
                log.debug("Collection {} already exists", collectionName);
                return;
            }

            // 创建字段
            FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.VarChar)
                .withMaxLength(36)
                .withPrimaryKey(true)
                .withAutoID(false)
                .build();

            FieldType contentField = FieldType.newBuilder()
                .withName("content")
                .withDataType(DataType.VarChar)
                .withMaxLength(65535)
                .build();

            FieldType vectorField = FieldType.newBuilder()
                .withName("vector")
                .withDataType(DataType.FloatVector)
                .withDimension(DIMENSION)
                .build();

            // 创建集合
            CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDescription("Conversation chunks for RAG")
                .addFieldType(idField)
                .addFieldType(contentField)
                .addFieldType(vectorField)
                .build();

            R<io.milvus.param.RpcStatus> response = milvusClient.createCollection(createParam);
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("Collection {} created successfully", collectionName);
                
                // 创建索引（必须先创建索引才能加载集合）
                createIndex();
            } else {
                log.error("Failed to create collection: {}", response.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to initialize collection", e);
        }
    }
    
    /**
     * 创建索引
     */
    private void createIndex() {
        try {
            // 使用简化的索引参数
            io.milvus.param.index.CreateIndexParam indexParam = io.milvus.param.index.CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName("vector")
                .withIndexName("idx_vector")
                .build();
            
            R<io.milvus.param.RpcStatus> indexResponse = milvusClient.createIndex(indexParam);
            if (indexResponse.getStatus() == R.Status.Success.getCode()) {
                log.info("Index created for collection {}", collectionName);
            } else {
                log.error("Failed to create index: {}", indexResponse.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to create index", e);
        }
    }

    /**
     * 插入数据
     */
    public void insert(String id, String content) {
        try {
            // 确保集合存在
            createCollectionIfNotExists();
            
            // 生成 Embedding
            float[] embedding = embeddingService.embed(content);

            // 准备数据
            List<String> ids = Arrays.asList(id);
            List<String> contents = Arrays.asList(content);

            // 转换向量
            List<Float> vectorList = new ArrayList<>();
            for (float v : embedding) {
                vectorList.add(v);
            }
            List<List<Float>> vectors = Arrays.asList(vectorList);

            // 构建插入参数
            List<InsertParam.Field> fields = Arrays.asList(
                new InsertParam.Field("id", ids),
                new InsertParam.Field("content", contents),
                new InsertParam.Field("vector", vectors)
            );

            InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();

            R<?> response = milvusClient.insert(insertParam);
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("Successfully inserted to Milvus: {}", id);
                
                // Flush 数据确保写入磁盘
                milvusClient.flush(
                    io.milvus.param.collection.FlushParam.newBuilder()
                        .withCollectionNames(Arrays.asList(collectionName))
                        .build()
                );
            } else {
                log.error("Failed to insert to Milvus: {}", response.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to insert to Milvus", e);
        }
    }

    /**
     * 搜索相似内容
     */
    public List<String> search(String query, int topK) {
        try {
            // 确保集合存在
            createCollectionIfNotExists();
            
            // 加载集合
            R<?> loadResponse = milvusClient.loadCollection(
                io.milvus.param.collection.LoadCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build()
            );
            if (loadResponse.getStatus() != R.Status.Success.getCode()) {
                log.error("Failed to load collection: {}", loadResponse.getMessage());
                return new ArrayList<>();
            }
            
            // 生成查询向量
            float[] queryEmbedding = embeddingService.embed(query);
            List<Float> vectorList = new ArrayList<>();
            for (float v : queryEmbedding) {
                vectorList.add(v);
            }
            List<List<Float>> searchVectors = Arrays.asList(vectorList);

            SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withVectors(searchVectors)
                .withVectorFieldName("vector")
                .withTopK(topK)
                .withParams("{\"nprobe\":10}")
                .addOutField("content")
                .build();

            R<?> response = milvusClient.search(searchParam);
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("Search failed: {}", response.getMessage());
                return new ArrayList<>();
            }

            log.info("Search completed");
            
            // 解析搜索结果 - 注意：需要使用 response.getData().getResults()
            io.milvus.grpc.SearchResults searchResults = (io.milvus.grpc.SearchResults) response.getData();
            io.milvus.response.SearchResultsWrapper wrapper = new io.milvus.response.SearchResultsWrapper(searchResults.getResults());
            List<String> results = new ArrayList<>();
            
            // 获取第一个查询向量的结果
            List<io.milvus.response.SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);
            if (scores != null) {
                for (io.milvus.response.SearchResultsWrapper.IDScore score : scores) {
                    // 获取 content 字段 - 通过 IDScore.get() 方法
                    Object contentObj = score.get("content");
                    if (contentObj != null) {
                        String content = contentObj.toString();
                        if (!content.isEmpty()) {
                            results.add(content);
                            log.debug("Found result: {} (score: {})", content.substring(0, Math.min(50, content.length())), score.getScore());
                        }
                    }
                }
            }
            
            log.info("Search returned {} results", results.size());
            return results;
        } catch (Exception e) {
            log.error("Failed to search Milvus", e);
            return new ArrayList<>();
        }
    }
}
