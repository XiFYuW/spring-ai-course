package org.example.client.controller;

import org.example.client.service.ProductMcpService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 商品管理控制器
 * 提供 REST API 接口操作商品数据（异步版本）
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductMcpService productMcpService;

    public ProductController(ProductMcpService productMcpService) {
        this.productMcpService = productMcpService;
    }

    /**
     * 创建商品
     * POST /api/products
     */
    @PostMapping
    public Mono<Map<String, Object>> createProduct(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Double price = request.get("price") != null ? Double.valueOf(request.get("price").toString()) : null;
        Integer stock = request.get("stock") != null ? Integer.valueOf(request.get("stock").toString()) : null;
        String category = (String) request.get("category");

        return productMcpService.createProduct(name, description, price, stock, category)
                .map(result -> Map.of(
                        "success", result.startsWith("✅"),
                        "message", result
                ));
    }

    /**
     * 根据ID查询商品
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public Mono<Map<String, Object>> getProductById(@PathVariable Long id) {
        return productMcpService.getProductById(id)
                .map(result -> Map.of(
                        "success", result.startsWith("✅"),
                        "data", result
                ));
    }

    /**
     * 根据商品名称查询商品
     * GET /api/products/name/{name}
     */
    @GetMapping("/name/{name}")
    public Mono<Map<String, Object>> getProductByName(@PathVariable String name) {
        return productMcpService.getProductByName(name)
                .map(result -> Map.of(
                        "success", result.startsWith("✅"),
                        "data", result
                ));
    }

    /**
     * 查询所有商品
     * GET /api/products
     */
    @GetMapping
    public Mono<Map<String, Object>> getAllProducts() {
        return productMcpService.getAllProducts()
                .map(result -> Map.of(
                        "success", !result.startsWith("📭"),
                        "data", result
                ));
    }

    /**
     * 根据状态查询商品
     * GET /api/products/status/{status}
     */
    @GetMapping("/status/{status}")
    public Mono<Map<String, Object>> getProductsByStatus(@PathVariable String status) {
        return productMcpService.getProductsByStatus(status)
                .map(result -> Map.of(
                        "success", !result.startsWith("📭"),
                        "status", status,
                        "data", result
                ));
    }

    /**
     * 根据分类查询商品
     * GET /api/products/category/{category}
     */
    @GetMapping("/category/{category}")
    public Mono<Map<String, Object>> getProductsByCategory(@PathVariable String category) {
        return productMcpService.getProductsByCategory(category)
                .map(result -> Map.of(
                        "success", !result.startsWith("📭"),
                        "category", category,
                        "data", result
                ));
    }

    /**
     * 更新商品
     * PUT /api/products/{id}
     */
    @PutMapping("/{id}")
    public Mono<Map<String, Object>> updateProduct(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Double price = request.get("price") != null ? Double.valueOf(request.get("price").toString()) : null;
        Integer stock = request.get("stock") != null ? Integer.valueOf(request.get("stock").toString()) : null;
        String category = (String) request.get("category");
        String status = (String) request.get("status");

        return productMcpService.updateProduct(id, name, description, price, stock, category, status)
                .map(result -> Map.of(
                        "success", result.startsWith("✅"),
                        "message", result
                ));
    }

    /**
     * 删除商品
     * DELETE /api/products/{id}
     */
    @DeleteMapping("/{id}")
    public Mono<Map<String, Object>> deleteProduct(@PathVariable Long id) {
        return productMcpService.deleteProduct(id)
                .map(result -> Map.of(
                        "success", result.startsWith("✅"),
                        "message", result
                ));
    }

    /**
     * 根据价格范围查询商品
     * GET /api/products/price-range?minPrice=10&maxPrice=100
     */
    @GetMapping("/price-range")
    public Mono<Map<String, Object>> getProductsByPriceRange(
            @RequestParam Double minPrice,
            @RequestParam Double maxPrice) {
        return productMcpService.getProductsByPriceRange(minPrice, maxPrice)
                .map(result -> Map.of(
                        "success", !result.startsWith("📭"),
                        "minPrice", minPrice,
                        "maxPrice", maxPrice,
                        "data", result
                ));
    }

    /**
     * 搜索商品
     * GET /api/products/search?keyword=phone
     */
    @GetMapping("/search")
    public Mono<Map<String, Object>> searchProducts(@RequestParam String keyword) {
        return productMcpService.searchProducts(keyword)
                .map(result -> Map.of(
                        "success", !result.startsWith("📭"),
                        "keyword", keyword,
                        "data", result
                ));
    }

    /**
     * 统计商品总数
     * GET /api/products/count
     */
    @GetMapping("/count")
    public Mono<Map<String, Object>> countProducts() {
        return productMcpService.countProducts()
                .map(result -> Map.of(
                        "success", true,
                        "data", result
                ));
    }

    /**
     * AI 智能商品问答（流式输出）
     * POST /api/products/ask/stream
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askProductAIStream(@RequestBody Map<String, String> request) {
        String question = request.getOrDefault("question", "请介绍一下当前商品情况");
        return productMcpService.askProductAIStream(question);
    }
}
