package org.example.service;

import org.example.entity.MovieActor;
import org.example.entity.ProductInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * 结构化输出转换器服务
 * 
 * 演示 Spring AI 结构化输出转换器的三种主要实现：
 * 1. BeanOutputConverter - 将 AI 输出转换为 Java Bean
 * 2. MapOutputConverter - 将 AI 输出转换为 Map 结构
 * 3. ListOutputConverter - 将 AI 输出转换为 List 结构
 * 
 * 参考文档：https://docs.springframework.org.cn/spring-ai/reference/api/structured-output-converter.html
 * 
 * @author Spring AI Course
 */
@Service
public class StructuredOutputService {

    private static final Logger logger = LoggerFactory.getLogger(StructuredOutputService.class);

    private final ChatClient chatClient;

    public StructuredOutputService(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    // ==================== BeanOutputConverter 示例 ====================

    /**
     * 使用 BeanOutputConverter 获取演员电影信息
     * 
     * BeanOutputConverter 会将 AI 的 JSON 响应自动转换为指定的 Java 类实例
     * 它使用 FormatProvider 指示 AI 模型生成符合 JSON Schema 的响应
     * 
     * @param actorName 演员名称
     * @return 包含演员信息的 MovieActor 对象
     */
    public Mono<MovieActor> getActorMovies(String actorName) {
        return Mono.fromCallable(() -> {
            // 创建 BeanOutputConverter，指定目标类型为 MovieActor
            BeanOutputConverter<MovieActor> converter = new BeanOutputConverter<>(MovieActor.class);

            // 获取格式化指令，这会生成基于 MovieActor 类的 JSON Schema
            String format = converter.getFormat();
            logger.debug("生成的格式指令: {}", format);

            // 构建提示词模板，包含用户输入和格式占位符
            String userPrompt = """
                    为演员 {actor} 生成电影作品信息。
                    包含该演员最著名的5部电影和获得的3个重要奖项。
                    {format}
                    """;

            // 使用 PromptTemplate 构建提示词，替换变量和格式指令
            PromptTemplate promptTemplate = PromptTemplate.builder()
                    .template(userPrompt)
                    .variables(Map.of("actor", actorName, "format", format))
                    .build();

            Prompt prompt = new Prompt(promptTemplate.createMessage());

            // 调用 AI 模型
            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            logger.debug("AI 原始响应: {}", response);

            // 使用转换器将 JSON 响应转换为 MovieActor 对象
            return converter.convert(response);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 使用 BeanOutputConverter 获取产品信息
     * 
     * @param productDescription 产品描述
     * @return 包含产品详细信息的 ProductInfo 对象
     */
    public Mono<ProductInfo> generateProductInfo(String productDescription) {
        return Mono.fromCallable(() -> {
            // 创建 BeanOutputConverter，指定目标类型为 ProductInfo
            BeanOutputConverter<ProductInfo> converter = new BeanOutputConverter<>(ProductInfo.class);

            String format = converter.getFormat();

            String userPrompt = """
                    根据以下产品描述生成完整的产品信息：
                    {description}
                    
                    请生成一个合理的产品名称、详细描述、价格、类别和库存数量。
                    {format}
                    """;

            PromptTemplate promptTemplate = PromptTemplate.builder()
                    .template(userPrompt)
                    .variables(Map.of("description", productDescription, "format", format))
                    .build();

            Prompt prompt = new Prompt(promptTemplate.createMessage());

            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            return converter.convert(response);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ==================== MapOutputConverter 示例 ====================

    /**
     * 使用 MapOutputConverter 获取灵活的键值对数据
     * 
     * MapOutputConverter 会将 AI 的 JSON 响应转换为 Map<String, Object>
     * 适用于结构不固定或需要动态解析的场景
     * 
     * @param topic 主题
     * @return 包含主题相关信息的 Map
     */
    public Mono<Map<String, Object>> analyzeTopic(String topic) {
        return Mono.fromCallable(() -> {
            // 创建 MapOutputConverter
            MapOutputConverter converter = new MapOutputConverter();

            String format = converter.getFormat();
            logger.debug("Map 格式指令: {}", format);

            String userPrompt = """
                    分析以下主题，并以键值对形式返回相关信息：
                    主题：{topic}
                    
                    请返回以下信息（JSON格式）：
                    - 定义（definition）
                    - 重要性（importance）
                    - 相关概念（relatedConcepts，数组形式）
                    - 应用场景（applications，数组形式）
                    
                    {format}
                    """;

            PromptTemplate promptTemplate = PromptTemplate.builder()
                    .template(userPrompt)
                    .variables(Map.of("topic", topic, "format", format))
                    .build();

            Prompt prompt = new Prompt(promptTemplate.createMessage());

            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            logger.debug("AI 原始响应: {}", response);

            // 转换为 Map
            return converter.convert(response);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 使用 MapOutputConverter 获取产品对比信息
     * 
     * @param product1 产品1名称
     * @param product2 产品2名称
     * @return 包含对比结果的 Map
     */
    public Mono<Map<String, Object>> compareProducts(String product1, String product2) {
        return Mono.fromCallable(() -> {
            MapOutputConverter converter = new MapOutputConverter();
            String format = converter.getFormat();

            String userPrompt = """
                    对比以下两个产品，返回对比结果：
                    产品1：{product1}
                    产品2：{product2}
                    
                    请包含以下对比维度（JSON格式）：
                    - 价格对比（priceComparison）
                    - 功能对比（featureComparison）
                    - 优缺点（prosAndCons，对象形式）
                    - 推荐场景（recommendedScenarios，数组形式）
                    
                    {format}
                    """;

            PromptTemplate promptTemplate = PromptTemplate.builder()
                    .template(userPrompt)
                    .variables(Map.of("product1", product1, "product2", product2, "format", format))
                    .build();

            Prompt prompt = new Prompt(promptTemplate.createMessage());

            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            return converter.convert(response);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ==================== ListOutputConverter 示例 ====================

    /**
     * 使用 ListOutputConverter 获取列表数据
     * 
     * ListOutputConverter 会将 AI 的逗号分隔响应转换为 List
     * 适用于需要获取列表项的场景
     * 
     * @param category 类别
     * @param count 数量
     * @return 包含列表项的 List
     */
    public Mono<List<String>> getSuggestions(String category, int count) {
        return Mono.fromCallable(() -> {
            // 创建 ListOutputConverter，使用默认的 ConversionService
            ListOutputConverter converter = new ListOutputConverter(new DefaultConversionService());

            String format = converter.getFormat();
            logger.debug("List 格式指令: {}", format);

            String userPrompt = """
                    列出 {count} 个关于 {category} 的建议。
                    请以逗号分隔的列表形式返回。
                    {format}
                    """;

            PromptTemplate promptTemplate = PromptTemplate.builder()
                    .template(userPrompt)
                    .variables(Map.of("count", String.valueOf(count), "category", category, "format", format))
                    .build();

            Prompt prompt = new Prompt(promptTemplate.createMessage());

            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            logger.debug("AI 原始响应: {}", response);

            // 转换为 List
            return converter.convert(response);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 使用 ListOutputConverter 获取关键词列表
     * 
     * @param text 文本内容
     * @param keywordCount 关键词数量
     * @return 关键词列表
     */
    public Mono<List<String>> extractKeywords(String text, int keywordCount) {
        return Mono.fromCallable(() -> {
            ListOutputConverter converter = new ListOutputConverter(new DefaultConversionService());
            String format = converter.getFormat();

            String userPrompt = """
                    从以下文本中提取 {count} 个最重要的关键词：
                    
                    文本：{text}
                    
                    请以逗号分隔的列表形式返回这些关键词。
                    {format}
                    """;

            PromptTemplate promptTemplate = PromptTemplate.builder()
                    .template(userPrompt)
                    .variables(Map.of("count", String.valueOf(keywordCount), "text", text, "format", format))
                    .build();

            Prompt prompt = new Prompt(promptTemplate.createMessage());

            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            return converter.convert(response);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 使用 ListOutputConverter 获取步骤列表
     * 
     * @param task 任务描述
     * @return 步骤列表
     */
    public Mono<List<String>> getTaskSteps(String task) {
        return Mono.fromCallable(() -> {
            ListOutputConverter converter = new ListOutputConverter(new DefaultConversionService());
            String format = converter.getFormat();

            String userPrompt = """
                    为完成以下任务提供详细的步骤列表：
                    任务：{task}
                    
                    请以逗号分隔的列表形式返回每个步骤（每个步骤应该简洁明了）。
                    {format}
                    """;

            PromptTemplate promptTemplate = PromptTemplate.builder()
                    .template(userPrompt)
                    .variables(Map.of("task", task, "format", format))
                    .build();

            Prompt prompt = new Prompt(promptTemplate.createMessage());

            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            return converter.convert(response);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
