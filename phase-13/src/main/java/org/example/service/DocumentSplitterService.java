package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文档切割服务
 * 
 * 提供多种文本切割策略，将长文本分割成适合向量存储的文档块
 * 支持按字符数、段落、句子等多种切割方式
 */
@Service
public class DocumentSplitterService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentSplitterService.class);

    /**
     * 默认切割配置
     */
    public static final int DEFAULT_CHUNK_SIZE = 1000;      // 每个块的最大字符数
    public static final int DEFAULT_CHUNK_OVERLAP = 200;    // 块之间的重叠字符数

    /**
     * 按固定字符数切割文本（带重叠）
     * 
     * 这是最常用的切割策略，确保每个块都有足够的上下文信息
     * 
     * @param text 原始文本
     * @param filename 文件名（用于元数据）
     * @return 切割后的文档列表
     */
    public List<Document> splitByCharacter(String text, String filename) {
        return splitByCharacter(text, filename, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
    }

    /**
     * 按固定字符数切割文本（带重叠）- 自定义参数
     * 
     * @param text 原始文本
     * @param filename 文件名
     * @param chunkSize 每个块的大小
     * @param chunkOverlap 块之间的重叠大小
     * @return 切割后的文档列表
     */
    public List<Document> splitByCharacter(String text, String filename, int chunkSize, int chunkOverlap) {
        List<Document> documents = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            logger.warn("Empty text provided for splitting");
            return documents;
        }

        // 清理文本：统一换行符
        String cleanedText = text.replace("\r\n", "\n").replace("\r", "\n");
        
        int textLength = cleanedText.length();
        int startIndex = 0;
        int chunkIndex = 0;

        while (startIndex < textLength) {
            // 计算当前块的结束位置
            int endIndex = Math.min(startIndex + chunkSize, textLength);
            
            // 如果不是最后一块，尝试在句子或单词边界切割
            if (endIndex < textLength) {
                endIndex = findBestSplitPoint(cleanedText, endIndex);
            }

            // 提取当前块的内容
            String chunk = cleanedText.substring(startIndex, endIndex).trim();
            
            if (!chunk.isEmpty()) {
                // 创建文档块元数据
                Map<String, Object> metadata = Map.of(
                    "source", filename,
                    "chunk_index", chunkIndex,
                    "start_char", startIndex,
                    "end_char", endIndex,
                    "total_chunks", -1  // 暂时设为-1，最后更新
                );
                
                documents.add(new Document(chunk, metadata));
                chunkIndex++;
            }

            // 计算下一个块的起始位置（考虑重叠）
            startIndex = endIndex - chunkOverlap;
            if (startIndex >= endIndex) {
                startIndex = endIndex; // 防止死循环
            }
        }

        // 更新总块数
        final int totalChunks = documents.size();
        documents.replaceAll(doc -> {
            Map<String, Object> updatedMetadata = new java.util.HashMap<>(doc.getMetadata());
            updatedMetadata.put("total_chunks", totalChunks);
            return new Document(doc.getText(), updatedMetadata);
        });

        logger.info("Split document '{}' into {} chunks (size={}, overlap={})", 
                filename, documents.size(), chunkSize, chunkOverlap);
        
        return documents;
    }

    /**
     * 按段落切割文本
     * 
     * 适用于结构化文档，如文章、报告等
     * 
     * @param text 原始文本
     * @param filename 文件名
     * @return 切割后的文档列表
     */
    public List<Document> splitByParagraph(String text, String filename) {
        List<Document> documents = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return documents;
        }

        // 按段落分割（一个或多个换行符）
        String[] paragraphs = text.split("\\n\\s*\\n");
        
        int chunkIndex = 0;
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (!trimmed.isEmpty()) {
                Map<String, Object> metadata = Map.of(
                    "source", filename,
                    "chunk_index", chunkIndex,
                    "split_method", "paragraph"
                );
                documents.add(new Document(trimmed, metadata));
                chunkIndex++;
            }
        }

        logger.info("Split document '{}' into {} paragraphs", filename, documents.size());
        return documents;
    }

    /**
     * 智能切割：先按段落，如果段落太长再按字符数切割
     * 
     * @param text 原始文本
     * @param filename 文件名
     * @param maxChunkSize 最大块大小
     * @return 切割后的文档列表
     */
    public List<Document> splitSmart(String text, String filename, int maxChunkSize) {
        List<Document> documents = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return documents;
        }

        String[] paragraphs = text.split("\\n\\s*\\n");
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        int startParagraph = 0;

        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i].trim();
            if (paragraph.isEmpty()) continue;

            // 如果当前段落本身超过最大长度，需要先切割当前累积的内容
            if (paragraph.length() > maxChunkSize) {
                // 保存当前累积的内容
                if (currentChunk.length() > 0) {
                    Map<String, Object> metadata = Map.of(
                        "source", filename,
                        "chunk_index", chunkIndex++,
                        "paragraph_range", startParagraph + "-" + (i - 1),
                        "split_method", "smart"
                    );
                    documents.add(new Document(currentChunk.toString().trim(), metadata));
                    currentChunk = new StringBuilder();
                }
                
                // 切割这个长段落
                List<Document> paragraphChunks = splitByCharacter(paragraph, filename, maxChunkSize, DEFAULT_CHUNK_OVERLAP);
                documents.addAll(paragraphChunks);
                startParagraph = i + 1;
            } 
            // 如果添加这个段落会超过限制，先保存当前块
            else if (currentChunk.length() + paragraph.length() + 2 > maxChunkSize) {
                if (currentChunk.length() > 0) {
                    Map<String, Object> metadata = Map.of(
                        "source", filename,
                        "chunk_index", chunkIndex++,
                        "paragraph_range", startParagraph + "-" + (i - 1),
                        "split_method", "smart"
                    );
                    documents.add(new Document(currentChunk.toString().trim(), metadata));
                }
                currentChunk = new StringBuilder(paragraph);
                startParagraph = i;
            } 
            // 否则添加到当前块
            else {
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            }
        }

        // 保存最后一个块
        if (currentChunk.length() > 0) {
            Map<String, Object> metadata = Map.of(
                "source", filename,
                "chunk_index", chunkIndex,
                "paragraph_range", startParagraph + "-" + (paragraphs.length - 1),
                "split_method", "smart"
            );
            documents.add(new Document(currentChunk.toString().trim(), metadata));
        }

        logger.info("Smart split document '{}' into {} chunks", filename, documents.size());
        return documents;
    }

    /**
     * 查找最佳切割点（优先在句子结束处切割）
     * 
     * @param text 文本
     * @param targetIndex 目标切割位置
     * @return 实际切割位置
     */
    private int findBestSplitPoint(String text, int targetIndex) {
        // 在目标位置前后100个字符范围内寻找最佳切割点
        int searchStart = Math.max(targetIndex - 100, 0);
        int searchEnd = Math.min(targetIndex + 100, text.length());
        
        String searchArea = text.substring(searchStart, searchEnd);
        int targetInArea = targetIndex - searchStart;
        
        // 优先找句号、问号、感叹号后跟空格或换行
        String[] sentenceEndings = {". ", "? ", "! ", "。", "？", "！", "\n"};
        int bestPoint = -1;
        
        for (String ending : sentenceEndings) {
            int index = searchArea.lastIndexOf(ending, targetInArea);
            if (index > bestPoint && index > targetInArea - 50) {
                bestPoint = index + ending.length();
            }
        }
        
        // 如果没找到句子结束，找空格
        if (bestPoint == -1) {
            int spaceIndex = searchArea.lastIndexOf(" ", targetInArea);
            if (spaceIndex > targetInArea - 30) {
                bestPoint = spaceIndex + 1;
            }
        }
        
        // 如果还是没找到，就使用目标位置
        if (bestPoint == -1) {
            bestPoint = targetInArea;
        }
        
        return searchStart + bestPoint;
    }
}
