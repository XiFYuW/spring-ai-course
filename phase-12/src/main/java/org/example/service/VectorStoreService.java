package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * 向量存储服务类
 * 
 * 提供文档的添加、搜索、删除等操作
 * 基于 Redis 向量存储实现
 */
@Service
public class VectorStoreService {

    private static final Logger logger = LoggerFactory.getLogger(VectorStoreService.class);

    private final VectorStore vectorStore;

    public VectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 添加文档到向量存储
     * 
     * @param content 文档内容
     * @param metadata 文档元数据
     * @return 操作结果
     */
    public Mono<Void> addDocument(String content, Map<String, Object> metadata) {
        return Mono.fromRunnable(() -> {
            Document document = new Document(content, metadata);
            vectorStore.add(List.of(document));
            logger.info("Document added to vector store: {}", content.substring(0, Math.min(50, content.length())));
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * 添加文档到向量存储（无元数据）
     * 
     * @param content 文档内容
     * @return 操作结果
     */
    public Mono<Void> addDocument(String content) {
        return addDocument(content, Map.of());
    }

    /**
     * 批量添加文档
     * 
     * @param documents 文档列表
     * @return 操作结果
     */
    public Mono<Void> addDocuments(List<Document> documents) {
        return Mono.fromRunnable(() -> {
            vectorStore.add(documents);
            logger.info("Batch added {} documents to vector store", documents.size());
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * 相似性搜索
     * 
     * @param query 查询文本
     * @param topK 返回结果数量
     * @return 相似文档列表
     */
    public Mono<List<Document>> similaritySearch(String query, int topK) {
        return Mono.fromCallable(() -> {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .build();
            
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            logger.info("Similarity search for '{}' returned {} results", query, results.size());
            return results;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 相似性搜索（带相似度阈值）
     * 
     * @param query 查询文本
     * @param topK 返回结果数量
     * @param similarityThreshold 相似度阈值（0.0 - 1.0）
     * @return 相似文档列表
     */
    public Mono<List<Document>> similaritySearch(String query, int topK, double similarityThreshold) {
        return Mono.fromCallable(() -> {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build();
            
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            logger.info("Similarity search for '{}' with threshold {} returned {} results", 
                    query, similarityThreshold, results.size());
            return results;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 根据表达式搜索文档
     * 
     * @param query 查询文本
     * @param filterExpression 过滤表达式（如 "meta1 == 'value1'"）
     * @param topK 返回结果数量
     * @return 相似文档列表
     */
    public Mono<List<Document>> searchWithFilter(String query, String filterExpression, int topK) {
        return Mono.fromCallable(() -> {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression(filterExpression)
                    .build();
            
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            logger.info("Filtered search for '{}' with filter '{}' returned {} results", 
                    query, filterExpression, results.size());
            return results;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 删除所有文档
     * 
     * @return 操作结果
     */
    public Mono<Boolean> deleteAll() {
        return Mono.fromCallable(() -> {
            // 使用空列表删除所有文档
            vectorStore.delete(List.of());
            logger.info("All documents deleted from vector store");
            return true;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 根据ID删除文档
     * 
     * @param ids 文档ID列表
     * @return 操作结果
     */
    public Mono<Boolean> deleteByIds(List<String> ids) {
        return Mono.fromCallable(() -> {
            vectorStore.delete(ids);
            logger.info("Deleted {} documents from vector store", ids.size());
            return true;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
