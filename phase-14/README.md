# 揭秘 Spring AI 文档切割：从"暴力分割"到"语义智能"的进阶之路

> 📦 **项目源码**：[https://github.com/XiFYuW/spring-ai-course/tree/main/phase-14](https://github.com/XiFYuW/spring-ai-course/tree/main/phase-14)

---

## 引言

在构建 RAG（检索增强生成）系统时，**文档切割（Document Splitting）** 是一个经常被忽视但至关重要的环节。切割质量直接决定了：

- **检索精度**：块太大包含噪声，块太小丢失上下文
- **生成质量**：上下文窗口的利用率
- **存储成本**：冗余内容和向量数量

本文将带你从最简单的字符切割，逐步深入到**递归字符切割**、**Markdown 结构切割**、**Token 感知切割**、**语义切割**等 6 种高级策略，手把手教你用 Spring AI 实现企业级的文档处理系统。

**读完本文，你将收获**：
- 理解不同切割策略的适用场景和优缺点
- 掌握 Spring AI 文档切割的完整实现
- 学会根据业务场景选择最优策略

---

## 目录

1. [为什么文档切割如此重要？](#一为什么文档切割如此重要)
2. [6 种文档切割策略详解](#二6-种文档切割策略详解)
3. [Spring AI 实战：从零实现智能切割服务](#三spring-ai-实战从零实现智能切割服务)
4. [策略对比与选型指南](#四策略对比与选型指南)
5. [性能优化与避坑指南](#五性能优化与避坑指南)
6. [总结与扩展思考](#六总结与扩展思考)

---

## 一、为什么文档切割如此重要？

### 1.1 切割质量对 RAG 系统的影响

```
┌─────────────────────────────────────────────────────────────┐
│                    RAG 系统架构                              │
├─────────────────────────────────────────────────────────────┤
│  文档 → [文档切割] → 文本块 → [Embedding] → 向量存储          │
│                                      ↓                      │
│  用户查询 ──────────────────────→ [相似度检索] → 上下文       │
│                                                      ↓      │
│                                              [LLM 生成]     │
└─────────────────────────────────────────────────────────────┘
```

**切割不当的后果**：

| 问题 | 后果 | 示例 |
|------|------|------|
| 块太大 | 包含无关信息，稀释语义 | "Spring 框架介绍 + 数据库配置 + 缓存优化"混在一起 |
| 块太小 | 丢失上下文，语义不完整 | "它提供了依赖注入功能"——"它"指代不明 |
| 切割位置不当 | 句子被截断，信息断裂 | "Spring Boot 的自动配"（配置被截断） |

### 1.2 理想切割的标准

- ✅ **语义完整性**：每个块包含完整的语义单元
- ✅ **上下文保留**：相邻块之间有适当的重叠
- ✅ **长度适中**：适配 Embedding 模型和 LLM 的上下文限制
- ✅ **结构感知**：保留文档的层级结构（标题、段落等）

---

## 二、6 种文档切割策略详解

### 2.1 策略总览

| 策略 | 核心思想 | 适用场景 | 复杂度 |
|------|----------|----------|--------|
| **CHARACTER** | 固定字符数切割 | 简单文本、快速原型 | ⭐ |
| **RECURSIVE** | 按语义层级递归分割 | 通用文本、LangChain 推荐 | ⭐⭐ |
| **MARKDOWN** | 识别 Markdown 结构 | 技术文档、README | ⭐⭐ |
| **TOKEN** | 按 Token 数量切割 | 精确控制 LLM 上下文 | ⭐⭐⭐ |
| **SEMANTIC** | 基于 Embedding 相似度 | 高质量语义检索 | ⭐⭐⭐⭐ |
| **SMART_PARAGRAPH** | 段落+字符混合 | 平衡效率和质量 | ⭐⭐ |

### 2.2 递归字符切割（RECURSIVE）

**核心思想**：按分隔符优先级尝试分割，优先保持语义完整性

```
分隔符优先级（从高到低）：
段落(\n\n) → 换行(\n) → 句子(.!?。！？) → 分号(;；) → 逗号(,，) → 空格( ) → 字符
```

**工作流程**：

```java
// 1. 尝试按段落分割
"第一章\n\n第一节内容...\n\n第二节内容..."
    ↓
["第一章", "第一节内容...", "第二节内容..."]

// 2. 如果某个块仍太大，递归使用下一个分隔符
"第一节内容..." (长度 > 1000)
    ↓ 按句子分割
["第一节介绍了 Spring Boot 的核心概念。", "它简化了配置过程。", ...]

// 3. 如果句子还太长，继续递归
"第一节介绍了 Spring Boot 的核心概念..." (长度 > 1000)
    ↓ 按逗号分割
["第一节介绍了 Spring Boot 的核心概念", "包括自动配置、起步依赖等", ...]
```

**代码实现**：

```java
public List<Document> splitRecursive(String text, String filename, SplitOptions options) {
    String[] separators = {
        "\n\n", "\n", ". ", "? ", "! ", "。", "？", "！", 
        "; ", "；", ", ", "，", " ", ""
    };
    
    // 递归分割实现
    return recursiveSplitInternal(text, 0, chunkSize, chunkOverlap, 0);
}
```

### 2.3 Markdown 结构切割（MARKDOWN）

**核心思想**：识别 Markdown 的标题层级，保留文档结构

```markdown
# 项目介绍          ← H1
## 安装指南         ← H2
### 环境要求        ← H3
内容...
### 安装步骤        ← H3
内容...
## 使用说明         ← H2
内容...
```

**切割结果**：

```java
// Chunk 1
{
  "text": "# 项目介绍\n\n这是项目的详细介绍...",
  "metadata": {
    "heading_level": 1,
    "heading_text": "项目介绍",
    "section_type": "heading"
  }
}

// Chunk 2
{
  "text": "## 安装指南\n\n### 环境要求\n需要 Java 17...",
  "metadata": {
    "heading_level": 2,
    "heading_text": "安装指南",
    "section_type": "heading"
  }
}
```

**优势**：
- 保留文档层级结构
- 检索时可以精确定位到章节
- 支持代码块、列表等特殊元素

### 2.4 Token 感知切割（TOKEN）

**核心思想**：基于 Token 数量而非字符数切割，精确适配 LLM 限制

**Token 估算规则**：

```java
private int estimateTokens(String text) {
    int chineseChars = countChinese(text);  // 中文 ~1.67 字符/token
    int englishChars = countEnglish(text);  // 英文 ~4 字符/token
    int numbers = countNumbers(text);       // 数字 ~3.3 字符/token
    
    return (int) Math.ceil(
        chineseChars * 0.6 +
        englishChars * 0.25 +
        numbers * 0.3
    );
}
```

**使用场景**：
- GPT-4 上下文限制 8K/32K/128K
- Claude 100K 上下文
- 需要精确控制输入长度

### 2.5 语义切割（SEMANTIC）

**核心思想**：在语义变化处切割，而非固定长度

**工作流程**：

```
文本: "Spring Boot 简化了 Java 开发。它提供了自动配置功能。今天天气很好。机器学习是 AI 的核心技术。"

Step 1: 分割为句子
["Spring Boot 简化了 Java 开发。", "它提供了自动配置功能。", "今天天气很好。", "机器学习是 AI 的核心技术。"]

Step 2: 计算相邻句子 Embedding 相似度
句子1 ↔ 句子2: 0.92 (高相似度，同主题)
句子2 ↔ 句子3: 0.31 (低相似度，主题切换)
句子3 ↔ 句子4: 0.28 (低相似度，主题切换)

Step 3: 在相似度低于阈值处切割
Chunk 1: "Spring Boot 简化了 Java 开发。它提供了自动配置功能。"
Chunk 2: "今天天气很好。"
Chunk 3: "机器学习是 AI 的核心技术。"
```

**优势**：
- 每个块包含同一主题的完整内容
- 检索时语义相关性更高
- 避免话题切换导致的语义断裂

**成本考量**：
- 需要调用 Embedding API
- 适合高质量检索场景
- 不适合实时处理大量文档

---

## 三、Spring AI 实战：从零实现智能切割服务

### 3.1 项目结构

```
src/main/java/org/example/
├── service/
│   ├── DocumentSplitterService.java    # 核心切割服务
│   ├── FileProcessingService.java      # 文件处理服务
│   └── VectorStoreService.java         # 向量存储服务
├── controller/
│   └── VectorStoreController.java      # REST API
└── Application.java
```

### 3.2 核心实现：DocumentSplitterService

#### 3.2.1 策略枚举定义

```java
@Service
public class DocumentSplitterService {
    
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
}
```

#### 3.2.2 统一入口方法

```java
/**
 * 统一文档切割入口 - 带选项
 */
public List<Document> split(String text, String filename, 
                            SplitStrategy strategy, 
                            SplitOptions options) {
    if (text == null || text.isEmpty()) {
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
```

#### 3.2.3 切割选项类

```java
/**
 * 切割选项类 - 支持链式调用
 */
public static class SplitOptions {
    private Integer chunkSize;        // 块大小（字符数）
    private Integer chunkOverlap;     // 重叠大小
    private Integer maxTokens;        // 最大 Token 数
    private Double semanticThreshold; // 语义相似度阈值

    public SplitOptions withChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    public SplitOptions withChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
        return this;
    }

    public SplitOptions withMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public SplitOptions withSemanticThreshold(double threshold) {
        this.semanticThreshold = threshold;
        return this;
    }
}
```

### 3.3 递归字符切割实现

```java
/**
 * 递归字符切割 - LangChain 推荐策略
 */
public List<Document> splitRecursive(String text, String filename, 
                                     SplitOptions options) {
    int chunkSize = options.getChunkSize() != null ? 
                    options.getChunkSize() : DEFAULT_CHUNK_SIZE;
    int chunkOverlap = options.getChunkOverlap() != null ? 
                       options.getChunkOverlap() : DEFAULT_CHUNK_OVERLAP;
    
    String cleanedText = normalizeText(text);
    
    // 递归分割
    List<TextChunk> chunks = recursiveSplitInternal(
        cleanedText, 0, chunkSize, chunkOverlap, 0
    );
    
    // 转换为 Document
    List<Document> documents = new ArrayList<>();
    for (int i = 0; i < chunks.size(); i++) {
        TextChunk chunk = chunks.get(i);
        Map<String, Object> metadata = Map.of(
            "source", filename,
            "chunk_index", i,
            "total_chunks", chunks.size(),
            "split_method", "recursive",
            "separator_used", chunk.getSeparator()
        );
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
        result.add(new TextChunk(text, 
            depth > 0 ? RECURSIVE_SEPARATORS[separatorIndex - 1] : "none"));
        return result;
    }
    
    // 如果已经用完所有分隔符，强制按字符切割
    if (separatorIndex >= RECURSIVE_SEPARATORS.length) {
        return forceCharacterSplit(text, chunkSize, chunkOverlap);
    }
    
    String separator = RECURSIVE_SEPARATORS[separatorIndex];
    String[] parts = separator.isEmpty() ? 
                     text.split("") : 
                     text.split(Pattern.quote(separator));
    
    StringBuilder currentChunk = new StringBuilder();
    
    for (int i = 0; i < parts.length; i++) {
        String part = parts[i];
        String withSeparator = i < parts.length - 1 ? 
                               part + separator : part;
        
        // 如果单个部分就超过限制，需要递归分割
        if (part.length() > chunkSize) {
            if (currentChunk.length() > 0) {
                result.add(new TextChunk(
                    currentChunk.toString().trim(), separator));
                currentChunk = new StringBuilder();
            }
            
            List<TextChunk> subChunks = recursiveSplitInternal(
                part, separatorIndex + 1, chunkSize, chunkOverlap, depth + 1
            );
            result.addAll(subChunks);
        }
        // 如果添加这个部分会超过限制，先保存当前块
        else if (currentChunk.length() + withSeparator.length() > chunkSize 
                && currentChunk.length() > 0) {
            result.add(new TextChunk(
                currentChunk.toString().trim(), separator));
            
            // 考虑重叠
            if (chunkOverlap > 0 && currentChunk.length() > chunkOverlap) {
                String overlapText = currentChunk.substring(
                    currentChunk.length() - chunkOverlap);
                currentChunk = new StringBuilder(overlapText)
                    .append(withSeparator);
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
        result.add(new TextChunk(
            currentChunk.toString().trim(), separator));
    }
    
    return result;
}
```

### 3.4 语义切割实现

```java
/**
 * 语义切割 - 基于 Embedding 相似度
 */
public List<Document> splitSemantic(String text, String filename, 
                                    SplitOptions options) {
    double threshold = options.getSemanticThreshold() != null ? 
                       options.getSemanticThreshold() : DEFAULT_SEMANTIC_THRESHOLD;
    int maxTokens = options.getMaxTokens() != null ? 
                    options.getMaxTokens() : DEFAULT_MAX_TOKENS * 2;
    
    // 1. 分割为句子
    List<String> sentences = splitIntoSentences(text);
    if (sentences.size() <= 1) {
        return List.of(new Document(text, Map.of(
            "source", filename,
            "split_method", "semantic"
        )));
    }
    
    // 2. 计算每个句子的 Embedding
    List<float[]> embeddings = new ArrayList<>();
    for (String sentence : sentences) {
        float[] embedding = embeddingModel.embed(sentence);
        embeddings.add(embedding);
    }
    
    // 3. 计算相邻句子的相似度，确定切割点
    List<Integer> splitPoints = new ArrayList<>();
    splitPoints.add(0);
    
    for (int i = 0; i < embeddings.size() - 1; i++) {
        double similarity = cosineSimilarity(embeddings.get(i), 
                                              embeddings.get(i + 1));
        if (similarity < threshold) {
            splitPoints.add(i + 1);
        }
    }
    splitPoints.add(sentences.size());
    
    // 4. 根据切割点构建文档块
    List<Document> documents = new ArrayList<>();
    for (int i = 0; i < splitPoints.size() - 1; i++) {
        int startIdx = splitPoints.get(i);
        int endIdx = splitPoints.get(i + 1);
        
        StringBuilder chunk = new StringBuilder();
        for (int j = startIdx; j < endIdx; j++) {
            if (chunk.length() > 0) chunk.append(" ");
            chunk.append(sentences.get(j));
        }
        
        Map<String, Object> metadata = Map.of(
            "source", filename,
            "chunk_index", i,
            "split_method", "semantic",
            "sentence_range", startIdx + "-" + (endIdx - 1),
            "estimated_tokens", estimateTokens(chunk.toString())
        );
        
        documents.add(new Document(chunk.toString(), metadata));
    }
    
    return documents;
}

/**
 * 计算余弦相似度
 */
private double cosineSimilarity(float[] vec1, float[] vec2) {
    double dotProduct = 0.0;
    double norm1 = 0.0;
    double norm2 = 0.0;
    
    for (int i = 0; i < vec1.length; i++) {
        dotProduct += vec1[i] * vec2[i];
        norm1 += vec1[i] * vec1[i];
        norm2 += vec2[i] * vec2[i];
    }
    
    return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
}
```

### 3.5 REST API 使用示例

```java
@RestController
@RequestMapping("/api/vector-store")
public class VectorStoreController {

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Mono<ResponseEntity<ApiResponse<FileUploadResponse>>> uploadFile(
            @RequestPart("file") FilePart filePart,
            @RequestParam(name = "splitStrategy", defaultValue = "RECURSIVE") 
                DocumentSplitterService.SplitStrategy splitStrategy,
            @RequestParam(name = "chunkSize", defaultValue = "1000") int chunkSize,
            @RequestParam(name = "chunkOverlap", defaultValue = "200") int chunkOverlap) {
        
        return fileProcessingService.processAndStore(
                filePart, splitStrategy, chunkSize, chunkOverlap)
            .map(result -> ResponseEntity.ok(
                ApiResponse.success("File processed", result)));
    }
}
```

**API 调用示例**：

```bash
# 1. 递归字符切割（推荐通用场景）
curl -X POST "http://localhost:8080/api/vector-store/upload" \
  -F "file=@document.txt" \
  -F "splitStrategy=RECURSIVE" \
  -F "chunkSize=1000" \
  -F "chunkOverlap=200"

# 2. Markdown 结构切割（技术文档）
curl -X POST "http://localhost:8080/api/vector-store/upload" \
  -F "file=@readme.md" \
  -F "splitStrategy=MARKDOWN" \
  -F "chunkSize=1500"

# 3. Token 感知切割（精确控制）
curl -X POST "http://localhost:8080/api/vector-store/upload" \
  -F "file=@document.txt" \
  -F "splitStrategy=TOKEN" \
  -F "maxTokens=512"

# 4. 语义切割（高质量检索）
curl -X POST "http://localhost:8080/api/vector-store/upload" \
  -F "file=@article.txt" \
  -F "splitStrategy=SEMANTIC" \
  -F "semanticThreshold=0.7"
```

---

## 四、策略对比与选型指南

### 4.1 策略对比表

| 维度 | CHARACTER | RECURSIVE | MARKDOWN | TOKEN | SEMANTIC |
|------|-----------|-----------|----------|-------|----------|
| **速度** | ⚡⚡⚡ | ⚡⚡ | ⚡⚡ | ⚡⚡ | ⚡ |
| **语义完整性** | ⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| **结构保留** | ❌ | ❌ | ✅ | ❌ | ❌ |
| **LLM 适配** | ⭐⭐ | ⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **实现复杂度** | 简单 | 中等 | 中等 | 中等 | 复杂 |
| **API 成本** | 无 | 无 | 无 | 无 | 高 |

### 4.2 场景选型决策树

```
开始
  │
  ├─ 文档是 Markdown 格式？
  │     ├─ 是 → 使用 MARKDOWN 策略
  │     └─ 否 → 继续
  │
  ├─ 需要精确控制 Token 数量？
  │     ├─ 是 → 使用 TOKEN 策略
  │     └─ 否 → 继续
  │
  ├─ 追求最高检索质量且预算充足？
  │     ├─ 是 → 使用 SEMANTIC 策略
  │     └─ 否 → 继续
  │
  ├─ 追求速度与质量的平衡？
  │     ├─ 是 → 使用 RECURSIVE 策略（推荐）
  │     └─ 否 → 使用 CHARACTER 策略
```

### 4.3 推荐配置

| 场景 | 推荐策略 | 参数配置 |
|------|----------|----------|
| 通用文本处理 | RECURSIVE | chunkSize=1000, overlap=200 |
| 技术文档/手册 | MARKDOWN | chunkSize=1500 |
| GPT-4 上下文 | TOKEN | maxTokens=2000 |
| 高质量知识库 | SEMANTIC | threshold=0.7, maxTokens=1000 |
| 快速原型 | CHARACTER | chunkSize=500, overlap=50 |

---

## 五、性能优化与避坑指南

### 5.1 常见问题与解决方案

#### 问题 1：语义切割 API 调用过多

**现象**：处理大文档时 Embedding API 费用激增

**解决方案**：
```java
// 方案 1：采样计算（每 N 个句子计算一次）
for (int i = 0; i < sentences.size() - 1; i += 3) {
    double similarity = cosineSimilarity(embeddings.get(i), 
                                          embeddings.get(i + 1));
}

// 方案 2：先使用 RECURSIVE 预分割，再对大块使用 SEMANTIC
List<Document> preChunks = splitRecursive(text, filename, options);
for (Document chunk : preChunks) {
    if (estimateTokens(chunk.getText()) > 1000) {
        // 只对大块使用语义切割
        semanticSubChunks.addAll(splitSemantic(chunk.getText(), filename, options));
    }
}
```

#### 问题 2：中文文本切割位置不当

**现象**：中文句子被截断，如 "Spring Boot 的自"（动配置被截断）

**解决方案**：
```java
// 确保分隔符列表包含中文标点
private static final String[] RECURSIVE_SEPARATORS = {
    "\n\n", "\n",
    "。", "？", "！",      // 中文标点优先
    ". ", "? ", "! ",      // 英文标点
    "；", "，",
    "; ", ", ",
    " ", ""
};
```

#### 问题 3：块重叠导致重复检索

**现象**：搜索结果中出现重复内容

**解决方案**：
```java
// 元数据标记重叠区域
metadata.put("is_overlap", true);
metadata.put("parent_chunk", parentIndex);

// 检索时去重
documents.stream()
    .filter(doc -> !Boolean.TRUE.equals(doc.getMetadata().get("is_overlap")))
    .collect(Collectors.toList());
```

### 5.2 性能优化技巧

| 技巧 | 效果 | 实现方式 |
|------|------|----------|
| **并行处理** | 提升 3-5 倍 | 使用 `parallelStream()` 处理多个文档 |
| **批量 Embedding** | 减少 API 调用 | 一次请求多个句子的 Embedding |
| **缓存切割结果** | 避免重复计算 | 使用 Caffeine 缓存切割后的文档 |
| **预编译正则** | 提升 20% 性能 | 将 `Pattern` 定义为静态常量 |

```java
// 并行处理示例
public List<Document> batchSplit(List<File> files, SplitStrategy strategy) {
    return files.parallelStream()
        .flatMap(file -> {
            String content = readFile(file);
            return split(content, file.getName(), strategy, new SplitOptions())
                .stream();
        })
        .collect(Collectors.toList());
}
```

---

## 六、总结与扩展思考

### 6.1 核心要点回顾

1. **文档切割是 RAG 系统的关键环节**，直接影响检索和生成质量
2. **递归字符切割（RECURSIVE）** 是最佳通用选择，平衡了速度和质量
3. **语义切割（SEMANTIC）** 提供最高质量，但成本较高
4. **根据场景选择策略**，没有"银弹"

### 6.2 扩展思考

**可以在此基础上增加的功能**：

1. **多模态切割**：支持 PDF、Word、HTML 等格式的结构化提取
2. **动态块大小**：根据内容复杂度自适应调整块大小
3. **实体感知切割**：使用 NER 识别实体，避免在实体中间切割
4. **层级索引**：构建文档的层级索引（章→节→段→句）

**可以优化的性能点**：

1. **增量切割**：只处理变更的部分，避免全量重新切割
2. **分布式处理**：使用消息队列分发切割任务
3. **智能缓存**：基于内容哈希缓存切割结果

### 6.3 完整代码仓库

本文完整代码已开源，包含：
- ✅ 6 种切割策略完整实现
- ✅ REST API 接口
- ✅ 单元测试和集成测试
- ✅ 性能基准测试

---

## 附录：参考资料

1. [LangChain Text Splitters 文档](https://python.langchain.com/docs/modules/data_connection/document_transformers/)
2. [OpenAI Embedding 最佳实践](https://platform.openai.com/docs/guides/embeddings)
3. [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
4. [Elasticsearch 向量搜索](https://www.elastic.co/guide/en/elasticsearch/reference/current/knn-search.html)

---

**原创声明**：本文为原创教程，转载请注明出处。

<!-- 互动提示 -->
欢迎在评论区交流讨论！你更倾向于使用哪种切割策略？

💰 **为什么选择 32ai？**
**低至 0.56 : 1 比率**
🔗 **快速访问**: [点击访问](https://ai.32zi.com) — 直连、无需魔法。
