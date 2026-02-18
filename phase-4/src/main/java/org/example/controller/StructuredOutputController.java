package org.example.controller;

import org.example.entity.MovieActor;
import org.example.entity.ProductInfo;
import org.example.service.StructuredOutputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 结构化输出转换器 REST 控制器
 * 
 * 提供 API 端点演示 Spring AI 结构化输出转换器的三种实现：
 * 1. BeanOutputConverter - 将 AI 输出转换为 Java Bean
 * 2. MapOutputConverter - 将 AI 输出转换为 Map 结构
 * 3. ListOutputConverter - 将 AI 输出转换为 List 结构
 * 
 * 参考文档：https://docs.springframework.org.cn/spring-ai/reference/api/structured-output-converter.html
 * 
 * @author Spring AI Course
 */
@RestController
@RequestMapping("/api/structured")
public class StructuredOutputController {

    private static final Logger logger = LoggerFactory.getLogger(StructuredOutputController.class);

    private final StructuredOutputService structuredOutputService;

    public StructuredOutputController(StructuredOutputService structuredOutputService) {
        this.structuredOutputService = structuredOutputService;
    }

    // ==================== BeanOutputConverter API ====================

    /**
     * 获取演员电影信息 - BeanOutputConverter 示例
     * 
     * 将 AI 的 JSON 响应自动转换为 MovieActor Java 对象
     * 
     * @param actorName 演员名称
     * @return 包含演员电影信息的 MovieActor 对象
     */
    @GetMapping("/actor")
    public Mono<ResponseEntity<MovieActor>> getActorMovies(@RequestParam String actorName) {
        logger.info("获取演员信息: {}", actorName);
        return structuredOutputService.getActorMovies(actorName)
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> logger.info("成功获取演员信息: {}", result.getBody()))
                .doOnError(error -> logger.error("获取演员信息失败: {}", error.getMessage()));
    }

    /**
     * 生成产品信息 - BeanOutputConverter 示例
     * 
     * 根据产品描述生成完整的产品信息对象
     * 
     * @param request 包含产品描述的请求
     * @return 包含产品详细信息的 ProductInfo 对象
     */
    @PostMapping("/product")
    public Mono<ResponseEntity<ProductInfo>> generateProductInfo(
            @RequestBody ProductDescriptionRequest request) {
        logger.info("生成产品信息，描述: {}", request.description());
        return structuredOutputService.generateProductInfo(request.description())
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> logger.info("成功生成产品信息: {}", result.getBody()))
                .doOnError(error -> logger.error("生成产品信息失败: {}", error.getMessage()));
    }

    // ==================== MapOutputConverter API ====================

    /**
     * 分析主题 - MapOutputConverter 示例
     * 
     * 返回灵活的键值对数据结构
     * 
     * @param topic 主题
     * @return 包含主题相关信息的 Map
     */
    @GetMapping("/topic")
    public Mono<ResponseEntity<Map<String, Object>>> analyzeTopic(@RequestParam String topic) {
        logger.info("分析主题: {}", topic);
        return structuredOutputService.analyzeTopic(topic)
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> logger.info("成功分析主题: {}", result.getBody()))
                .doOnError(error -> logger.error("分析主题失败: {}", error.getMessage()));
    }

    /**
     * 对比两个产品 - MapOutputConverter 示例
     * 
     * @param product1 产品1名称
     * @param product2 产品2名称
     * @return 包含对比结果的 Map
     */
    @GetMapping("/compare")
    public Mono<ResponseEntity<Map<String, Object>>> compareProducts(
            @RequestParam String product1,
            @RequestParam String product2) {
        logger.info("对比产品: {} vs {}", product1, product2);
        return structuredOutputService.compareProducts(product1, product2)
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> logger.info("成功对比产品: {}", result.getBody()))
                .doOnError(error -> logger.error("对比产品失败: {}", error.getMessage()));
    }

    // ==================== ListOutputConverter API ====================

    /**
     * 获取建议列表 - ListOutputConverter 示例
     * 
     * 返回逗号分隔的列表数据
     * 
     * @param category 类别
     * @param count 数量（可选，默认5）
     * @return 建议列表
     */
    @GetMapping("/suggestions")
    public Mono<ResponseEntity<List<String>>> getSuggestions(
            @RequestParam String category,
            @RequestParam(defaultValue = "5") int count) {
        logger.info("获取建议列表，类别: {}, 数量: {}", category, count);
        return structuredOutputService.getSuggestions(category, count)
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> logger.info("成功获取建议列表: {}", result.getBody()))
                .doOnError(error -> logger.error("获取建议列表失败: {}", error.getMessage()));
    }

    /**
     * 提取关键词 - ListOutputConverter 示例
     * 
     * @param request 包含文本和关键词数量的请求
     * @return 关键词列表
     */
    @PostMapping("/keywords")
    public Mono<ResponseEntity<List<String>>> extractKeywords(
            @RequestBody KeywordExtractionRequest request) {
        logger.info("提取关键词，文本长度: {}, 数量: {}", 
                request.text().length(), request.count());
        return structuredOutputService.extractKeywords(request.text(), request.count())
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> logger.info("成功提取关键词: {}", result.getBody()))
                .doOnError(error -> logger.error("提取关键词失败: {}", error.getMessage()));
    }

    /**
     * 获取任务步骤 - ListOutputConverter 示例
     * 
     * @param task 任务描述
     * @return 步骤列表
     */
    @GetMapping("/steps")
    public Mono<ResponseEntity<List<String>>> getTaskSteps(@RequestParam String task) {
        logger.info("获取任务步骤: {}", task);
        return structuredOutputService.getTaskSteps(task)
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> logger.info("成功获取任务步骤: {}", result.getBody()))
                .doOnError(error -> logger.error("获取任务步骤失败: {}", error.getMessage()));
    }

    // ==================== 请求/响应记录 ====================

    /**
     * 产品描述请求
     */
    public record ProductDescriptionRequest(String description) {
    }

    /**
     * 关键词提取请求
     */
    public record KeywordExtractionRequest(String text, int count) {
    }
}
