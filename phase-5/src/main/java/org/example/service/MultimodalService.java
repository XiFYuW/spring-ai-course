package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * 多模态 AI 服务
 * 
 * 演示 Spring AI 多模态功能，支持同时处理文本和图像输入。
 * 多模态大型语言模型（LLM）能够结合图像、文本等多种模态信息进行处理和生成响应。
 * 
 * 支持的模型示例：
 * - OpenAI: GPT-4o, GPT-4 Vision
 * - Anthropic: Claude 3 (Opus, Sonnet, Haiku)
 * - Google: Gemini 1.5 Pro/Flash
 * - 开源: LLaVA, BakLLaVA, Llama 3.2
 * 
 * 参考文档：https://docs.springframework.org.cn/spring-ai/reference/api/multimodality.html
 * 
 * @author Spring AI Course
 */
@Service
public class MultimodalService {

    private static final Logger logger = LoggerFactory.getLogger(MultimodalService.class);

    private final ChatClient chatClient;

    public MultimodalService(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * 分析单张图片内容
     * 
     * 使用多模态模型分析图片并生成文本描述。
     * 这是最基本的图像理解功能，可以识别图片中的物体、场景、文字等。
     * 
     * 实现原理：
     * 1. 使用 UserMessage 的 media 字段传递图像数据
     * 2. MimeType 指定图像格式（如 IMAGE_PNG, IMAGE_JPEG）
     * 3. Media 数据可以是 Resource 对象（本地文件/类路径资源）或 URI
     * 
     * @param imageResource 图片资源（支持 ClassPathResource, FileSystemResource, UrlResource 等）
     * @param question 关于图片的问题（可选，默认为描述图片内容）
     * @return AI 对图片的分析结果
     */
    public Mono<String> analyzeImage(Resource imageResource, String question) {
        return Mono.fromCallable(() -> {
            logger.info("开始分析图片，问题: {}", question);

            // 使用 ChatClient 构建多模态请求
            String response = chatClient.prompt()
                    .user(userSpec -> userSpec
                            // 设置文本提示
                            .text(question != null ? question : "请详细描述这张图片中的内容，包括主要物体、场景、颜色、氛围等。")
                            // 添加媒体（图片）
                            .media(MimeTypeUtils.IMAGE_PNG, imageResource))
                    .call()
                    .content();

            logger.info("图片分析完成");
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 分析图片内容（使用默认问题）
     * 
     * @param imageResource 图片资源
     * @return AI 对图片的描述
     */
    public Mono<String> analyzeImage(Resource imageResource) {
        return analyzeImage(imageResource, null);
    }

    /**
     * 对比多张图片
     * 
     * 多模态模型可以同时接收多张图片输入，进行对比分析。
     * 适用于找出差异、相似之处、变化趋势等场景。
     * 
     * @param imageResources 图片资源列表
     * @param comparisonPrompt 对比分析提示词
     * @return 对比分析结果
     */
    public Mono<String> compareImages(List<Resource> imageResources, String comparisonPrompt) {
        return Mono.fromCallable(() -> {
            logger.info("开始对比 {} 张图片", imageResources.size());

            // 构建多模态请求，添加多张图片
            // 使用 Consumer 方式设置用户消息
            String response = chatClient.prompt()
                    .user(userSpec -> {
                        // 设置对比提示词
                        userSpec.text(comparisonPrompt != null ? comparisonPrompt 
                                : "请对比分析这些图片，找出它们的相似之处和差异。");
                        
                        // 添加所有图片到 media
                        for (Resource imageResource : imageResources) {
                            userSpec.media(MimeTypeUtils.IMAGE_PNG, imageResource);
                        }
                    })
                    .call()
                    .content();

            logger.info("图片对比完成");
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 从图片中提取结构化信息
     * 
     * 结合多模态能力和结构化输出，从图片中提取特定格式的信息。
     * 例如：从发票图片提取金额、日期、商家等信息。
     * 
     * @param imageResource 图片资源
     * @param extractionPrompt 信息提取提示词
     * @param outputFormat 期望的输出格式说明（如 JSON 结构）
     * @return 结构化提取结果
     */
    public Mono<String> extractStructuredInfo(Resource imageResource, 
                                               String extractionPrompt,
                                               String outputFormat) {
        return Mono.fromCallable(() -> {
            logger.info("开始从图片提取结构化信息");

            String fullPrompt = String.format("""
                    %s
                    
                    请以以下格式输出结果：
                    %s
                    """, 
                    extractionPrompt != null ? extractionPrompt : "请分析这张图片并提取关键信息。",
                    outputFormat != null ? outputFormat : """
                    {
                        "标题": "...",
                        "主要内容": "...",
                        "关键元素": ["...", "..."],
                        "颜色": "...",
                        "风格": "..."
                    }
                    """
            );

            String response = chatClient.prompt()
                    .user(userSpec -> userSpec
                            .text(fullPrompt)
                            .media(MimeTypeUtils.IMAGE_PNG, imageResource))
                    .call()
                    .content();

            logger.info("结构化信息提取完成");
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 图像问答（Visual Question Answering）
     * 
     * 针对图片内容回答特定问题。
     * 这是多模态模型的典型应用场景，需要模型理解图像内容并回答相关问题。
     * 
     * @param imageResource 图片资源
     * @param question 关于图片的具体问题
     * @return 问题的答案
     */
    public Mono<String> visualQuestionAnswering(Resource imageResource, String question) {
        return Mono.fromCallable(() -> {
            logger.info("视觉问答，问题: {}", question);

            String response = chatClient.prompt()
                    .user(userSpec -> userSpec
                            .text(question)
                            .media(MimeTypeUtils.IMAGE_PNG, imageResource))
                    .call()
                    .content();

            logger.info("视觉问答完成");
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 分析图片中的文字（OCR + 理解）
     * 
     * 多模态模型可以识别图片中的文字内容并进行理解分析。
     * 这比传统 OCR 更强大，因为模型可以理解文字上下文和含义。
     * 
     * @param imageResource 包含文字的图片资源
     * @param analysisType 分析类型（如 "extract" 提取文字，"summarize" 总结内容，"translate" 翻译）
     * @return 文字分析结果
     */
    public Mono<String> analyzeImageText(Resource imageResource, String analysisType) {
        return Mono.fromCallable(() -> {
            logger.info("分析图片中的文字，类型: {}", analysisType);

            String prompt = switch (analysisType != null ? analysisType.toLowerCase() : "extract") {
                case "summarize" -> "请阅读图片中的文字内容，并提供简洁的摘要。";
                case "translate" -> "请将图片中的文字翻译成中文。";
                case "analyze" -> "请分析图片中的文字内容，解释其含义和背景。";
                default -> "请提取图片中的所有文字内容，保持原有格式。";
            };

            String response = chatClient.prompt()
                    .user(userSpec -> userSpec
                            .text(prompt)
                            .media(MimeTypeUtils.IMAGE_PNG, imageResource))
                    .call()
                    .content();

            logger.info("图片文字分析完成");
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 生成图片的创意描述或故事
     * 
     * 基于图片内容生成创意文本，如故事、诗歌、营销文案等。
     * 
     * @param imageResource 图片资源
     * @param creativeStyle 创意风格（如 "story" 故事，"poem" 诗歌，"marketing" 营销文案）
     * @return 创意内容
     */
    public Mono<String> creativeDescription(Resource imageResource, String creativeStyle) {
        return Mono.fromCallable(() -> {
            logger.info("生成创意描述，风格: {}", creativeStyle);

            String prompt = switch (creativeStyle != null ? creativeStyle.toLowerCase() : "story") {
                case "poem" -> "请根据这张图片创作一首优美的诗歌。";
                case "marketing" -> "请为这张图片中的产品/场景撰写一段吸引人的营销文案。";
                case "social" -> "请为这张图片写一段适合社交媒体发布的配文，包含相关话题标签。";
                case "story" -> "请根据这张图片创作一个有趣的小故事。";
                default -> "请根据这张图片创作一段优美的描述性文字。";
            };

            String response = chatClient.prompt()
                    .user(userSpec -> userSpec
                            .text(prompt)
                            .media(MimeTypeUtils.IMAGE_PNG, imageResource))
                    .call()
                    .content();

            logger.info("创意描述生成完成");
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
