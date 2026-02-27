package org.example.client.service;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * MCP 客户端服务 - 商品数据库操作服务调用者
 * 连接 MCP 服务端，调用商品增删改查工具（异步版本）
 */
@Service
public class ProductMcpService {

    private final McpAsyncClient mcpAsyncClient;
    private final ChatClient.Builder chatClientBuilder;

    public ProductMcpService(
            List<McpAsyncClient> mcpAsyncClients,
            ChatClient.Builder chatClientBuilder) {
        this.mcpAsyncClient = mcpAsyncClients.stream()
                .filter(client -> {
                    return client.getClientInfo().name().equals("product-server - product-server");
                })
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到 product-server 客户端"));
        this.chatClientBuilder = chatClientBuilder;
    }

    @PostConstruct
    public void init() {
        if (mcpAsyncClient == null) {
            System.err.println("MCP 客户端未初始化，请检查配置");
            return;
        }
        mcpAsyncClient.listTools()
                .doOnNext(tools -> {
                    System.out.println("MCP 客户端已连接，可用工具：" + tools.tools().stream()
                            .map(McpSchema.Tool::name)
                            .toList());
                })
                .doOnError(e -> {
                    System.err.println("连接 MCP 服务器失败: " + e.getMessage());
                    System.err.println("请确保 MCP 服务器已启动 (http://localhost:8080)");
                })
                .subscribe();
    }

    /**
     * 创建商品
     */
    public Mono<String> createProduct(String name, String description, Double price, Integer stock, String category) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("name", name);
        if (description != null) params.put("description", description);
        if (price != null) params.put("price", price);
        if (stock != null) params.put("stock", stock);
        if (category != null) params.put("category", category);

        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest("createProduct", params)
        ).map(this::extractResult);
    }

    /**
     * 根据ID查询商品
     */
    public Mono<String> getProductById(Long id) {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "getProductById",
                        Map.of("id", id)
                )
        ).map(this::extractResult);
    }

    /**
     * 根据商品名称查询商品
     */
    public Mono<String> getProductByName(String name) {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "getProductByName",
                        Map.of("name", name)
                )
        ).map(this::extractResult);
    }

    /**
     * 查询所有商品
     */
    public Mono<String> getAllProducts() {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "getAllProducts",
                        Map.of()
                )
        ).map(this::extractResult);
    }

    /**
     * 根据状态查询商品
     */
    public Mono<String> getProductsByStatus(String status) {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "getProductsByStatus",
                        Map.of("status", status)
                )
        ).map(this::extractResult);
    }

    /**
     * 根据分类查询商品
     */
    public Mono<String> getProductsByCategory(String category) {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "getProductsByCategory",
                        Map.of("category", category)
                )
        ).map(this::extractResult);
    }

    /**
     * 更新商品
     */
    public Mono<String> updateProduct(Long id, String name, String description, Double price, Integer stock, String category, String status) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("id", id);
        if (name != null) params.put("name", name);
        if (description != null) params.put("description", description);
        if (price != null) params.put("price", price);
        if (stock != null) params.put("stock", stock);
        if (category != null) params.put("category", category);
        if (status != null) params.put("status", status);

        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest("updateProduct", params)
        ).map(this::extractResult);
    }

    /**
     * 删除商品
     */
    public Mono<String> deleteProduct(Long id) {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "deleteProduct",
                        Map.of("id", id)
                )
        ).map(this::extractResult);
    }

    /**
     * 根据价格范围查询商品
     */
    public Mono<String> getProductsByPriceRange(Double minPrice, Double maxPrice) {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "getProductsByPriceRange",
                        Map.of("minPrice", minPrice, "maxPrice", maxPrice)
                )
        ).map(this::extractResult);
    }

    /**
     * 搜索商品
     */
    public Mono<String> searchProducts(String keyword) {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "searchProducts",
                        Map.of("keyword", keyword)
                )
        ).map(this::extractResult);
    }

    /**
     * 统计商品总数
     */
    public Mono<String> countProducts() {
        return mcpAsyncClient.callTool(
                new McpSchema.CallToolRequest(
                        "countProducts",
                        Map.of()
                )
        ).map(this::extractResult);
    }

    /**
     * 使用 AI 智能查询商品信息（流式输出）
     */
    public Flux<String> askProductAIStream(String question) {
        return Mono.zip(
                getAllProducts().defaultIfEmpty("暂无商品数据"),
                countProducts().defaultIfEmpty("0")
        ).flatMapMany(tuple -> {
            String products = tuple.getT1();
            String count = tuple.getT2();

            String prompt = String.format(
                    "你是一个商品管理系统助手。基于以下商品数据，回答用户的问题。\n\n" +
                            "商品统计：%s\n\n商品列表：\n%s\n\n用户问题：%s",
                    count, products, question
            );

            return chatClientBuilder.build()
                    .prompt(prompt)
                    .stream()
                    .content();
        }).onErrorResume(e -> Flux.just("获取商品信息失败: " + e.getMessage()));
    }

    private String extractResult(McpSchema.CallToolResult result) {
        if (result.isError()) {
            return "调用出错：" + result.content();
        }
        return result.content().stream()
                .filter(c -> c instanceof McpSchema.TextContent)
                .map(c -> ((McpSchema.TextContent) c).text())
                .findFirst()
                .orElse("无结果");
    }
}
