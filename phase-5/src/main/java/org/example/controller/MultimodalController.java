package org.example.controller;

import org.example.service.MultimodalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 多模态 AI REST 控制器
 * 
 * 提供 API 端点演示 Spring AI 多模态功能：
 * 1. 单张图片分析 - 描述图片内容
 * 2. 视觉问答 - 针对图片回答特定问题
 * 3. 图片对比 - 对比多张图片的异同
 * 4. 结构化信息提取 - 从图片提取结构化数据
 * 5. 图片文字分析 - OCR + 理解
 * 6. 创意描述生成 - 基于图片生成故事/诗歌等
 * 
 * 参考文档：https://docs.springframework.org.cn/spring-ai/reference/api/multimodality.html
 * 
 * @author Spring AI Course
 */
@RestController
@RequestMapping("/api/multimodal")
public class MultimodalController {

    private static final Logger logger = LoggerFactory.getLogger(MultimodalController.class);

    private final MultimodalService multimodalService;

    public MultimodalController(MultimodalService multimodalService) {
        this.multimodalService = multimodalService;
    }

    // ==================== 基础图片分析 API ====================

    /**
     * 分析单张图片内容
     *
     * 上传图片文件，AI 将详细描述图片中的内容。
     * 支持 PNG、JPEG 等常见图片格式。
     *
     * 示例请求（使用 curl）：
     * curl -X POST http://localhost:8080/api/multimodal/analyze \
     *   -F "image=@/path/to/your/image.png" \
     *   -F "question=这张图片里有什么？"
     *
     * 如果不提供 question 参数，将使用默认提示词："请详细描述这张图片的内容"
     *
     * @param image 上传的图片文件
     * @param question 关于图片的问题（必填）
     * @return 图片分析结果
     */
    @PostMapping("/analyze")
    public Mono<ResponseEntity<String>> analyzeImage(
            @RequestPart("image") FilePart image,
            @RequestPart("question") String question) {

        logger.info("收到图片分析请求，文件名: {}, 问题: {}", image.filename(), question);

        return saveFilePartToTemp(image)
                .flatMap(tempPath -> {
                    Resource imageResource = new FileSystemResource(tempPath.toFile());
                    return multimodalService.analyzeImage(imageResource, question)
                            .doFinally(signal -> cleanupTempFile(tempPath));
                })
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> logger.info("图片分析成功"))
                .doOnError(error -> logger.error("图片分析失败: {}", error.getMessage()));
    }

    /**
     * 使用类路径图片进行分析（测试用）
     * 
     * 使用项目中预置的图片进行测试。
     * 需要在 resources 目录下放置测试图片。
     * 
     * @param imageName 图片文件名（位于 resources/images/ 目录下）
     * @param question 关于图片的问题（可选）
     * @return 图片分析结果
     */
    @GetMapping("/analyze/classpath")
    public Mono<ResponseEntity<String>> analyzeClassPathImage(
            @RequestParam("imageName") String imageName,
            @RequestParam(value = "question", required = false) String question) {
        
        logger.info("使用类路径图片进行分析: {}", imageName);

        return Mono.fromCallable(() -> {
            String path = "images/" + imageName;
            Resource imageResource = new ClassPathResource(path);
            
            if (!imageResource.exists()) {
                throw new IllegalArgumentException("图片不存在: " + path);
            }
            
            return imageResource;
        })
        .flatMap(imageResource -> multimodalService.analyzeImage(imageResource, question))
        .map(ResponseEntity::ok)
        .doOnSuccess(result -> logger.info("类路径图片分析成功"))
        .doOnError(error -> logger.error("类路径图片分析失败: {}", error.getMessage()));
    }

    // ==================== 视觉问答 API ====================

    /**
     * 视觉问答（Visual Question Answering）
     * 
     * 针对上传的图片回答特定问题。
     * 模型需要理解图片内容并给出准确答案。
     * 
     * 示例请求：
     * curl -X POST http://localhost:8080/api/multimodal/vqa \
     *   -F "image=@/path/to/image.png" \
     *   -F "question=图中有几个人？"
     * 
     * @param image 上传的图片文件
     * @param question 关于图片的具体问题
     * @return 问题的答案
     */
    @PostMapping("/vqa")
    public Mono<ResponseEntity<String>> visualQuestionAnswering(
            @RequestPart("image") FilePart image,
            @RequestPart("question") String question) {
        
        logger.info("收到视觉问答请求，问题: {}", question);

        return saveFilePartToTemp(image)
                .flatMap(tempPath -> {
                    Resource imageResource = new FileSystemResource(tempPath.toFile());
                    return multimodalService.visualQuestionAnswering(imageResource, question)
                            .doFinally(signal -> cleanupTempFile(tempPath));
                })
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> logger.info("视觉问答成功"))
                .doOnError(error -> logger.error("视觉问答失败: {}", error.getMessage()));
    }

    // ==================== 图片对比 API ====================

    /**
     * 对比多张图片
     * 
     * 上传多张图片，AI 将分析它们的相似之处和差异。
     * 适用于找不同、版本对比、变化检测等场景。
     * 
     * 示例请求：
     * curl -X POST http://localhost:8080/api/multimodal/compare \
     *   -F "images=@/path/to/image1.png" \
     *   -F "images=@/path/to/image2.png" \
     *   -F "prompt=对比这两张图片的差异"
     * 
     * @param images 上传的多张图片文件
     * @param prompt 对比分析提示词（可选）
     * @return 对比分析结果
     */
    @PostMapping("/compare")
    public Mono<ResponseEntity<String>> compareImages(
            @RequestPart("images") List<FilePart> images,
            @RequestPart("prompt") String prompt) {
        
        logger.info("收到图片对比请求，图片数量: {}", images.size());

        if (images.size() < 2) {
            return Mono.just(ResponseEntity.badRequest()
                    .body("请至少上传两张图片进行对比"));
        }

        return Mono.fromCallable(() -> images.stream()
                .map(this::saveFilePartToTemp)
                .toList())
                .flatMap(tempPathMonos -> 
                    Mono.zip(tempPathMonos, objects -> 
                        java.util.Arrays.stream(objects)
                                .map(obj -> (Path) obj)
                                .toList()
                    )
                )
                .flatMap(tempPaths -> {
                    List<Resource> imageResources = tempPaths.stream()
                            .map(path -> (Resource) new FileSystemResource(path.toFile()))
                            .toList();
                    
                    return multimodalService.compareImages(imageResources, prompt)
                            .doFinally(signal -> tempPaths.forEach(this::cleanupTempFile));
                })
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> logger.info("图片对比成功"))
                .doOnError(error -> logger.error("图片对比失败: {}", error.getMessage()));
    }

    // ==================== 结构化信息提取 API ====================

    /**
     * 从图片提取结构化信息
     * 
     * 从图片中提取特定格式的结构化数据。
     * 例如：从发票提取金额、日期，从名片提取联系方式等。
     * 
     * 示例请求：
     * curl -X POST http://localhost:8080/api/multimodal/extract \
     *   -F "image=@/path/to/invoice.png" \
     *   -F "prompt=提取发票信息" \
     *   -F "format={\"金额\":\"...\",\"日期\":\"...\"}"
     * 
     * @param image 上传的图片文件
     * @param extractionPrompt 信息提取提示词
     * @param outputFormat 期望的输出格式说明
     * @return 结构化提取结果
     */
    @PostMapping("/extract")
    public Mono<ResponseEntity<String>> extractStructuredInfo(
            @RequestPart("image") FilePart image,
            @RequestPart("prompt") String extractionPrompt,
            @RequestPart(value = "format", required = false) String outputFormat) {
        
        logger.info("收到结构化信息提取请求");

        return saveFilePartToTemp(image)
                .flatMap(tempPath -> {
                    Resource imageResource = new FileSystemResource(tempPath.toFile());
                    return multimodalService.extractStructuredInfo(imageResource, extractionPrompt, outputFormat)
                            .doFinally(signal -> cleanupTempFile(tempPath));
                })
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> logger.info("结构化信息提取成功"))
                .doOnError(error -> logger.error("结构化信息提取失败: {}", error.getMessage()));
    }

    // ==================== 图片文字分析 API ====================

    /**
     * 分析图片中的文字
     * 
     * 识别图片中的文字内容并进行分析处理。
     * 支持提取文字、总结内容、翻译等功能。
     * 
     * 示例请求：
     * curl -X POST "http://localhost:8080/api/multimodal/text?type=summarize" \
     *   -F "image=@/path/to/document.png"
     * 
     * @param image 上传的图片文件
     * @param type 分析类型：extract(提取), summarize(总结), translate(翻译), analyze(分析)
     * @return 文字分析结果
     */
    @PostMapping("/text")
    public Mono<ResponseEntity<String>> analyzeImageText(
            @RequestPart("image") FilePart image,
            @RequestPart(value = "type") String type) {
        
        logger.info("收到图片文字分析请求，类型: {}", type);

        return saveFilePartToTemp(image)
                .flatMap(tempPath -> {
                    Resource imageResource = new FileSystemResource(tempPath.toFile());
                    return multimodalService.analyzeImageText(imageResource, type)
                            .doFinally(signal -> cleanupTempFile(tempPath));
                })
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> logger.info("图片文字分析成功"))
                .doOnError(error -> logger.error("图片文字分析失败: {}", error.getMessage()));
    }

    // ==================== 创意描述 API ====================

    /**
     * 生成图片的创意描述
     * 
     * 基于图片内容生成创意文本内容。
     * 支持生成故事、诗歌、营销文案、社交媒体配文等。
     * 
     * 示例请求：
     * curl -X POST "http://localhost:8080/api/multimodal/creative?style=poem" \
     *   -F "image=@/path/to/image.png"
     * 
     * @param image 上传的图片文件
     * @param style 创意风格：story(故事), poem(诗歌), marketing(营销), social(社交媒体)
     * @return 创意内容
     */
    @PostMapping("/creative")
    public Mono<ResponseEntity<String>> creativeDescription(
            @RequestPart("image") FilePart image,
            @RequestPart(value = "style") String style) {
        
        logger.info("收到创意描述请求，风格: {}", style);

        return saveFilePartToTemp(image)
                .flatMap(tempPath -> {
                    Resource imageResource = new FileSystemResource(tempPath.toFile());
                    return multimodalService.creativeDescription(imageResource, style)
                            .doFinally(signal -> cleanupTempFile(tempPath));
                })
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> logger.info("创意描述生成成功"))
                .doOnError(error -> logger.error("创意描述生成失败: {}", error.getMessage()));
    }

    // ==================== 辅助方法 ====================

    /**
     * 将 FilePart 保存到临时文件
     * 
     * @param filePart 上传的文件
     * @return 临时文件路径
     */
    private Mono<Path> saveFilePartToTemp(FilePart filePart) {
        return Mono.fromCallable(() -> Files.createTempDirectory("multimodal_"))
                .flatMap(tempDir -> {
                    Path tempFile = tempDir.resolve(filePart.filename());
                    return filePart.transferTo(tempFile.toFile())
                            .then(Mono.fromCallable(() -> {
                                logger.debug("文件已保存到临时路径: {}", tempFile);
                                return tempFile;
                            }));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 清理临时文件
     * 
     * @param path 临时文件路径
     */
    private void cleanupTempFile(Path path) {
        try {
            Files.deleteIfExists(path);
            Files.deleteIfExists(path.getParent());
            logger.debug("临时文件已清理: {}", path);
        } catch (Exception e) {
            logger.warn("清理临时文件失败: {}", path, e);
        }
    }

    // ==================== 请求/响应记录类 ====================

    /**
     * 图片分析请求
     */
    public record ImageAnalysisRequest(String imageName, String question) {}

    /**
     * 视觉问答请求
     */
    public record VqaRequest(String imageName, String question) {}

    /**
     * 结构化提取请求
     */
    public record ExtractionRequest(String imageName, String prompt, String format) {}

    /**
     * 创意描述请求
     */
    public record CreativeRequest(String imageName, String style) {}
}
