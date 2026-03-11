package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 文档切割服务 - 策略模式重构版
 * 
 * 支持多种高级切割策略：
 * 1. 递归字符切割 (Recursive Character Splitting) - LangChain 推荐
 * 2. Markdown 结构切割 (Markdown Structure Splitting)
 * 3. Token 感知切割 (Token-aware Splitting)
 * 4. 语义切割 (Semantic Splitting)
 * 5. 智能段落切割 (Smart Paragraph Splitting)
 * 
 * 架构设计：
 * - 使用策略模式，每种切割方式独立实现
 * - 支持链式调用和组合策略
 * - 统一的元数据管理
 */
@Service
public class DocumentSplitterService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentSplitterService.class);

    // ==================== 配置常量 ====================
    
    /** 默认块大小（字符数） */
    public static final int DEFAULT_CHUNK_SIZE = 1000;
    /** 默认块重叠大小 */
    public static final int DEFAULT_CHUNK_OVERLAP = 200;
    /** 默认 Token 限制 */
    public static final int DEFAULT_MAX_TOKENS = 512;
    /** 语义相似度阈值 */
    public static final double DEFAULT_SEMANTIC_THRESHOLD = 0.7;
    
    // ==================== 递归字符切割分隔符 ====================
    
    /**
     * 递归字符切割的分隔符优先级列表
     * 按语义完整性从高到低排序
     */
    private static final String[] RECURSIVE_SEPARATORS = {
        "\n\n",      // 段落
        "\n",        // 换行
        ". ",        // 英文句号+空格
        "? ",        // 英文问号+空格
        "! ",        // 英文感叹号+空格
        "。",        // 中文句号
        "？",        // 中文问号
        "！",        // 中文感叹号
        "; ",        // 分号
        "；",        // 中文分号
        ", ",        // 逗号+空格
        "，",        // 中文逗号
        " ",         // 空格
        ""           // 字符（最后手段）
    };

    // ==================== 依赖注入 ====================
    
    private final EmbeddingModel embeddingModel;
    
    public DocumentSplitterService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    // ==================== 策略枚举 ====================
    
    /**
     * 文档切割策略枚举
     */
    public enum SplitStrategy {
        /** 递归字符切割 - 保持语义完整性 */
        RECURSIVE,
        /** Markdown 结构切割 - 保留文档结构 */
        MARKDOWN,
        /** Token 感知切割 - 适配 LLM 限制 */
        TOKEN,
        /** 语义切割 - 基于 Embedding 相似度 */
        SEMANTIC,
        /** 智能段落切割 - 段落+字符混合 */
        SMART_PARAGRAPH,
        /** 固定字符切割 - 简单快速 */
        CHARACTER
    }

    // ==================== 统一入口方法 ====================

    /**
     * 统一文档切割入口
     * 
     * @param text 原始文本
     * @param filename 文件名
     * @param strategy 切割策略
     * @return 切割后的文档列表
     */
    public List<Document> split(String text, String filename, SplitStrategy strategy) {
        return split(text, filename, strategy, new SplitOptions());
    }

    /**
     * 统一文档切割入口 - 带选项
     * 
     * @param text 原始文本
     * @param filename 文件名
     * @param strategy 切割策略
     * @param options 切割选项
     * @return 切割后的文档列表
     */
    public List<Document> split(String text, String filename, SplitStrategy strategy, SplitOptions options) {
        if (text == null || text.isEmpty()) {
            logger.warn("Empty text provided for splitting");
            return Collections.emptyList();
        }

        List<Document> documents = switch (strategy) {
            case RECURSIVE -> splitRecursive(text, filename, options);
            case MARKDOWN -> splitMarkdown(text, filename, options);
            case TOKEN -> splitByTokens(text, filename, options);
            case SEMANTIC -> splitSemantic(text, filename, options);
            case SMART_PARAGRAPH -> splitSmartParagraph(text, filename, options);
            case CHARACTER -> splitByCharacter(text, filename, options);
        };

        logger.info("Split document '{}' using {} strategy into {} chunks", 
                filename, strategy, documents.size());
        
        return documents;
    }

    // ==================== 1. 递归字符切割 ====================

    /**
     * 递归字符切割 - LangChain 推荐策略
     * 
     * 核心思想：
     * 1. 按分隔符优先级尝试分割
     * 2. 如果分割后的块仍太大，递归使用下一个分隔符
     * 3. 优先保持语义完整性
     * 
     * @param text 原始文本
     * @param filename 文件名
     * @param options 切割选项
     * @return 切割后的文档列表
     */
    public List<Document> splitRecursive(String text, String filename, SplitOptions options) {
        int chunkSize = options.getChunkSize() != null ? options.getChunkSize() : DEFAULT_CHUNK_SIZE;
        int chunkOverlap = options.getChunkOverlap() != null ? options.getChunkOverlap() : DEFAULT_CHUNK_OVERLAP;
        
        List<Document> documents = new ArrayList<>();
        
        // 清理文本
        String cleanedText = normalizeText(text);
        
        // 递归分割
        List<TextChunk> chunks = recursiveSplitInternal(
            cleanedText, 
            0, 
            chunkSize, 
            chunkOverlap, 
            0
        );
        
        // 转换为 Document 对象
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            Map<String, Object> metadata = buildMetadata(filename, i, chunks.size(), "recursive");
            metadata.put("separator_used", chunk.getSeparator());
            documents.add(new Document(chunk.getContent(), metadata));
        }
        
        return documents;
    }

    /**
     * 递归分割内部实现
     */
    private List<TextChunk> recursiveSplitInternal(
            String text, 
            int separatorIndex, 
            int chunkSize, 
            int chunkOverlap,
            int depth) {
        
        List<TextChunk> result = new ArrayList<>();
        
        // 如果文本已经小于块大小，直接返回
        if (text.length() <= chunkSize) {
            result.add(new TextChunk(text, depth > 0 ? RECURSIVE_SEPARATORS[separatorIndex - 1] : "none"));
            return result;
        }
        
        // 如果已经用完所有分隔符，强制按字符切割
        if (separatorIndex >= RECURSIVE_SEPARATORS.length) {
            return forceCharacterSplit(text, chunkSize, chunkOverlap);
        }
        
        String separator = RECURSIVE_SEPARATORS[separatorIndex];
        
        // 按当前分隔符分割
        String[] parts;
        if (separator.isEmpty()) {
            // 字符级分割
            parts = text.split("");
        } else {
            parts = text.split(Pattern.quote(separator));
        }
        
        // 合并小块，确保不超过 chunkSize
        StringBuilder currentChunk = new StringBuilder();
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            String withSeparator = i < parts.length - 1 ? part + separator : part;
            
            // 如果单个部分就超过限制，需要递归分割
            if (part.length() > chunkSize) {
                // 先保存当前累积的内容
                if (currentChunk.length() > 0) {
                    result.add(new TextChunk(currentChunk.toString().trim(), separator));
                    currentChunk = new StringBuilder();
                }
                
                // 递归分割这个长部分
                List<TextChunk> subChunks = recursiveSplitInternal(
                    part, 
                    separatorIndex + 1, 
                    chunkSize, 
                    chunkOverlap,
                    depth + 1
                );
                result.addAll(subChunks);
            }
            // 如果添加这个部分会超过限制，先保存当前块
            else if (currentChunk.length() + withSeparator.length() > chunkSize && currentChunk.length() > 0) {
                result.add(new TextChunk(currentChunk.toString().trim(), separator));
                
                // 考虑重叠
                if (chunkOverlap > 0 && currentChunk.length() > chunkOverlap) {
                    String overlapText = currentChunk.substring(currentChunk.length() - chunkOverlap);
                    currentChunk = new StringBuilder(overlapText).append(withSeparator);
                } else {
                    currentChunk = new StringBuilder(withSeparator);
                }
            }
            // 否则添加到当前块
            else {
                currentChunk.append(withSeparator);
            }
        }
        
        // 保存最后一个块
        if (currentChunk.length() > 0) {
            result.add(new TextChunk(currentChunk.toString().trim(), separator));
        }
        
        return result;
    }

    /**
     * 强制按字符切割（最后手段）
     */
    private List<TextChunk> forceCharacterSplit(String text, int chunkSize, int chunkOverlap) {
        List<TextChunk> result = new ArrayList<>();
        int start = 0;
        
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            result.add(new TextChunk(text.substring(start, end), "character"));
            start = end - chunkOverlap;
            if (start >= end) start = end;
        }
        
        return result;
    }

    // ==================== 2. Markdown 结构切割 ====================

    /**
     * Markdown 结构切割
     * 
     * 特点：
     * 1. 识别 Markdown 标题层级（# ## ###）
     * 2. 保留标题路径信息（如：文档/章节/小节）
     * 3. 支持代码块、列表等特殊结构
     * 
     * @param text Markdown 文本
     * @param filename 文件名
     * @param options 切割选项
     * @return 切割后的文档列表
     */
    public List<Document> splitMarkdown(String text, String filename, SplitOptions options) {
        int maxChunkSize = options.getChunkSize() != null ? options.getChunkSize() : DEFAULT_CHUNK_SIZE;
        
        List<Document> documents = new ArrayList<>();
        List<MarkdownSection> sections = parseMarkdownSections(text);
        
        int chunkIndex = 0;
        StringBuilder currentChunk = new StringBuilder();
        List<String> currentHeaders = new ArrayList<>();
        MarkdownSection currentSection = null;
        
        for (MarkdownSection section : sections) {
            // 如果当前累积内容 + 新 section 超过限制，先保存
            if (currentChunk.length() + section.getContent().length() > maxChunkSize 
                    && currentChunk.length() > 0) {
                
                Map<String, Object> metadata = buildMarkdownMetadata(
                    filename, 
                    chunkIndex++, 
                    currentSection
                );
                documents.add(new Document(currentChunk.toString().trim(), metadata));
                
                // 保留标题上下文
                currentChunk = new StringBuilder();
            }
            
            // 更新标题层级
            if (section.isHeading()) {
                updateHeaderHierarchy(currentHeaders, section);
            }
            
            // 添加内容
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(section.getContent());
            currentSection = section;
        }
        
        // 保存最后一个块
        if (currentChunk.length() > 0) {
            Map<String, Object> metadata = buildMarkdownMetadata(
                filename, 
                chunkIndex, 
                currentSection
            );
            documents.add(new Document(currentChunk.toString().trim(), metadata));
        }
        
        return documents;
    }

    /**
     * 解析 Markdown 为章节列表
     */
    private List<MarkdownSection> parseMarkdownSections(String text) {
        List<MarkdownSection> sections = new ArrayList<>();
        
        // 匹配标题、代码块、普通段落
        Pattern pattern = Pattern.compile(
            "^(#{1,6}\\s+.+$)|" +           // 标题
            "(```[\\s\\S]*?```)|" +          // 代码块
            "(^\\s*[-*+]\\s+.+$)|" +        // 列表项
            "(^\\s*\\d+\\.\\s+.+$)|" +      // 有序列表
            "(^>.+$)|" +                     // 引用
            "(^\\[.+\\]:\\s*.+$)|" +         // 链接引用
            "(.+)",                          // 普通文本
            Pattern.MULTILINE
        );
        
        String[] lines = text.split("\n");
        StringBuilder currentBlock = new StringBuilder();
        String blockType = "paragraph";
        int headingLevel = 0;
        String headingText = "";
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // 检测标题
            if (trimmed.matches("^#{1,6}\\s+.+")) {
                // 保存之前的块
                if (currentBlock.length() > 0) {
                    sections.add(new MarkdownSection(
                        blockType, 
                        currentBlock.toString().trim(),
                        headingLevel,
                        headingText
                    ));
                    currentBlock = new StringBuilder();
                }
                
                // 开始新标题
                headingLevel = countLeadingHashes(trimmed);
                headingText = trimmed.replaceAll("^#{1,6}\\s+", "");
                blockType = "heading";
                currentBlock.append(line);
            }
            // 检测代码块开始/结束
            else if (trimmed.startsWith("```")) {
                if (blockType.equals("code")) {
                    // 结束代码块
                    currentBlock.append("\n").append(line);
                    sections.add(new MarkdownSection(
                        blockType, 
                        currentBlock.toString().trim(),
                        headingLevel,
                        headingText
                    ));
                    currentBlock = new StringBuilder();
                    blockType = "paragraph";
                } else {
                    // 开始代码块
                    if (currentBlock.length() > 0) {
                        sections.add(new MarkdownSection(
                            blockType, 
                            currentBlock.toString().trim(),
                            headingLevel,
                            headingText
                        ));
                        currentBlock = new StringBuilder();
                    }
                    blockType = "code";
                    currentBlock.append(line);
                }
            }
            // 其他内容
            else {
                if (currentBlock.length() > 0) {
                    currentBlock.append("\n");
                }
                currentBlock.append(line);
            }
        }
        
        // 保存最后一个块
        if (currentBlock.length() > 0) {
            sections.add(new MarkdownSection(
                blockType, 
                currentBlock.toString().trim(),
                headingLevel,
                headingText
            ));
        }
        
        return sections;
    }

    /**
     * 计算标题层级
     */
    private int countLeadingHashes(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == '#') count++;
            else break;
        }
        return Math.min(count, 6);
    }

    /**
     * 更新标题层级
     */
    private void updateHeaderHierarchy(List<String> headers, MarkdownSection section) {
        int level = section.getHeadingLevel();
        String title = section.getHeadingText();
        
        // 调整列表大小
        while (headers.size() < level) {
            headers.add("");
        }
        while (headers.size() > level) {
            headers.remove(headers.size() - 1);
        }
        
        if (level > 0) {
            headers.set(level - 1, title);
        }
    }

    /**
     * 构建 Markdown 元数据
     */
    private Map<String, Object> buildMarkdownMetadata(
            String filename, 
            int chunkIndex, 
            MarkdownSection section) {
        
        Map<String, Object> metadata = buildMetadata(filename, chunkIndex, -1, "markdown");
        
        if (section != null) {
            metadata.put("section_type", section.getType());
            metadata.put("heading_level", section.getHeadingLevel());
            metadata.put("heading_text", section.getHeadingText());
        }
        
        return metadata;
    }

    // ==================== 3. Token 感知切割 ====================

    /**
     * Token 感知切割
     * 
     * 特点：
     * 1. 估算 Token 数量（基于字符数和语言模型经验）
     * 2. 确保不超过 LLM 的上下文限制
     * 3. 优先在语义边界切割
     * 
     * Token 估算规则：
     * - 英文：~4 字符/token
     * - 中文：~1.5 字符/token
     * - 代码：~3 字符/token
     * 
     * @param text 原始文本
     * @param filename 文件名
     * @param options 切割选项
     * @return 切割后的文档列表
     */
    public List<Document> splitByTokens(String text, String filename, SplitOptions options) {
        Integer maxTokens = options.getMaxTokens();
        if (maxTokens == null) {
            maxTokens = DEFAULT_MAX_TOKENS;
        }
        
        List<Document> documents = new ArrayList<>();
        
        // 先按句子分割
        List<String> sentences = splitIntoSentences(text);
        
        StringBuilder currentChunk = new StringBuilder();
        int currentTokenCount = 0;
        int chunkIndex = 0;
        int startSentence = 0;
        
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            int sentenceTokens = estimateTokens(sentence);
            
            // 如果单个句子就超过限制，需要进一步分割
            if (sentenceTokens > maxTokens) {
                // 先保存当前累积的内容
                if (currentChunk.length() > 0) {
                    Map<String, Object> metadata = buildTokenMetadata(
                        filename, 
                        chunkIndex++, 
                        currentTokenCount,
                        startSentence,
                        i - 1
                    );
                    documents.add(new Document(currentChunk.toString().trim(), metadata));
                    currentChunk = new StringBuilder();
                    currentTokenCount = 0;
                }
                
                // 强制分割这个长句子
                List<Document> forcedChunks = forceSplitByTokens(
                    sentence, 
                    filename, 
                    chunkIndex, 
                    maxTokens,
                    i
                );
                documents.addAll(forcedChunks);
                chunkIndex += forcedChunks.size();
                startSentence = i + 1;
            }
            // 如果添加这个句子会超过限制，先保存当前块
            else if (currentTokenCount + sentenceTokens > maxTokens && currentChunk.length() > 0) {
                Map<String, Object> metadata = buildTokenMetadata(
                    filename, 
                    chunkIndex++, 
                    currentTokenCount,
                    startSentence,
                    i - 1
                );
                documents.add(new Document(currentChunk.toString().trim(), metadata));
                
                currentChunk = new StringBuilder(sentence);
                currentTokenCount = sentenceTokens;
                startSentence = i;
            }
            // 否则添加到当前块
            else {
                if (currentChunk.length() > 0) {
                    currentChunk.append(" ");
                    currentTokenCount++; // 空格也算一个 token
                }
                currentChunk.append(sentence);
                currentTokenCount += sentenceTokens;
            }
        }
        
        // 保存最后一个块
        if (currentChunk.length() > 0) {
            Map<String, Object> metadata = buildTokenMetadata(
                filename, 
                chunkIndex, 
                currentTokenCount,
                startSentence,
                sentences.size() - 1
            );
            documents.add(new Document(currentChunk.toString().trim(), metadata));
        }
        
        return documents;
    }

    /**
     * 估算文本的 Token 数量
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        int chineseChars = 0;
        int englishChars = 0;
        int numbers = 0;
        int spaces = 0;
        int others = 0;
        
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A) {
                chineseChars++;
            } else if (Character.isLetter(c)) {
                englishChars++;
            } else if (Character.isDigit(c)) {
                numbers++;
            } else if (Character.isWhitespace(c)) {
                spaces++;
            } else {
                others++;
            }
        }
        
        // 估算公式
        double tokens = chineseChars * 0.6  // 中文约 1.67 字符/token
                + englishChars * 0.25       // 英文约 4 字符/token
                + numbers * 0.3             // 数字约 3.3 字符/token
                + spaces * 0.2              // 空格
                + others * 0.5;             // 其他字符
        
        return (int) Math.ceil(tokens);
    }

    /**
     * 强制按 Token 分割（针对超长句子）
     */
    private List<Document> forceSplitByTokens(
            String text, 
            String filename, 
            int startIndex,
            int maxTokens,
            int sentenceIndex) {
        
        List<Document> documents = new ArrayList<>();
        
        // 估算每个字符的 token 占比
        double tokensPerChar = (double) estimateTokens(text) / text.length();
        int charsPerChunk = (int) (maxTokens / tokensPerChar);
        
        int start = 0;
        int chunkIndex = startIndex;
        
        while (start < text.length()) {
            int end = Math.min(start + charsPerChunk, text.length());
            
            // 尝试在单词边界切割
            if (end < text.length()) {
                end = findWordBoundary(text, end);
            }
            
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                Map<String, Object> metadata = buildMetadata(filename, chunkIndex++, -1, "token_forced");
                metadata.put("sentence_index", sentenceIndex);
                metadata.put("estimated_tokens", estimateTokens(chunk));
                documents.add(new Document(chunk, metadata));
            }
            
            start = end;
        }
        
        return documents;
    }

    /**
     * 构建 Token 切割的元数据
     */
    private Map<String, Object> buildTokenMetadata(
            String filename, 
            int chunkIndex, 
            int tokenCount,
            int startSentence,
            int endSentence) {
        
        Map<String, Object> metadata = buildMetadata(filename, chunkIndex, -1, "token");
        metadata.put("estimated_tokens", tokenCount);
        metadata.put("sentence_range", startSentence + "-" + endSentence);
        
        return metadata;
    }

    // ==================== 4. 语义切割 ====================

    /**
     * 语义切割 - 基于 Embedding 相似度
     * 
     * 核心思想：
     * 1. 将文本分割为句子
     * 2. 计算相邻句子的 Embedding 相似度
     * 3. 在相似度低于阈值处切割（主题变化处）
     * 
     * 优点：保持语义连贯性，避免在话题中间切割
     * 缺点：需要调用 Embedding API，成本较高
     * 
     * @param text 原始文本
     * @param filename 文件名
     * @param options 切割选项
     * @return 切割后的文档列表
     */
    public List<Document> splitSemantic(String text, String filename, SplitOptions options) {
        Double threshold = options.getSemanticThreshold();
        if (threshold == null) {
            threshold = DEFAULT_SEMANTIC_THRESHOLD;
        }
        
        Integer maxTokens = options.getMaxTokens();
        if (maxTokens == null) {
            maxTokens = DEFAULT_MAX_TOKENS * 2; // 语义块可以更大一些
        }
        
        // 1. 分割为句子
        List<String> sentences = splitIntoSentences(text);
        if (sentences.size() <= 1) {
            Map<String, Object> metadata = buildMetadata(filename, 0, 1, "semantic");
            metadata.put("semantic_method", "single_sentence");
            return List.of(new Document(text, metadata));
        }
        
        // 2. 计算每个句子的 Embedding
        List<float[]> embeddings = new ArrayList<>();
        for (String sentence : sentences) {
            try {
                float[] embedding = embeddingModel.embed(sentence);
                embeddings.add(embedding);
            } catch (Exception e) {
                logger.warn("Failed to embed sentence, using zero vector: {}", e.getMessage());
                embeddings.add(new float[0]);
            }
        }
        
        // 3. 计算相邻句子的相似度，确定切割点
        List<Integer> splitPoints = new ArrayList<>();
        splitPoints.add(0); // 始终从开头开始
        
        for (int i = 0; i < embeddings.size() - 1; i++) {
            float[] emb1 = embeddings.get(i);
            float[] emb2 = embeddings.get(i + 1);
            
            if (emb1.length > 0 && emb2.length > 0) {
                double similarity = cosineSimilarity(emb1, emb2);
                
                // 如果相似度低于阈值，在此处切割
                if (similarity < threshold) {
                    splitPoints.add(i + 1);
                    logger.debug("Semantic split at sentence {} (similarity: {})", i + 1, similarity);
                }
            }
        }
        splitPoints.add(sentences.size()); // 结尾
        
        // 4. 根据切割点构建文档块
        List<Document> documents = new ArrayList<>();
        int chunkIndex = 0;
        
        for (int i = 0; i < splitPoints.size() - 1; i++) {
            int startIdx = splitPoints.get(i);
            int endIdx = splitPoints.get(i + 1);
            
            // 合并句子
            StringBuilder chunk = new StringBuilder();
            for (int j = startIdx; j < endIdx; j++) {
                if (chunk.length() > 0) chunk.append(" ");
                chunk.append(sentences.get(j));
            }
            
            // 检查 Token 限制，如果超过需要进一步分割
            String chunkText = chunk.toString();
            int chunkTokens = estimateTokens(chunkText);
            
            if (chunkTokens > maxTokens) {
                // 使用 Token 感知切割进一步分割
                List<Document> subDocs = splitByTokens(
                    chunkText, 
                    filename, 
                    new SplitOptions().withMaxTokens(maxTokens)
                );
                
                // 更新元数据
                for (int j = 0; j < subDocs.size(); j++) {
                    Document subDoc = subDocs.get(j);
                    Map<String, Object> metadata = new HashMap<>(subDoc.getMetadata());
                    metadata.put("semantic_chunk", chunkIndex);
                    metadata.put("semantic_subchunk", j);
                    documents.add(new Document(subDoc.getText(), metadata));
                }
            } else {
                Map<String, Object> metadata = buildMetadata(filename, chunkIndex, -1, "semantic");
                metadata.put("sentence_range", startIdx + "-" + (endIdx - 1));
                metadata.put("estimated_tokens", chunkTokens);
                
                // 记录相邻块的相似度信息
                if (i < splitPoints.size() - 2) {
                    int nextStart = splitPoints.get(i + 1);
                    if (startIdx < embeddings.size() && nextStart < embeddings.size()) {
                        double similarity = cosineSimilarity(
                            embeddings.get(startIdx), 
                            embeddings.get(nextStart)
                        );
                        metadata.put("boundary_similarity", similarity);
                    }
                }
                
                documents.add(new Document(chunkText, metadata));
            }
            
            chunkIndex++;
        }
        
        // 更新总块数
        final int totalChunks = documents.size();
        documents.replaceAll(doc -> {
            Map<String, Object> updatedMetadata = new HashMap<>(doc.getMetadata());
            updatedMetadata.put("total_chunks", totalChunks);
            return new Document(doc.getText(), updatedMetadata);
        });
        
        return documents;
    }

    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length || vec1.length == 0) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    // ==================== 5. 智能段落切割 ====================

    /**
     * 智能段落切割 - 改进版
     * 
     * 结合段落语义和大小限制
     */
    public List<Document> splitSmartParagraph(String text, String filename, SplitOptions options) {
        int maxChunkSize = options.getChunkSize() != null ? options.getChunkSize() : DEFAULT_CHUNK_SIZE;
        int chunkOverlap = options.getChunkOverlap() != null ? options.getChunkOverlap() : DEFAULT_CHUNK_OVERLAP;
        
        List<Document> documents = new ArrayList<>();
        
        // 按段落分割
        String[] paragraphs = text.split("\n\s*\n");
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        int startParagraph = 0;
        
        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i].trim();
            if (paragraph.isEmpty()) continue;
            
            // 如果段落本身超过限制
            if (paragraph.length() > maxChunkSize) {
                // 保存当前累积的内容
                if (currentChunk.length() > 0) {
                    Map<String, Object> metadata = buildMetadata(
                        filename, 
                        chunkIndex++, 
                        -1, 
                        "smart_paragraph"
                    );
                    metadata.put("paragraph_range", startParagraph + "-" + (i - 1));
                    documents.add(new Document(currentChunk.toString().trim(), metadata));
                    currentChunk = new StringBuilder();
                }
                
                // 使用递归切割处理长段落
                List<Document> paragraphChunks = splitRecursive(
                    paragraph, 
                    filename, 
                    new SplitOptions()
                        .withChunkSize(maxChunkSize)
                        .withChunkOverlap(chunkOverlap)
                );
                documents.addAll(paragraphChunks);
                startParagraph = i + 1;
            }
            // 如果添加这个段落会超过限制
            else if (currentChunk.length() + paragraph.length() + 2 > maxChunkSize 
                    && currentChunk.length() > 0) {
                
                Map<String, Object> metadata = buildMetadata(
                    filename, 
                    chunkIndex++, 
                    -1, 
                    "smart_paragraph"
                );
                metadata.put("paragraph_range", startParagraph + "-" + (i - 1));
                documents.add(new Document(currentChunk.toString().trim(), metadata));
                
                // 考虑重叠
                if (chunkOverlap > 0) {
                    currentChunk = new StringBuilder(getOverlapText(currentChunk.toString(), chunkOverlap));
                    if (currentChunk.length() > 0) {
                        currentChunk.append("\n\n");
                    }
                    currentChunk.append(paragraph);
                } else {
                    currentChunk = new StringBuilder(paragraph);
                }
                startParagraph = i;
            }
            // 添加到当前块
            else {
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            }
        }
        
        // 保存最后一个块
        if (currentChunk.length() > 0) {
            Map<String, Object> metadata = buildMetadata(
                filename, 
                chunkIndex, 
                -1, 
                "smart_paragraph"
            );
            metadata.put("paragraph_range", startParagraph + "-" + (paragraphs.length - 1));
            documents.add(new Document(currentChunk.toString().trim(), metadata));
        }
        
        // 更新总块数
        final int totalChunks = documents.size();
        documents.replaceAll(doc -> {
            Map<String, Object> updatedMetadata = new HashMap<>(doc.getMetadata());
            updatedMetadata.put("total_chunks", totalChunks);
            return new Document(doc.getText(), updatedMetadata);
        });
        
        return documents;
    }

    // ==================== 6. 基础字符切割 ====================

    /**
     * 基础字符切割
     */
    public List<Document> splitByCharacter(String text, String filename, SplitOptions options) {
        int chunkSize = options.getChunkSize() != null ? options.getChunkSize() : DEFAULT_CHUNK_SIZE;
        int chunkOverlap = options.getChunkOverlap() != null ? options.getChunkOverlap() : DEFAULT_CHUNK_OVERLAP;
        
        List<Document> documents = new ArrayList<>();
        String cleanedText = normalizeText(text);
        
        int textLength = cleanedText.length();
        int startIndex = 0;
        int chunkIndex = 0;
        
        while (startIndex < textLength) {
            int endIndex = Math.min(startIndex + chunkSize, textLength);
            
            if (endIndex < textLength) {
                endIndex = findBestSplitPoint(cleanedText, endIndex);
            }
            
            String chunk = cleanedText.substring(startIndex, endIndex).trim();
            
            if (!chunk.isEmpty()) {
                Map<String, Object> metadata = buildMetadata(filename, chunkIndex, -1, "character");
                metadata.put("start_char", startIndex);
                metadata.put("end_char", endIndex);
                documents.add(new Document(chunk, metadata));
                chunkIndex++;
            }
            
            startIndex = endIndex - chunkOverlap;
            if (startIndex >= endIndex) {
                startIndex = endIndex;
            }
        }
        
        // 更新总块数
        final int totalChunks = documents.size();
        documents.replaceAll(doc -> {
            Map<String, Object> updatedMetadata = new HashMap<>(doc.getMetadata());
            updatedMetadata.put("total_chunks", totalChunks);
            return new Document(doc.getText(), updatedMetadata);
        });
        
        return documents;
    }

    // ==================== 工具方法 ====================

    /**
     * 标准化文本
     */
    private String normalizeText(String text) {
        return text.replace("\r\n", "\n").replace("\r", "\n");
    }

    /**
     * 分割为句子列表
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        
        // 句子结束符正则
        Pattern pattern = Pattern.compile("[^.!?。！？]+[.!?。！？]+");
        Matcher matcher = pattern.matcher(text);
        
        int lastEnd = 0;
        while (matcher.find()) {
            // 添加句子前的文本（如果有）
            if (matcher.start() > lastEnd) {
                String before = text.substring(lastEnd, matcher.start()).trim();
                if (!before.isEmpty()) {
                    sentences.add(before);
                }
            }
            
            String sentence = matcher.group().trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            lastEnd = matcher.end();
        }
        
        // 添加剩余文本
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd).trim();
            if (!remaining.isEmpty()) {
                sentences.add(remaining);
            }
        }
        
        // 如果没有找到句子，按换行分割
        if (sentences.isEmpty()) {
            String[] lines = text.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    sentences.add(trimmed);
                }
            }
        }
        
        return sentences;
    }

    /**
     * 查找最佳切割点
     */
    private int findBestSplitPoint(String text, int targetIndex) {
        int searchStart = Math.max(targetIndex - 100, 0);
        int searchEnd = Math.min(targetIndex + 100, text.length());
        
        String searchArea = text.substring(searchStart, searchEnd);
        int targetInArea = targetIndex - searchStart;
        
        String[] sentenceEndings = {". ", "? ", "! ", "。", "？", "！", "\n"};
        int bestPoint = -1;
        
        for (String ending : sentenceEndings) {
            int index = searchArea.lastIndexOf(ending, targetInArea);
            if (index > bestPoint && index > targetInArea - 50) {
                bestPoint = index + ending.length();
            }
        }
        
        if (bestPoint == -1) {
            int spaceIndex = searchArea.lastIndexOf(" ", targetInArea);
            if (spaceIndex > targetInArea - 30) {
                bestPoint = spaceIndex + 1;
            }
        }
        
        if (bestPoint == -1) {
            bestPoint = targetInArea;
        }
        
        return searchStart + bestPoint;
    }

    /**
     * 查找单词边界
     */
    private int findWordBoundary(String text, int targetIndex) {
        // 向后查找空格或标点
        for (int i = targetIndex; i < text.length() && i < targetIndex + 20; i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c) || ".!?;:,。！？；：，".indexOf(c) >= 0) {
                return i + 1;
            }
        }
        
        // 向前查找
        for (int i = targetIndex; i > Math.max(0, targetIndex - 20); i--) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                return i + 1;
            }
        }
        
        return targetIndex;
    }

    /**
     * 获取重叠文本
     */
    private String getOverlapText(String text, int overlapSize) {
        if (text.length() <= overlapSize) {
            return text;
        }
        
        // 尝试在句子边界切割
        String overlap = text.substring(text.length() - overlapSize);
        int sentenceStart = overlap.indexOf(". ") + 2;
        if (sentenceStart > 2 && sentenceStart < overlapSize / 2) {
            return overlap.substring(sentenceStart);
        }
        
        return overlap;
    }

    /**
     * 构建基础元数据
     */
    private Map<String, Object> buildMetadata(
            String filename, 
            int chunkIndex, 
            int totalChunks, 
            String splitMethod) {
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", filename);
        metadata.put("chunk_index", chunkIndex);
        metadata.put("total_chunks", totalChunks);
        metadata.put("split_method", splitMethod);
        metadata.put("timestamp", System.currentTimeMillis());
        
        return metadata;
    }

    // ==================== 内部类 ====================

    /**
     * 切割选项类
     */
    public static class SplitOptions {
        private Integer chunkSize;
        private Integer chunkOverlap;
        private Integer maxTokens;
        private Double semanticThreshold;
        private Map<String, Object> customMetadata;

        public SplitOptions() {}

        public Integer getChunkSize() { return chunkSize; }
        public SplitOptions withChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public Integer getChunkOverlap() { return chunkOverlap; }
        public SplitOptions withChunkOverlap(int chunkOverlap) {
            this.chunkOverlap = chunkOverlap;
            return this;
        }

        public Integer getMaxTokens() { return maxTokens; }
        public SplitOptions withMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Double getSemanticThreshold() { return semanticThreshold; }
        public SplitOptions withSemanticThreshold(double threshold) {
            this.semanticThreshold = threshold;
            return this;
        }

        public Map<String, Object> getCustomMetadata() { return customMetadata; }
        public SplitOptions withCustomMetadata(Map<String, Object> metadata) {
            this.customMetadata = metadata;
            return this;
        }
    }

    /**
     * 文本块内部类
     */
    private static class TextChunk {
        private final String content;
        private final String separator;

        public TextChunk(String content, String separator) {
            this.content = content;
            this.separator = separator;
        }

        public String getContent() { return content; }
        public String getSeparator() { return separator; }
    }

    /**
     * Markdown 章节内部类
     */
    private static class MarkdownSection {
        private final String type;
        private final String content;
        private final int headingLevel;
        private final String headingText;

        public MarkdownSection(String type, String content, int headingLevel, String headingText) {
            this.type = type;
            this.content = content;
            this.headingLevel = headingLevel;
            this.headingText = headingText;
        }

        public String getType() { return type; }
        public String getContent() { return content; }
        public int getHeadingLevel() { return headingLevel; }
        public String getHeadingText() { return headingText; }
        public boolean isHeading() { return "heading".equals(type); }
    }
}
