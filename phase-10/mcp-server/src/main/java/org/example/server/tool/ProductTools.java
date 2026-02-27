package org.example.server.tool;

import org.example.server.entity.Product;
import org.example.server.repository.ProductRepository;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;

/**
 * MCP 服务器 - 商品数据库操作工具提供者
 * 使用 Spring AI MCP 注解暴露商品增删改查功能（异步版本）
 * 基于 R2DBC + PostgreSQL 响应式数据库
 */
@Component
public class ProductTools {

    private final ProductRepository productRepository;

    public ProductTools(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 创建新商品
     */
    @McpTool(
            name = "createProduct",
            description = "创建新商品，需要商品名称、描述、价格、库存和分类"
    )
    public Mono<String> createProduct(
            @McpToolParam(description = "商品名称，必填", required = true) String name,
            @McpToolParam(description = "商品描述", required = false) String description,
            @McpToolParam(description = "商品价格，必填", required = true) Double price,
            @McpToolParam(description = "商品库存", required = false) Integer stock,
            @McpToolParam(description = "商品分类", required = false) String category) {

        System.out.println("[ProductTools] 开始创建商品: " + name);

        return productRepository.findByName(name)
                .flatMap(existingProduct -> {
                    String errorMsg = "❌ 创建失败：商品 '" + name + "' 已存在";
                    System.out.println("[ProductTools] " + errorMsg);
                    return Mono.just(errorMsg);
                })
                .switchIfEmpty(
                        Mono.defer(() -> {
                            Product newProduct = new Product(name, description, price, stock, category);
                            return productRepository.save(newProduct)
                                    .map(savedProduct -> {
                                        String successMsg = "✅ 商品创建成功: " + name;
                                        System.out.println("[ProductTools] " + successMsg);
                                        return "✅ 商品创建成功！\n" + formatProduct(savedProduct);
                                    });
                        })
                );
    }

    /**
     * 根据ID查询商品
     */
    @McpTool(
            name = "getProductById",
            description = "根据商品ID查询商品信息"
    )
    public Mono<String> getProductById(
            @McpToolParam(description = "商品ID，必填", required = true) Long id) {

        System.out.println("[ProductTools] 查询商品ID: " + id);

        return productRepository.findById(id)
                .map(product -> {
                    String msg = "✅ 查询成功，商品: " + product.getName();
                    System.out.println("[ProductTools] " + msg);
                    return "✅ 查询成功！\n" + formatProduct(product);
                })
                .defaultIfEmpty("❌ 未找到ID为 " + id + " 的商品");
    }

    /**
     * 根据商品名称查询商品
     */
    @McpTool(
            name = "getProductByName",
            description = "根据商品名称查询商品信息"
    )
    public Mono<String> getProductByName(
            @McpToolParam(description = "商品名称，必填", required = true) String name) {

        System.out.println("[ProductTools] 查询商品名称: " + name);

        return productRepository.findByName(name)
                .map(product -> {
                    String msg = "✅ 查询成功，商品: " + product.getName();
                    System.out.println("[ProductTools] " + msg);
                    return "✅ 查询成功！\n" + formatProduct(product);
                })
                .defaultIfEmpty("❌ 未找到商品名称为 '" + name + "' 的商品");
    }

    /**
     * 查询所有商品
     */
    @McpTool(
            name = "getAllProducts",
            description = "查询所有商品列表"
    )
    public Mono<String> getAllProducts() {

        System.out.println("[ProductTools] 查询所有商品");

        return productRepository.findAll()
                .collectList()
                .flatMap(products -> {
                    String msg = "查询完成，共 " + products.size() + " 条记录";
                    System.out.println("[ProductTools] " + msg);

                    if (products.isEmpty()) {
                        return Mono.just("📭 暂无商品数据");
                    }
                    StringBuilder result = new StringBuilder();
                    result.append("📋 商品列表（共 ").append(products.size()).append(" 条）：\n");
                    result.append("=".repeat(80)).append("\n");
                    for (Product product : products) {
                        result.append(formatProduct(product)).append("\n");
                        result.append("-".repeat(80)).append("\n");
                    }
                    return Mono.just(result.toString());
                });
    }

    /**
     * 根据状态查询商品
     */
    @McpTool(
            name = "getProductsByStatus",
            description = "根据状态查询商品列表，如 ACTIVE、INACTIVE、DISABLED"
    )
    public Mono<String> getProductsByStatus(
            @McpToolParam(description = "商品状态：ACTIVE(在售)、INACTIVE(下架)、DISABLED(禁用)", required = true) String status) {

        System.out.println("[ProductTools] 查询状态为 '" + status + "' 的商品");

        return productRepository.findByStatus(status.toUpperCase())
                .collectList()
                .flatMap(products -> {
                    String msg = "状态 '" + status + "' 查询完成，共 " + products.size() + " 条记录";
                    System.out.println("[ProductTools] " + msg);

                    if (products.isEmpty()) {
                        return Mono.just("📭 暂无状态为 '" + status + "' 的商品");
                    }
                    StringBuilder result = new StringBuilder();
                    result.append("📋 状态为 '").append(status).append("' 的商品列表（共 ")
                            .append(products.size()).append(" 条）：\n");
                    result.append("=".repeat(80)).append("\n");
                    for (Product product : products) {
                        result.append(formatProduct(product)).append("\n");
                        result.append("-".repeat(80)).append("\n");
                    }
                    return Mono.just(result.toString());
                });
    }

    /**
     * 根据分类查询商品
     */
    @McpTool(
            name = "getProductsByCategory",
            description = "根据分类查询商品列表"
    )
    public Mono<String> getProductsByCategory(
            @McpToolParam(description = "商品分类，必填", required = true) String category) {

        System.out.println("[ProductTools] 查询分类为 '" + category + "' 的商品");

        return productRepository.findByCategory(category)
                .collectList()
                .flatMap(products -> {
                    String msg = "分类 '" + category + "' 查询完成，共 " + products.size() + " 条记录";
                    System.out.println("[ProductTools] " + msg);

                    if (products.isEmpty()) {
                        return Mono.just("📭 暂无分类为 '" + category + "' 的商品");
                    }
                    StringBuilder result = new StringBuilder();
                    result.append("📋 分类为 '").append(category).append("' 的商品列表（共 ")
                            .append(products.size()).append(" 条）：\n");
                    result.append("=".repeat(80)).append("\n");
                    for (Product product : products) {
                        result.append(formatProduct(product)).append("\n");
                        result.append("-".repeat(80)).append("\n");
                    }
                    return Mono.just(result.toString());
                });
    }

    /**
     * 更新商品信息
     */
    @McpTool(
            name = "updateProduct",
            description = "根据商品ID更新商品信息"
    )
    public Mono<String> updateProduct(
            @McpToolParam(description = "商品ID，必填", required = true) Long id,
            @McpToolParam(description = "新商品名称（不修改传null）", required = false) String name,
            @McpToolParam(description = "新描述（不修改传null）", required = false) String description,
            @McpToolParam(description = "新价格（不修改传null）", required = false) Double price,
            @McpToolParam(description = "新库存（不修改传null）", required = false) Integer stock,
            @McpToolParam(description = "新分类（不修改传null）", required = false) String category,
            @McpToolParam(description = "新状态：ACTIVE、INACTIVE、DISABLED（不修改传null）", required = false) String status) {

        System.out.println("[ProductTools] 开始更新商品ID: " + id);

        return productRepository.findById(id)
                .flatMap(existingProduct -> {
                    if (name != null && !name.isEmpty()) {
                        existingProduct.setName(name);
                    }
                    if (description != null) {
                        existingProduct.setDescription(description);
                    }
                    if (price != null) {
                        existingProduct.setPrice(price);
                    }
                    if (stock != null) {
                        existingProduct.setStock(stock);
                    }
                    if (category != null && !category.isEmpty()) {
                        existingProduct.setCategory(category);
                    }
                    if (status != null && !status.isEmpty()) {
                        existingProduct.setStatus(status.toUpperCase());
                    }
                    existingProduct.setUpdatedAt(java.time.LocalDateTime.now());

                    return productRepository.save(existingProduct)
                            .map(updatedProduct -> {
                                String msg = "✅ 商品更新成功: " + updatedProduct.getName();
                                System.out.println("[ProductTools] " + msg);
                                return "✅ 商品更新成功！\n" + formatProduct(updatedProduct);
                            });
                })
                .defaultIfEmpty("❌ 未找到ID为 " + id + " 的商品，无法更新");
    }

    /**
     * 删除商品
     */
    @McpTool(
            name = "deleteProduct",
            description = "根据商品ID删除商品"
    )
    public Mono<String> deleteProduct(
            @McpToolParam(description = "商品ID，必填", required = true) Long id) {

        System.out.println("[ProductTools] 开始删除商品ID: " + id);

        return productRepository.findById(id)
                .flatMap(existingProduct -> {
                    String productName = existingProduct.getName();
                    return productRepository.deleteById(id)
                            .then(Mono.fromCallable(() -> {
                                String msg = "✅ 商品删除成功: " + productName;
                                System.out.println("[ProductTools] " + msg);
                                return "✅ 商品删除成功！\n已删除商品：" + productName;
                            }));
                })
                .defaultIfEmpty("❌ 未找到ID为 " + id + " 的商品，无法删除");
    }

    /**
     * 根据价格范围查询商品
     */
    @McpTool(
            name = "getProductsByPriceRange",
            description = "根据价格范围查询商品"
    )
    public Mono<String> getProductsByPriceRange(
            @McpToolParam(description = "最低价格", required = true) Double minPrice,
            @McpToolParam(description = "最高价格", required = true) Double maxPrice) {

        System.out.println("[ProductTools] 查询价格范围: " + minPrice + "-" + maxPrice);

        return productRepository.findByPriceRange(minPrice, maxPrice)
                .collectList()
                .flatMap(products -> {
                    String msg = "价格范围查询完成，共 " + products.size() + " 条记录";
                    System.out.println("[ProductTools] " + msg);

                    if (products.isEmpty()) {
                        return Mono.just("📭 价格在 " + minPrice + " 到 " + maxPrice + " 之间的商品不存在");
                    }
                    StringBuilder result = new StringBuilder();
                    result.append("📋 价格在 ").append(minPrice).append("-").append(maxPrice)
                            .append(" 的商品列表（共 ").append(products.size()).append(" 条）：\n");
                    result.append("=".repeat(80)).append("\n");
                    for (Product product : products) {
                        result.append(formatProduct(product)).append("\n");
                        result.append("-".repeat(80)).append("\n");
                    }
                    return Mono.just(result.toString());
                });
    }

    /**
     * 模糊搜索商品
     */
    @McpTool(
            name = "searchProducts",
            description = "根据关键词模糊搜索商品名称"
    )
    public Mono<String> searchProducts(
            @McpToolParam(description = "搜索关键词", required = true) String keyword) {

        System.out.println("[ProductTools] 搜索关键词: " + keyword);

        return productRepository.findByNameContaining(keyword)
                .collectList()
                .flatMap(products -> {
                    String msg = "搜索完成，共 " + products.size() + " 条记录";
                    System.out.println("[ProductTools] " + msg);

                    if (products.isEmpty()) {
                        return Mono.just("📭 未找到包含 '" + keyword + "' 的商品");
                    }
                    StringBuilder result = new StringBuilder();
                    result.append("📋 搜索 '").append(keyword).append("' 的结果（共 ")
                            .append(products.size()).append(" 条）：\n");
                    result.append("=".repeat(80)).append("\n");
                    for (Product product : products) {
                        result.append(formatProduct(product)).append("\n");
                        result.append("-".repeat(80)).append("\n");
                    }
                    return Mono.just(result.toString());
                });
    }

    /**
     * 统计商品总数
     */
    @McpTool(
            name = "countProducts",
            description = "统计系统中的商品总数"
    )
    public Mono<String> countProducts() {

        System.out.println("[ProductTools] 统计商品总数");

        return productRepository.countAll()
                .map(count -> {
                    String msg = "商品总数: " + count;
                    System.out.println("[ProductTools] " + msg);
                    return "📊 系统商品总数：" + count + " 件";
                });
    }

    /**
     * 格式化商品对象为字符串
     */
    private String formatProduct(Product product) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format(
                "📦 商品ID: %d\n" +
                        "   商品名称: %s\n" +
                        "   描述: %s\n" +
                        "   价格: ¥%.2f\n" +
                        "   库存: %d\n" +
                        "   分类: %s\n" +
                        "   状态: %s\n" +
                        "   创建时间: %s\n" +
                        "   更新时间: %s",
                product.getId(),
                product.getName(),
                product.getDescription() != null ? product.getDescription() : "暂无描述",
                product.getPrice() != null ? product.getPrice() : 0.0,
                product.getStock() != null ? product.getStock() : 0,
                product.getCategory() != null ? product.getCategory() : "未分类",
                product.getStatus(),
                product.getCreatedAt() != null ? product.getCreatedAt().format(formatter) : "未知",
                product.getUpdatedAt() != null ? product.getUpdatedAt().format(formatter) : "未知"
        );
    }
}
