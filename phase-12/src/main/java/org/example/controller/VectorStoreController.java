package org.example.controller;

import org.example.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 向量存储 REST API 控制器
 * 
 * 提供文档的增删改查接口，基于 Elasticsearch 向量存储
 */
@RestController
@RequestMapping("/api/vector-store")
public class VectorStoreController {

    private static final Logger logger = LoggerFactory.getLogger(VectorStoreController.class);

    private final VectorStoreService vectorStoreService;

    public VectorStoreController(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * 添加文档
     * 
     * POST /api/vector-store/documents
     * 
     * 请求体示例:
     * {
     *   "content": "Spring AI rocks!! Spring AI rocks!!",
     *   "metadata": {
     *     "category": "technology",
     *     "author": "admin"
     *   }
     * }
     */
    @PostMapping("/documents")
    public Mono<ResponseEntity<ApiResponse<Void>>> addDocument(@RequestBody AddDocumentRequest request) {
        logger.info("Adding document: {}", request.content().substring(0, Math.min(50, request.content.length())));

        return vectorStoreService.addDocument(request.content(), request.metadata())
                .thenReturn(ResponseEntity.ok(ApiResponse.<Void>success("Document added successfully", null)))
                .onErrorResume(e -> {
                    logger.error("Failed to add document", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.<Void>error("Failed to add document: " + e.getMessage())));
                });
    }

    /**
     * 批量添加文档
     * 
     * POST /api/vector-store/documents/batch
     */
    @PostMapping("/documents/batch")
    public Mono<ResponseEntity<ApiResponse<Void>>> addDocuments(@RequestBody List<AddDocumentRequest> requests) {
        logger.info("Batch adding {} documents", requests.size());

        List<Document> documents = requests.stream()
                .map(req -> new Document(req.content(), req.metadata()))
                .toList();

        return vectorStoreService.addDocuments(documents)
                .thenReturn(ResponseEntity.ok(ApiResponse.<Void>success("Batch documents added successfully", null)))
                .onErrorResume(e -> {
                    logger.error("Failed to batch add documents", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.<Void>error("Failed to batch add documents: " + e.getMessage())));
                });
    }

    /**
     * 相似性搜索
     * 
     * GET /api/vector-store/search?query=Spring&topK=5
     */
    @GetMapping("/search")
    public Mono<ResponseEntity<ApiResponse<List<DocumentResponse>>>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        logger.info("Searching for: {}, topK: {}", query, topK);
        
        return vectorStoreService.similaritySearch(query, topK)
                .map(documents -> {
                    List<DocumentResponse> responses = documents.stream()
                            .map(doc -> new DocumentResponse(
                                    doc.getId(),
                                    doc.getText(),
                                    doc.getMetadata()
                            ))
                            .toList();
                    return ResponseEntity.ok(ApiResponse.success("Search completed", responses));
                })
                .onErrorResume(e -> {
                    logger.error("Search failed", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error("Search failed: " + e.getMessage())));
                });
    }

    /**
     * 带相似度阈值的搜索
     * 
     * GET /api/vector-store/search/threshold?query=Spring&topK=5&threshold=0.8
     */
    @GetMapping("/search/threshold")
    public Mono<ResponseEntity<ApiResponse<List<DocumentResponse>>>> searchWithThreshold(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "0.0") double threshold) {
        logger.info("Searching for: {}, topK: {}, threshold: {}", query, topK, threshold);
        
        return vectorStoreService.similaritySearch(query, topK, threshold)
                .map(documents -> {
                    List<DocumentResponse> responses = documents.stream()
                            .map(doc -> new DocumentResponse(
                                    doc.getId(),
                                    doc.getText(),
                                    doc.getMetadata()
                            ))
                            .toList();
                    return ResponseEntity.ok(ApiResponse.success("Search completed", responses));
                })
                .onErrorResume(e -> {
                    logger.error("Search with threshold failed", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error("Search failed: " + e.getMessage())));
                });
    }

    /**
     * 带过滤条件的搜索
     * 
     * GET /api/vector-store/search/filter?query=Spring&filter=category=='technology'&topK=5
     */
    @GetMapping("/search/filter")
    public Mono<ResponseEntity<ApiResponse<List<DocumentResponse>>>> searchWithFilter(
            @RequestParam String query,
            @RequestParam String filter,
            @RequestParam(defaultValue = "5") int topK) {
        logger.info("Searching for: {} with filter: {}, topK: {}", query, filter, topK);
        
        return vectorStoreService.searchWithFilter(query, filter, topK)
                .map(documents -> {
                    List<DocumentResponse> responses = documents.stream()
                            .map(doc -> new DocumentResponse(
                                    doc.getId(),
                                    doc.getText(),
                                    doc.getMetadata()
                            ))
                            .toList();
                    return ResponseEntity.ok(ApiResponse.success("Filtered search completed", responses));
                })
                .onErrorResume(e -> {
                    logger.error("Filtered search failed", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error("Filtered search failed: " + e.getMessage())));
                });
    }

    /**
     * 删除所有文档
     * 
     * DELETE /api/vector-store/documents
     */
    @DeleteMapping("/documents")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteAll() {
        logger.info("Deleting all documents");

        return vectorStoreService.deleteAll()
                .map(success -> ResponseEntity.ok(ApiResponse.<Void>success("All documents deleted", null)))
                .onErrorResume(e -> {
                    logger.error("Failed to delete all documents", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.<Void>error("Failed to delete documents: " + e.getMessage())));
                });
    }

    /**
     * 根据ID删除文档
     * 
     * DELETE /api/vector-store/documents/ids
     */
    @DeleteMapping("/documents/ids")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteByIds(@RequestBody List<String> ids) {
        logger.info("Deleting documents by IDs: {}", ids);

        return vectorStoreService.deleteByIds(ids)
                .map(success -> ResponseEntity.ok(ApiResponse.<Void>success("Documents deleted", null)))
                .onErrorResume(e -> {
                    logger.error("Failed to delete documents", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.<Void>error("Failed to delete documents: " + e.getMessage())));
                });
    }

    // ==================== 请求/响应记录类 ====================

    public record AddDocumentRequest(String content, Map<String, Object> metadata) {
        public AddDocumentRequest {
            if (metadata == null) {
                metadata = Map.of();
            }
        }
    }

    public record DocumentResponse(String id, String content, Map<String, Object> metadata) {}

    public record ApiResponse<T>(boolean success, String message, T data) {
        public static <T> ApiResponse<T> success(String message, T data) {
            return new ApiResponse<>(true, message, data);
        }
        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null);
        }
    }
}
