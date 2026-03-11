package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 文件处理服务
 * 
 * 处理文件上传、读取、编码检测和文本提取
 * 支持 TXT 文件上传到向量存储的完整流程
 */
@Service
public class FileProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessingService.class);

    private final DocumentSplitterService documentSplitterService;
    private final VectorStoreService vectorStoreService;

    public FileProcessingService(
            DocumentSplitterService documentSplitterService,
            VectorStoreService vectorStoreService) {
        this.documentSplitterService = documentSplitterService;
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * 处理上传的 TXT 文件并存储到向量数据库
     * 
     * 完整流程：
     * 1. 读取文件内容
     * 2. 检测并转换编码
     * 3. 切割文本
     * 4. 存储到向量数据库
     * 
     * @param filePart 上传的文件
     * @param splitStrategy 切割策略：RECURSIVE, MARKDOWN, TOKEN, SEMANTIC, SMART_PARAGRAPH, CHARACTER
     * @param chunkSize 块大小（字符数）
     * @param chunkOverlap 块重叠大小
     * @return 处理结果信息
     */
    public Mono<FileProcessingResult> processAndStore(
            FilePart filePart,
            DocumentSplitterService.SplitStrategy splitStrategy,
            int chunkSize,
            int chunkOverlap) {
        
        String filename = filePart.filename();
        logger.info("Processing file: {}, strategy: {}, chunkSize: {}, overlap: {}", 
                filename, splitStrategy, chunkSize, chunkOverlap);

        return readFileContent(filePart)
                .flatMap(content -> {
                    // 切割文档
                    DocumentSplitterService.SplitOptions options = new DocumentSplitterService.SplitOptions()
                            .withChunkSize(chunkSize)
                            .withChunkOverlap(chunkOverlap);
                    
                    List<Document> documents = documentSplitterService.split(content, filename, splitStrategy, options);
                    
                    if (documents.isEmpty()) {
                        return Mono.error(new IllegalArgumentException("No content could be extracted from file"));
                    }

                    // 存储到向量数据库
                    return vectorStoreService.addDocuments(documents)
                            .thenReturn(new FileProcessingResult(
                                    filename,
                                    documents.size(),
                                    content.length(),
                                    splitStrategy.name(),
                                    "File processed and stored successfully"
                            ));
                })
                .doOnSuccess(result -> logger.info("Successfully processed file: {}", filename))
                .doOnError(error -> logger.error("Failed to process file: {}", filename, error));
    }

    /**
     * 简化的文件处理方法（使用默认参数）
     * 默认使用递归字符切割策略
     * 
     * @param filePart 上传的文件
     * @return 处理结果
     */
    public Mono<FileProcessingResult> processAndStore(FilePart filePart) {
        return processAndStore(filePart, DocumentSplitterService.SplitStrategy.RECURSIVE, 1000, 200);
    }

    /**
     * 读取文件内容为字符串
     * 
     * @param filePart 上传的文件
     * @return 文件内容
     */
    public Mono<String> readFileContent(FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    
                    // 尝试检测编码，优先使用 UTF-8
                    String content = detectAndDecode(bytes);
                    return content;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 从本地文件路径读取内容
     * 
     * @param filePath 文件路径
     * @return 文件内容
     */
    public Mono<String> readLocalFile(Path filePath) {
        return Mono.fromCallable(() -> {
                    byte[] bytes = Files.readAllBytes(filePath);
                    String filename = filePath.getFileName().toString();
                    String content = detectAndDecode(bytes);
                    logger.info("Read local file: {}, size: {} bytes", filename, bytes.length);
                    return content;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 处理本地文件并存储到向量数据库
     * 
     * @param filePath 本地文件路径
     * @param splitStrategy 切割策略
     * @param chunkSize 块大小
     * @param chunkOverlap 重叠大小
     * @return 处理结果
     */
    public Mono<FileProcessingResult> processLocalFile(
            Path filePath,
            DocumentSplitterService.SplitStrategy splitStrategy,
            int chunkSize,
            int chunkOverlap) {
        
        String filename = filePath.getFileName().toString();
        
        return readLocalFile(filePath)
                .flatMap(content -> {
                    DocumentSplitterService.SplitOptions options = new DocumentSplitterService.SplitOptions()
                            .withChunkSize(chunkSize)
                            .withChunkOverlap(chunkOverlap);
                    
                    List<Document> documents = documentSplitterService.split(content, filename, splitStrategy, options);
                    
                    if (documents.isEmpty()) {
                        return Mono.error(new IllegalArgumentException("No content could be extracted from file"));
                    }

                    return vectorStoreService.addDocuments(documents)
                            .thenReturn(new FileProcessingResult(
                                    filename,
                                    documents.size(),
                                    content.length(),
                                    splitStrategy.name(),
                                    "Local file processed and stored successfully"
                            ));
                });
    }



    /**
     * 检测并解码字节数组为字符串
     * 
     * 优先尝试 UTF-8，如果失败则尝试 GBK
     * 
     * @param bytes 字节数组
     * @return 解码后的字符串
     */
    private String detectAndDecode(byte[] bytes) {
        // 首先尝试 UTF-8
        String utf8Content = new String(bytes, StandardCharsets.UTF_8);
        
        // 简单的检测：如果包含 UTF-8 解码失败的常见乱码特征，尝试 GBK
        if (containsGarbledCharacters(utf8Content)) {
            try {
                String gbkContent = new String(bytes, Charset.forName("GBK"));
                logger.debug("Detected GBK encoding");
                return gbkContent;
            } catch (Exception e) {
                logger.warn("Failed to decode with GBK, falling back to UTF-8");
            }
        }
        
        return utf8Content;
    }

    /**
     * 检查字符串是否包含乱码特征
     * 
     * @param content 字符串
     * @return 是否可能包含乱码
     */
    private boolean containsGarbledCharacters(String content) {
        // 检查常见的 UTF-8 解码 GBK 时的乱码特征
        // 这些字符通常表示编码错误
        String[] garbledPatterns = {"�", "ï¿½", "Â", "Ã"};
        for (String pattern : garbledPatterns) {
            if (content.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 文件处理结果记录
     */
    public record FileProcessingResult(
            String filename,
            int chunkCount,
            int totalCharacters,
            String splitMethod,
            String message
    ) {}
}
