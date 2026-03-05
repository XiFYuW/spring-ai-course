# Spring AI 实战：TXT 文件上传 + 智能文本切割 + 向量数据库存储，构建企业级 RAG 检索系统

> 📦 **项目源码**：[https://github.com/XiFYuW/spring-ai-course/tree/main/phase-13](https://github.com/XiFYuW/spring-ai-course/tree/main/phase-13)

---

## 目录

- [引言](#引言)
- [技术栈与核心概念](#技术栈与核心概念)
- [项目结构](#项目结构)
- [环境准备](#环境准备)
- [核心实现](#核心实现)
  - [1. 文档切割服务](#1-文档切割服务)
  - [2. 文件处理服务](#2-文件处理服务)
  - [3. 向量存储控制器](#3-向量存储控制器)
- [API 接口说明](#api-接口说明)
- [测试与验证](#测试与验证)
- [避坑指南](#避坑指南)
- [总结与扩展](#总结与扩展)

---

## 引言

在 AI 大模型时代，**RAG（Retrieval-Augmented Generation，检索增强生成）** 已成为企业级 AI 应用的核心架构。它通过将私有文档数据向量化存储，让大模型能够"读懂"企业内部知识，实现精准问答。

**本文将带你实现**：
- ✅ 支持 TXT 文件上传的 REST API
- ✅ 三种智能文本切割策略（字符切割、段落切割、智能切割）
- ✅ 自动编码检测（UTF-8/GBK）
- ✅ 存储到 Elasticsearch 向量数据库
- ✅ 基于向量的相似性检索

**读完本文你将收获**：
- Spring AI Vector Store 的实战使用经验
- 文本切割的核心算法与设计思想
- 完整的 RAG 文档处理流水线

---

## 技术栈与核心概念

### 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.5.10 | 基础框架 |
| Spring AI | 1.1.0-SNAPSHOT | AI 开发框架 |
| Elasticsearch | 8.x | 向量数据库 |
| OpenAI API | - | Embedding 模型 |
| Java | 25 | 编程语言 |

### 核心概念

#### 1. 文本切割（Text Splitting）

为什么需要切割？
- **Embedding 模型限制**：大多数 Embedding 模型有最大输入长度限制（如 512/1024/2048 tokens）
- **检索精度**：过长的文本会导致语义稀释，降低检索准确度
- **上下文窗口**：大模型的上下文窗口有限，需要精炼的文本块

#### 2. 切割策略对比

| 策略 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **字符切割** | 简单可控，块大小固定 | 可能切断句子 | 通用场景 |
| **段落切割** | 保留段落完整性 | 块大小不均匀 | 结构化文档 |
| **智能切割** | 兼顾完整性和大小控制 | 实现稍复杂 | **推荐** |

#### 3. 重叠（Overlap）

**为什么需要重叠？**

```
块1: [AAAAAAAAAA|BBBBBBBBBB]
块2:          [BBBBBBBBBB|CCCCCCCCCC]
               ↑↑↑↑↑↑↑↑↑↑
               重叠区域（200字符）
```

重叠确保跨块边界的上下文不会丢失，提高检索召回率。

---

## 项目结构

```
spring-ai-course/phase-13
├── src/main/java/org/example/
│   ├── SpringAiJcStart.java              # 启动类
│   ├── controller/
│   │   └── VectorStoreController.java    # REST API 控制器
│   └── service/
│       ├── VectorStoreService.java       # 向量存储服务
│       ├── DocumentSplitterService.java  # 文档切割服务 ⭐新增
│       └── FileProcessingService.java    # 文件处理服务 ⭐新增
├── pom.xml
└── README.md
```

---

## 环境准备

### 1. 获取 AI API Key

本项目使用 OpenAI 兼容的 API 服务，你可以：

1. 使用 OpenAI 官方 API
2. 使用第三方代理服务（如项目中配置的[https://ai.32zi.com](https://ai.32zi.com)）

**配置方式**：在 `application.yml` 中设置你的 API Key

模型是：`text-embedding-ada-002`

### 2. Maven 依赖

```xml
<dependencies>
    <!-- Spring Boot WebFlux -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
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
</dependencies>
```

### 3. 配置文件

```yaml
# application.yml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
    vectorstore:
      elasticsearch:
        index-name: vector-store
        dimensions: 1536  # text-embedding-ada-002 的维度
```

### 4. 启动 Elasticsearch

```bash
# 使用 Docker 快速启动
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  elasticsearch:8.11.0
```

---

## 核心实现

### 1. 文档切割服务

**DocumentSplitterService.java** - 提供三种切割策略：

```java
@Service
public class DocumentSplitterService {

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
    public List<Document> splitByCharacter(String text, String filename, int chunkSize, int chunkOverlap) {
        List<Document> documents = new ArrayList<>();
        
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
                // 创建文档块元数据 - 这些元数据对 RAG 检索至关重要
                Map<String, Object> metadata = Map.of(
                    "source", filename,           // 来源文件
                    "chunk_index", chunkIndex,    // 块序号
                    "start_char", startIndex,     // 起始字符位置
                    "end_char", endIndex,         // 结束字符位置
                    "total_chunks", totalChunks   // 总块数
                );
                
                documents.add(new Document(chunk, metadata));
                chunkIndex++;
            }

            // 计算下一个块的起始位置（考虑重叠）
            startIndex = endIndex - chunkOverlap;
        }
        
        return documents;
    }

    /**
     * 智能切割：先按段落，如果段落太长再按字符数切割
     * 
     * 这是推荐的切割策略，兼顾语义完整性和大小控制
     */
    public List<Document> splitSmart(String text, String filename, int maxChunkSize) {
        List<Document> documents = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\s*\\n");
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;

        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i].trim();
            if (paragraph.isEmpty()) continue;

            // 如果当前段落本身超过最大长度，需要先切割当前累积的内容
            if (paragraph.length() > maxChunkSize) {
                // 保存当前累积的内容
                if (currentChunk.length() > 0) {
                    saveChunk(documents, currentChunk, filename, chunkIndex++);
                    currentChunk = new StringBuilder();
                }
                
                // 切割这个长段落
                List<Document> paragraphChunks = splitByCharacter(paragraph, filename, maxChunkSize, DEFAULT_CHUNK_OVERLAP);
                documents.addAll(paragraphChunks);
            } 
            // 如果添加这个段落会超过限制，先保存当前块
            else if (currentChunk.length() + paragraph.length() + 2 > maxChunkSize) {
                if (currentChunk.length() > 0) {
                    saveChunk(documents, currentChunk, filename, chunkIndex++);
                }
                currentChunk = new StringBuilder(paragraph);
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
            saveChunk(documents, currentChunk, filename, chunkIndex);
        }

        return documents;
    }

    /**
     * 查找最佳切割点（优先在句子结束处切割）
     * 
     * 避免在单词中间切断，提高语义连贯性
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
```

**关键设计思想**：
1. **元数据追踪**：每个块都记录来源文件、位置信息，方便溯源
2. **智能边界检测**：优先在句子结束处切割，避免语义断裂
3. **重叠机制**：确保跨块信息不丢失

---

### 2. 文件处理服务

**FileProcessingService.java** - 完整的文件处理流水线：

```java
@Service
public class FileProcessingService {

    private final DocumentSplitterService documentSplitterService;
    private final VectorStoreService vectorStoreService;

    /**
     * 处理上传的 TXT 文件并存储到向量数据库
     * 
     * 完整流程：
     * 1. 读取文件内容
     * 2. 检测并转换编码（UTF-8/GBK）
     * 3. 切割文本
     * 4. 存储到向量数据库
     */
    public Mono<FileProcessingResult> processAndStore(
            FilePart filePart,
            String splitMethod,
            int chunkSize,
            int chunkOverlap) {
        
        String filename = filePart.filename();
        
        return readFileContent(filePart)
                .flatMap(content -> {
                    // 切割文档
                    List<Document> documents = splitDocument(content, filename, splitMethod, chunkSize, chunkOverlap);
                    
                    // 存储到向量数据库
                    return vectorStoreService.addDocuments(documents)
                            .thenReturn(new FileProcessingResult(
                                    filename,
                                    documents.size(),
                                    content.length(),
                                    splitMethod,
                                    "File processed and stored successfully"
                            ));
                });
    }

    /**
     * 读取文件内容为字符串
     */
    public Mono<String> readFileContent(FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    
                    // 自动检测编码
                    return detectAndDecode(bytes);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 检测并解码字节数组为字符串
     * 
     * 优先尝试 UTF-8，如果检测到乱码则尝试 GBK
     * 解决中文 Windows 环境常见的编码问题
     */
    private String detectAndDecode(byte[] bytes) {
        // 首先尝试 UTF-8
        String utf8Content = new String(bytes, StandardCharsets.UTF_8);
        
        // 简单的检测：如果包含 UTF-8 解码失败的常见乱码特征，尝试 GBK
        if (containsGarbledCharacters(utf8Content)) {
            try {
                String gbkContent = new String(bytes, Charset.forName("GBK"));
                return gbkContent;
            } catch (Exception e) {
                // 回退到 UTF-8
            }
        }
        
        return utf8Content;
    }

    /**
     * 检查字符串是否包含乱码特征
     */
    private boolean containsGarbledCharacters(String content) {
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
```

**核心亮点**：
- **自动编码检测**：解决 UTF-8/GBK 编码问题
- **响应式编程**：使用 Reactor 处理大文件上传
- **完整元数据**：记录处理过程的所有关键信息

---

### 3. 向量存储控制器

**VectorStoreController.java** - 添加文件上传接口：

```java
@RestController
@RequestMapping("/api/vector-store")
public class VectorStoreController {

    private final VectorStoreService vectorStoreService;
    private final FileProcessingService fileProcessingService;

    /**
     * 上传 TXT 文件并存储到向量数据库
     * 
     * POST /api/vector-store/upload
     * 
     * 支持参数：
     * - file: TXT 文件（必需）
     * - splitMethod: 切割方法，可选 character/paragraph/smart（默认：smart）
     * - chunkSize: 块大小（默认：1000）
     * - chunkOverlap: 重叠大小（默认：200）
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Mono<ResponseEntity<ApiResponse<FileUploadResponse>>> uploadFile(
            @RequestPart("file") FilePart filePart,
            @RequestParam(name = "splitMethod", defaultValue = "smart") String splitMethod,
            @RequestParam(name = "chunkSize", defaultValue = "1000") int chunkSize,
            @RequestParam(name = "chunkOverlap", defaultValue = "200") int chunkOverlap) {
        
        String filename = filePart.filename();

        // 验证文件类型
        if (!filename.toLowerCase().endsWith(".txt")) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(ApiResponse.error("Only .txt files are supported")));
        }

        return fileProcessingService.processAndStore(filePart, splitMethod, chunkSize, chunkOverlap)
                .map(result -> ResponseEntity.ok(ApiResponse.success("File uploaded and processed successfully",
                        new FileUploadResponse(
                                result.filename(),
                                result.chunkCount(),
                                result.totalCharacters(),
                                result.splitMethod(),
                                result.message()
                        ))));
    }

    /**
     * 相似性搜索
     * 
     * GET /api/vector-store/search?query=Spring&topK=5
     */
    @GetMapping("/search")
    public Mono<ResponseEntity<ApiResponse<List<DocumentResponse>>>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        
        return vectorStoreService.similaritySearch(query, topK)
                .map(documents -> {
                    List<DocumentResponse> responses = documents.stream()
                            .map(doc -> new DocumentResponse(
                                    doc.getId(),
                                    doc.getText(),
                                    doc.getMetadata()
                            ))
                            .toList();
                    return ResponseEntity.ok(ApiResponse.success("Search completed", responses));
                });
    }
}
```

---

## API 接口说明

### 1. 上传文件

```bash
curl -X POST "http://localhost:8080/api/vector-store/upload" \
  -F "file=@document.txt" \
  -F "splitMethod=smart" \
  -F "chunkSize=1000" \
  -F "chunkOverlap=200"
```

**响应示例**：

```json
{
  "success": true,
  "message": "File uploaded and processed successfully",
  "data": {
    "filename": "document.txt",
    "chunkCount": 15,
    "totalCharacters": 12580,
    "splitMethod": "smart",
    "message": "File processed and stored successfully"
  }
}
```

### 2. 相似性搜索

```bash
curl "http://localhost:8080/api/vector-store/search?query=Spring%20AI&topK=3"
```

**响应示例**：

```json
{
  "success": true,
  "message": "Search completed",
  "data": [
    {
      "id": "doc-001",
      "content": "Spring AI 提供了简洁的 API 用于与 AI 模型交互...",
      "metadata": {
        "source": "document.txt",
        "chunk_index": 5,
        "start_char": 5000,
        "end_char": 6000
      }
    }
  ]
}
```

### 3. 批量添加文档

```bash
curl -X POST "http://localhost:8080/api/vector-store/documents/batch" \
  -H "Content-Type: application/json" \
  -d '[
    {"content": "文档内容1", "metadata": {"category": "tech"}},
    {"content": "文档内容2", "metadata": {"category": "tech"}}
  ]'
```

---

## 测试与验证

### 1. 准备测试文件

创建一个测试用的 TXT 文件：

```bash
cat > test-document.txt << 'EOF'
Spring AI 简介

Spring AI 是 Spring 生态系统中用于简化人工智能应用开发的框架。它提供了一致的 API 抽象，让开发者能够轻松集成各种 AI 模型。

核心特性包括：
- 模型抽象：统一不同 AI 提供商的接口
- 提示词模板：支持动态构建提示词
- 向量存储：内置多种向量数据库支持
- 函数调用：支持 AI 调用外部工具

向量存储是 RAG 架构的核心组件。Spring AI 支持 Elasticsearch、Redis、PostgreSQL 等多种向量数据库。

文本切割是将长文档分割成小块的过程。合理的切割策略能够提高检索精度，同时保持语义连贯性。
EOF
```

### 2. 上传测试

```bash
curl -X POST "http://localhost:8080/api/vector-store/upload" \
  -F "file=@test-document.txt" \
  -F "splitMethod=smart"
```

### 3. 搜索测试

```bash
curl "http://localhost:8080/api/vector-store/search?query=什么是Spring%20AI&topK=2"
```

---

## 避坑指南

### 1. 编码问题

**问题**：Windows 环境创建的 TXT 文件使用 GBK 编码，上传后出现乱码。

**解决方案**：
```java
// 自动检测编码逻辑
private String detectAndDecode(byte[] bytes) {
    String utf8Content = new String(bytes, StandardCharsets.UTF_8);
    if (containsGarbledCharacters(utf8Content)) {
        return new String(bytes, Charset.forName("GBK"));
    }
    return utf8Content;
}
```

### 2. 块大小设置

**建议值**：
- `chunkSize`: 500-2000 字符（根据 Embedding 模型调整）
- `chunkOverlap`: 块大小的 10%-20%

**太大**：语义稀释，检索精度下降  
**太小**：上下文丢失，理解不完整

### 3. Elasticsearch 连接

**问题**：连接 Elasticsearch 失败

**检查清单**：
- [ ] Elasticsearch 是否已启动？`curl http://localhost:9200`
- [ ] 网络是否连通？
- [ ] 索引名称是否正确配置？

---

## 总结与扩展

### 本文核心要点

1. **文本切割是 RAG 的基础**：合理的切割策略直接影响检索质量
2. **元数据很重要**：记录来源信息，支持结果溯源
3. **编码问题要预防**：中文字符编码是常见坑点
4. **Spring AI 简化开发**：Vector Store 抽象让切换数据库变得容易

### 扩展方向

**功能扩展**：
- [ ] 支持 PDF、Word、Markdown 等更多格式
- [ ] 添加文档更新和版本管理
- [ ] 实现批量上传和异步处理
- [ ] 添加 OCR 支持图片中的文字

**性能优化**：
- [ ] 使用线程池并行处理大文件
- [ ] 实现增量更新，避免重复处理
- [ ] 添加缓存机制

**RAG 完整链路**：
```
文档上传 → 文本切割 → 向量化 → 存储 → 检索 → 重排序 → 上下文构建 → LLM 生成
```

---

## 参考资料

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Elasticsearch Vector Search](https://www.elastic.co/guide/en/elasticsearch/reference/current/knn-search.html)
- [OpenAI Embeddings API](https://platform.openai.com/docs/guides/embeddings)

---

**原创声明**：本文为原创教程，转载请注明出处。

**欢迎在评论区交流讨论！**

---

💰 **为什么选择 32ai？**

**低至 0.56 : 1 比率**
🔗 **快速访问**: [点击访问](https://ai.32zi.com) — 直连、无需魔法。
