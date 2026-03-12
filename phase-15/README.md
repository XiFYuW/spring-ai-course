# Spring AI 11 种文档切割策略全解析

> 本文将深入讲解 Spring AI 中 11 种文档切割策略的原理、适用场景和实战用法，特别重点介绍 5 种高级切割策略。

> 📦 **项目源码**：[https://github.com/XiFYuW/spring-ai-course/tree/main/phase-15](https://github.com/XiFYuW/spring-ai-course/tree/main/phase-15)

---

## 目录

- [一、前言](#一前言)
- [二、环境准备](#二环境准备)
- [三、基础策略回顾](#三基础策略回顾)
- [四、新策略详解](#四新策略详解)
  - [4.1 Agentic 切割 - 智能主题边界](#41-agentic-切割---智能主题边界)
  - [4.2 代码感知切割 - 程序员的福音](#42-代码感知切割---程序员的福音)
  - [4.3 层次结构切割 - 文档树构建](#43-层次结构切割---文档树构建)
  - [4.4 滑动窗口切割 - RAG 神器](#44-滑动窗口切割---rag-神器)
  - [4.5 结构化数据切割 - 数据文件专用](#45-结构化数据切割---数据文件专用)
- [五、策略选择指南](#五策略选择指南)
- [六、API 使用示例](#六api-使用示例)
- [七、性能优化建议](#七性能优化建议)
- [八、总结](#八总结)

---

## 一、前言

### 1.1 为什么文档切割如此重要？

在 RAG（检索增强生成）系统中，文档切割（Document Splitting）是**最关键的前置步骤**之一：

- ✅ **影响检索精度**：切割得当，检索更准确
- ✅ **影响上下文完整性**：保持语义连贯性
- ✅ **影响 Token 利用率**：避免浪费 LLM 的上下文窗口
- ✅ **影响响应质量**：好的切割 = 更好的回答

### 1.2 本文将收获什么？

读完本文，你将掌握：

1. **11 种切割策略**的核心原理和适用场景
2. **5 种新高级策略**的详细使用方法
3. 如何根据**文档类型选择最佳策略**
4. 实战中的**性能优化技巧**

---

## 二、环境准备

### 2.1 项目依赖

```xml
<dependencies>
    <!-- Spring AI OpenAI -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>
    
    <!-- Spring AI Elasticsearch Vector Store -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-vector-store-elasticsearch</artifactId>
    </dependency>
    
    <!-- WebFlux -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
</dependencies>
```

### 2.2 配置文件

```yaml
# application.yml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      embedding:
        options:
          model: text-embedding-3-small
    vectorstore:
      elasticsearch:
        index-name: document-store
        dimensions: 1536
```

---

## 三、基础策略回顾

在深入新策略之前，先快速回顾 6 种基础策略：

| 策略 | 特点 | 适用场景 |
|------|------|----------|
| **RECURSIVE** | 递归字符切割，保持语义完整性 | 通用文本 |
| **MARKDOWN** | 识别标题层级，保留文档结构 | Markdown 文档 |
| **TOKEN** | 估算 Token 数量，适配 LLM 限制 | 需要精确控制 Token |
| **SEMANTIC** | 基于 Embedding 相似度切割 | 高质量语义切割 |
| **SMART_PARAGRAPH** | 段落+字符混合切割 | 长段落文档 |
| **CHARACTER** | 固定字符切割，简单快速 | 快速处理 |

---

## 四、新策略详解

### 4.1 Agentic 切割 - 智能主题边界

#### 4.1.1 核心思想

Agentic 切割借鉴了 LLM Agent 的理念，**使用启发式规则在主题边界处进行切割**，确保每个块都围绕一个完整的主题。

#### 4.1.2 实现原理

```java
/**
 * Agentic 切割核心逻辑
 * 
 * 1. 首先使用递归切割获得初步块
 * 2. 识别主题转换标记词
 * 3. 在主题边界处优化切割点
 */
public List<Document> splitAgentic(String text, String filename, SplitOptions options) {
    // 第一步：递归切割获得初步块
    List<TextChunk> initialChunks = recursiveSplitInternal(
        normalizeText(text),
        0,
        chunkSize * 2, // 更大的初始块
        chunkOverlap,
        0
    );
    
    // 第二步：使用启发式规则优化切割点
    for (TextChunk chunk : initialChunks) {
        // 识别主题转换标记词
        String optimizedContent = optimizeChunkBoundary(content, chunkSize);
        // ...
    }
}
```

#### 4.1.3 主题标记词识别

```java
// 主题转换标记词（中英文）
String[] topicMarkers = {
    // 标点符号
    "\n\n", "\n", ". ", "? ", "! ", "。", "？", "！",
    // 中文序列词
    "首先", "其次", "然后", "接下来", "最后",
    "第一", "第二", "第三", "另一方面", "此外", "总之"
};
```

#### 4.1.4 使用示例

```bash
# 上传长文档，使用 Agentic 切割
curl -X POST "http://localhost:8080/api/vector-store/upload?splitStrategy=AGENTIC&chunkSize=1500" \
  -F "file=@long-article.txt"
```

**返回的元数据：**

```json
{
  "split_method": "agentic",
  "original_separator": "\n\n",
  "optimization_applied": true,
  "chunk_group": 0
}
```

#### 4.1.5 适用场景

- ✅ **长篇文章**：确保每个块围绕一个主题
- ✅ **技术文档**：保持概念完整性
- ✅ **论文/报告**：避免在论证中间切割

---

### 4.2 代码感知切割 - 程序员的福音

#### 4.2.1 核心思想

代码感知切割专门针对**源代码文件**，能够识别：

- 函数/方法边界
- 类/接口/枚举定义
- Import/依赖语句
- 代码注释

#### 4.2.2 支持的语言

| 语言 | 扩展名 | 识别能力 |
|------|--------|----------|
| Java | `.java` | 类、接口、枚举、方法、注解 |
| Python | `.py` | 类、函数、装饰器 |
| JavaScript/TypeScript | `.js`, `.ts` | 函数、类、箭头函数 |
| Go | `.go` | 函数、结构体、接口 |
| Rust | `.rs` | 函数、结构体、枚举、trait |
| C/C++ | `.c`, `.cpp`, `.cc` | 函数、类、结构体 |
| C# | `.cs` | 类、接口、方法、属性 |
| PHP | `.php` | 类、函数、命名空间 |
| Ruby | `.rb` | 类、方法、模块 |
| Swift | `.swift` | 类、函数、结构体 |
| Kotlin | `.kt` | 类、函数、接口 |
| Scala | `.scala` | 类、特质、函数 |

#### 4.2.3 实现原理

```java
/**
 * 代码感知切割
 * 
 * 1. 检测编程语言
 * 2. 解析代码块（类、方法等）
 * 3. 提取 import 语句
 * 4. 按代码结构切割
 */
public List<Document> splitCodeAware(String text, String filename, SplitOptions options) {
    // 检测语言
    String language = detectLanguage(filename);
    
    // 解析代码块
    List<CodeBlock> codeBlocks = parseCodeBlocks(text, language);
    
    // 提取 imports
    List<String> imports = extractImports(text, language);
    
    // 构建文档
    for (CodeBlock block : codeBlocks) {
        Map<String, Object> metadata = buildCodeMetadata(
            filename, chunkIndex, block, imports, language
        );
        // ...
    }
}
```

#### 4.2.4 代码块解析示例

```java
// Java 代码块解析正则
Pattern pattern = Pattern.compile(
    "(?:(public|private|protected|static|final|abstract|class|interface|enum|record|void|@\\w+)\\s+)?" +
    "(?:<[^>]+>\\s*)?" +
    "(\\w+)\\s*\\([^)]*\\)\\s*(?:throws\\s+\\w+(?:\\s*,\\s*\\w+)*)?\\s*\\{[^}]*\\}" +
    "|(?:public|private|protected)?\\s*(?:static\\s+)?(?:final\\s+)?(?:abstract\\s+)?" +
    "(?:class|interface|enum|record)\\s+(\\w+)",
    Pattern.DOTALL
);
```

#### 4.2.5 使用示例

```bash
# 上传 Java 文件
curl -X POST "http://localhost:8080/api/vector-store/upload?splitStrategy=CODE_AWARE" \
  -F "file=@UserService.java"

# 上传 Python 文件
curl -X POST "http://localhost:8080/api/vector-store/upload?splitStrategy=CODE_AWARE" \
  -F "file=@main.py"
```

**返回的元数据：**

```json
{
  "split_method": "code_aware",
  "language": "java",
  "block_type": "method",
  "block_name": "getUserById",
  "start_line": 45,
  "imports_count": 12,
  "has_imports": true
}
```

#### 4.2.6 适用场景

- ✅ **代码库 RAG**：构建代码知识库
- ✅ **代码审查**：按方法检索相关代码
- ✅ **技术文档**：配合代码示例的文档

---

### 4.3 层次结构切割 - 文档树构建

#### 4.3.1 核心思想

层次结构切割将文档构建成**树状结构**：

```
文档 (Root)
├── 章节 1 (Chapter)
│   ├── 小节 1.1 (Section)
│   │   ├── 段落 1 (Paragraph)
│   │   └── 段落 2 (Paragraph)
│   └── 小节 1.2 (Section)
│       └── 段落 3 (Paragraph)
└── 章节 2 (Chapter)
    └── 小节 2.1 (Section)
        └── 段落 4 (Paragraph)
```

#### 4.3.2 实现原理

```java
/**
 * 层次结构切割
 * 
 * 构建文档树：文档 → 章节 → 小节 → 段落
 */
public List<Document> splitHierarchical(String text, String filename, SplitOptions options) {
    // 构建层次树
    DocumentNode root = buildHierarchicalTree(text, filename);
    
    // 展平树为文档列表
    List<Document> documents = new ArrayList<>();
    flattenTree(root, documents, filename, 0, maxChunkSize);
    
    return documents;
}

/**
 * 构建层次树
 */
private DocumentNode buildHierarchicalTree(String text, String filename) {
    DocumentNode root = new DocumentNode("root", "document", text, 0, 0);
    
    // 第一层：按章节分割
    String[] chapters = text.split(
        "(?=(?:^|\\n)\\s*(?:第[一二三四五六七八九十\\d]+[章节篇]|Chapter\\s+\\d+|\\d+\\.\\s+[^\\n]+))"
    );
    
    // 第二层：按小节分割
    // 第三层：按段落分割
    // ...
}
```

#### 4.3.3 章节识别模式

| 模式 | 示例 |
|------|------|
| 中文章节 | `第一章`, `第二节`, `第三篇` |
| 英文章节 | `Chapter 1`, `Chapter 2` |
| 数字章节 | `1. 引言`, `2. 背景` |

#### 4.3.4 使用示例

```bash
# 上传结构化长文档
curl -X POST "http://localhost:8080/api/vector-store/upload?splitStrategy=HIERARCHICAL&chunkSize=2000" \
  -F "file=@book.txt"
```

**返回的元数据：**

```json
{
  "split_method": "hierarchical",
  "node_id": "section_0_1",
  "node_type": "section",
  "hierarchy_level": 2,
  "node_index": 1,
  "has_children": true,
  "children_count": 3
}
```

#### 4.3.5 适用场景

- ✅ **书籍/教材**：保持章节结构
- ✅ **技术手册**：按章节检索
- ✅ **法律文档**：保持条款层次
- ✅ **论文**：保持章节逻辑

---

### 4.4 滑动窗口切割 - RAG 神器

#### 4.4.1 核心思想

滑动窗口切割使用**高重叠率**（默认 50%）的窗口滑动切割文本，确保：

- ✅ **无信息遗漏**：每个位置都被多个窗口覆盖
- ✅ **上下文连续性**：相邻块有大量重叠
- ✅ **检索召回率提升**：适合 RAG 场景

#### 4.4.2 实现原理

```java
/**
 * 滑动窗口切割
 * 
 * 特点：
 * - 高重叠率（默认 50%）
 * - 确保没有信息遗漏
 * - 适合检索增强生成 (RAG)
 */
public List<Document> splitSlidingWindow(String text, String filename, SplitOptions options) {
    int windowSize = options.getChunkSize() != null ? options.getChunkSize() : DEFAULT_CHUNK_SIZE;
    int overlapRatio = options.getChunkOverlap() != null ? options.getChunkOverlap() : 50;
    
    // 计算步长
    int stride = Math.max(1, windowSize * (100 - overlapRatio) / 100);
    
    while (startIndex < normalizedText.length()) {
        int endIndex = Math.min(startIndex + windowSize, normalizedText.length());
        
        // 尝试在句子边界结束
        if (endIndex < normalizedText.length()) {
            endIndex = findBestSplitPoint(normalizedText, endIndex);
        }
        
        // 滑动窗口
        startIndex += stride;
    }
}
```

#### 4.4.3 重叠率计算

| 窗口大小 | 重叠率 | 步长 | 效果 |
|----------|--------|------|------|
| 1000 | 50% | 500 | 中等重叠 |
| 1000 | 70% | 300 | 高重叠，高召回 |
| 1000 | 30% | 700 | 低重叠，高效率 |

#### 4.4.4 使用示例

```bash
# 高重叠率（70%）- 适合高精度检索
curl -X POST "http://localhost:8080/api/vector-store/upload?splitStrategy=SLIDING_WINDOW&chunkSize=1000&chunkOverlap=70" \
  -F "file=@document.txt"

# 中等重叠率（50%）- 平衡方案
curl -X POST "http://localhost:8080/api/vector-store/upload?splitStrategy=SLIDING_WINDOW&chunkSize=1000&chunkOverlap=50" \
  -F "file=@document.txt"
```

**返回的元数据：**

```json
{
  "split_method": "sliding_window",
  "window_start": 0,
  "window_end": 1000,
  "window_size": 1000,
  "stride": 500,
  "overlap_ratio": 50,
  "overlap_with_previous": 0
}
```

#### 4.4.5 适用场景

- ✅ **RAG 系统**：提升检索召回率
- ✅ **问答系统**：确保答案不被切割
- ✅ **长文档检索**：避免边界信息丢失
- ✅ **关键信息提取**：多重覆盖确保完整

---

### 4.5 结构化数据切割 - 数据文件专用

#### 4.5.1 核心思想

结构化数据切割专门针对 **CSV、JSON、XML、YAML** 等结构化格式，**保持记录完整性**。

#### 4.5.2 自动格式检测

```java
/**
 * 检测结构化数据格式
 */
private String detectStructuredFormat(String filename, String content) {
    String lower = filename.toLowerCase();
    if (lower.endsWith(".csv")) return "csv";
    if (lower.endsWith(".json")) return "json";
    if (lower.endsWith(".xml")) return "xml";
    if (lower.endsWith(".yaml") || lower.endsWith(".yml")) return "yaml";
    
    // 根据内容检测
    String trimmed = content.trim();
    if (trimmed.startsWith("[") || trimmed.startsWith("{")) return "json";
    if (trimmed.startsWith("<?xml") || trimmed.startsWith("<")) return "xml";
    // ...
}
```

#### 4.5.3 CSV 切割

```java
/**
 * CSV 切割 - 保留表头
 */
private List<Document> splitCsv(String text, String filename, SplitOptions options) {
    int maxRows = options.getChunkSize() != null ? options.getChunkSize() / 50 : 100;
    
    // 提取表头
    String header = lines[0];
    List<String> headers = parseCsvLine(header);
    
    // 每个块都包含表头
    for (String line : dataLines) {
        if (rowCount >= maxRows) {
            // 保存当前块
            // 新块以表头开始
            currentChunk.append(header).append("\n");
        }
        currentChunk.append(line).append("\n");
    }
}
```

#### 4.5.4 JSON 切割

```java
/**
 * JSON 切割 - 按对象/条目分割
 */
private List<Document> splitJson(String text, String filename, SplitOptions options) {
    // JSON 数组：提取每个对象
    if (trimmed.startsWith("[")) {
        List<String> objects = extractJsonObjects(trimmed);
        processJsonObjects(objects, filename, documents, maxSize);
    }
    // JSON 对象：按顶层键分割
    else if (trimmed.startsWith("{")) {
        List<String> entries = extractJsonEntries(trimmed);
        processJsonObjects(entries, filename, documents, maxSize);
    }
}
```

#### 4.5.5 XML 切割

```java
/**
 * XML 切割 - 按元素分割
 */
private List<Document> splitXml(String text, String filename, SplitOptions options) {
    // 提取 XML 声明
    String declaration = "";
    int declEnd = text.indexOf("?>");
    if (declEnd > 0) {
        declaration = text.substring(0, declEnd + 2) + "\n";
    }
    
    // 提取主要元素
    List<String> elements = extractXmlElements(text);
    
    // 每个块包含声明
    for (String elem : elements) {
        currentChunk.append(declaration);
        currentChunk.append(elem);
    }
}
```

#### 4.5.6 使用示例

```bash
# CSV 文件
curl -X POST "http://localhost:8080/api/vector-store/upload?splitStrategy=STRUCTURED_DATA" \
  -F "file=@users.csv"

# JSON 文件
curl -X POST "http://localhost:8080/api/vector-store/upload?splitStrategy=STRUCTURED_DATA" \
  -F "file=@data.json"

# XML 文件
curl -X POST "http://localhost:8080/api/vector-store/upload?splitStrategy=STRUCTURED_DATA" \
  -F "file=@config.xml"

# YAML 文件
curl -X POST "http://localhost:8080/api/vector-store/upload?splitStrategy=STRUCTURED_DATA" \
  -F "file=@config.yaml"
```

**返回的元数据（CSV）：**

```json
{
  "split_method": "structured_data",
  "format": "csv",
  "start_index": 1,
  "end_index": 100,
  "record_count": 100,
  "headers": ["id", "name", "email"],
  "header_count": 3
}
```

**返回的元数据（JSON）：**

```json
{
  "split_method": "structured_data",
  "format": "json",
  "start_index": 0,
  "end_index": 49,
  "record_count": 50
}
```

#### 4.5.7 适用场景

- ✅ **数据文件处理**：CSV、JSON、XML、YAML
- ✅ **配置文件检索**：按配置项检索
- ✅ **日志分析**：结构化日志处理
- ✅ **API 文档**：OpenAPI/Swagger 规范

---

## 五、策略选择指南

### 5.1 按文档类型选择

```
文档类型
├── 普通文本
│   ├── 短文本 → CHARACTER
│   ├── 长文章 → RECURSIVE / AGENTIC
│   └── 需要高召回 → SLIDING_WINDOW
├── Markdown
│   ├── 技术文档 → MARKDOWN
│   └── 博客文章 → MARKDOWN / AGENTIC
├── 代码文件
│   ├── Java/Python/JS → CODE_AWARE
│   └── 通用代码 → RECURSIVE
├── 结构化数据
│   ├── CSV → STRUCTURED_DATA
│   ├── JSON → STRUCTURED_DATA
│   ├── XML → STRUCTURED_DATA
│   └── YAML → STRUCTURED_DATA
├── 书籍/论文
│   ├── 教材 → HIERARCHICAL
│   ├── 论文 → HIERARCHICAL
│   └── 手册 → HIERARCHICAL
└── 特殊需求
    ├── 精确 Token 控制 → TOKEN
    ├── 语义连贯性 → SEMANTIC
    └── 段落完整性 → SMART_PARAGRAPH
```

### 5.2 按使用场景选择

| 场景 | 推荐策略 | 原因 |
|------|----------|------|
| **快速原型** | CHARACTER | 简单快速 |
| **生产环境** | RECURSIVE | 平衡效果与性能 |
| **高质量 RAG** | SLIDING_WINDOW | 高召回率 |
| **代码知识库** | CODE_AWARE | 保持代码结构 |
| **企业文档** | HIERARCHICAL | 保持文档层次 |
| **数据检索** | STRUCTURED_DATA | 记录完整性 |
| **语义搜索** | SEMANTIC | 基于语义切割 |

### 5.3 参数调优建议

| 策略 | chunkSize | chunkOverlap | 说明 |
|------|-----------|--------------|------|
| RECURSIVE | 1000-1500 | 200 | 标准配置 |
| AGENTIC | 1500-2000 | 200 | 更大的块保持主题 |
| CODE_AWARE | 2000-3000 | 0 | 按代码块自然分割 |
| HIERARCHICAL | 2000-5000 | 0 | 按层次自然分割 |
| SLIDING_WINDOW | 1000 | 50-70 | 高重叠率 |
| STRUCTURED_DATA | - | - | 按记录数控制 |

---

## 六、API 使用示例

### 6.1 完整 API 列表

```bash
# 基础策略
POST /api/vector-store/upload?splitStrategy=RECURSIVE
POST /api/vector-store/upload?splitStrategy=MARKDOWN
POST /api/vector-store/upload?splitStrategy=TOKEN
POST /api/vector-store/upload?splitStrategy=SEMANTIC
POST /api/vector-store/upload?splitStrategy=SMART_PARAGRAPH
POST /api/vector-store/upload?splitStrategy=CHARACTER

# 新策略
POST /api/vector-store/upload?splitStrategy=AGENTIC
POST /api/vector-store/upload?splitStrategy=CODE_AWARE
POST /api/vector-store/upload?splitStrategy=HIERARCHICAL
POST /api/vector-store/upload?splitStrategy=SLIDING_WINDOW
POST /api/vector-store/upload?splitStrategy=STRUCTURED_DATA
```

### 6.2 完整请求示例

```bash
# 1. Agentic 切割 - 长文章
curl -X POST "http://localhost:8080/api/vector-store/upload?splitStrategy=AGENTIC&chunkSize=1500&chunkOverlap=200" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@article.txt"

# 2. 代码感知切割 - Java 文件
curl -X POST "http://localhost:8080/api/vector-store/upload?splitStrategy=CODE_AWARE" \
  -F "file=@UserService.java"

# 3. 层次结构切割 - 书籍
curl -X POST "http://localhost:8080/api/vector-store/upload?splitStrategy=HIERARCHICAL&chunkSize=3000" \
  -F "file=@book.txt"

# 4. 滑动窗口切割 - RAG 场景
curl -X POST "http://localhost:8080/api/vector-store/upload?splitStrategy=SLIDING_WINDOW&chunkSize=1000&chunkOverlap=70" \
  -F "file=@knowledge-base.txt"

# 5. 结构化数据切割 - CSV
curl -X POST "http://localhost:8080/api/vector-store/upload?splitStrategy=STRUCTURED_DATA" \
  -F "file=@products.csv"

# 6. 结构化数据切割 - JSON
curl -X POST "http://localhost:8080/api/vector-store/upload?splitStrategy=STRUCTURED_DATA" \
  -F "file=@api-docs.json"
```

### 6.3 响应示例

```json
{
  "success": true,
  "message": "File uploaded and processed successfully",
  "data": {
    "filename": "article.txt",
    "chunkCount": 12,
    "totalCharacters": 15420,
    "splitMethod": "AGENTIC",
    "message": "Processed with AGENTIC strategy"
  }
}
```

---

## 七、性能优化建议

### 7.1 大文件处理

```java
// 对于大文件，建议：
// 1. 使用流式处理
// 2. 增加 chunkSize 减少块数
// 3. 使用异步处理

// 推荐配置
SplitOptions options = new SplitOptions()
    .withChunkSize(2000)      // 更大的块
    .withChunkOverlap(100);   // 较小的重叠
```

### 7.2 批量处理

```java
// 批量上传多个文件
for (File file : files) {
    // 根据文件类型选择策略
    SplitStrategy strategy = selectStrategy(file);
    
    documentSplitterService.split(
        readFile(file),
        file.getName(),
        strategy,
        options
    );
}
```

### 7.3 缓存优化

```java
// 对于重复处理的文档，可以缓存切割结果
@Cacheable(value = "documentChunks", key = "#filename + '_' + #strategy")
public List<Document> split(String text, String filename, SplitStrategy strategy) {
    // ...
}
```

---

## 八、总结

### 8.1 11 种策略一览

| 策略 | 类型 | 核心优势 |
|------|------|----------|
| RECURSIVE | 基础 | 语义完整性 |
| MARKDOWN | 基础 | 结构保留 |
| TOKEN | 基础 | Token 精确控制 |
| SEMANTIC | 基础 | 语义相似度 |
| SMART_PARAGRAPH | 基础 | 段落完整性 |
| CHARACTER | 基础 | 简单快速 |
| **AGENTIC** | **新增** | **智能主题边界** |
| **CODE_AWARE** | **新增** | **代码结构识别** |
| **HIERARCHICAL** | **新增** | **文档树构建** |
| **SLIDING_WINDOW** | **新增** | **高召回率** |
| **STRUCTURED_DATA** | **新增** | **数据完整性** |

### 8.2 核心要点

1. **没有最好的策略，只有最适合的策略**
2. **根据文档类型选择**：代码用 CODE_AWARE，数据用 STRUCTURED_DATA
3. **根据场景选择**：RAG 用 SLIDING_WINDOW，书籍用 HIERARCHICAL
4. **参数调优很重要**：chunkSize 和 chunkOverlap 需要根据实际需求调整

### 8.3 后续学习

- 深入理解 Embedding 原理
- 学习向量数据库优化
- 探索多模态文档处理
- 研究 Agentic RAG 架构

---

## 参考资料

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [LangChain 文档切割最佳实践](https://python.langchain.com/docs/modules/data_connection/document_transformers/)
- [RAG 架构设计指南](https://www.pinecone.io/learn/series/rag/)

---

**原创声明**：本文为原创教程，转载请注明出处。

欢迎在评论区交流讨论！
