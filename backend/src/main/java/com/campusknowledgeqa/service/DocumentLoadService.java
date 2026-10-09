package com.campusknowledgeqa.service;

import com.campusknowledgeqa.config.KnowledgeVectorStore;
import com.campusknowledgeqa.util.TextCleaner;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DocumentLoadService {

    /** 表格每块最多行数 */
    private static final int MAX_TABLE_ROWS_PER_CHUNK = 30;
    /** 表格每块最多字符数 */
    private static final int MAX_TABLE_CHARS_PER_CHUNK = 1500;
    /** 兜底：单条文档最大字符数（超过则按字符硬切），保证不会超 embedding 单条 token 上限 */
    private static final int MAX_CHARS_PER_CHUNK = 1500;
    private static final int HARD_SPLIT_OVERLAP = 100;
    /** 每次调用 embedding 的最大切片数 */
    private static final int EMBED_BATCH_SIZE = 50;
    /** 单批失败重试次数 */
    private static final int ADD_MAX_RETRY = 2;

    @Autowired
    private KnowledgeVectorStore vectorStore;
    @Autowired
    private PdfTableExtractor pdfTableExtractor;
    @Autowired
    private WordDocumentParser wordDocumentParser;

    /** 文本切片大小（token），需小于 embedding 模型单条上限 */
    @Value("${app.rag.chunk-size:600}")
    private int chunkSize;

    public int loadPdf(Path pdfPath, String sourceName) {
        PagePdfDocumentReader reader = new PagePdfDocumentReader(new FileSystemResource(pdfPath));
        List<Document> originDocs = reader.get();
        // 清洗正文（去控制字符/压缩空白/去重复页眉页脚）
        List<Document> cleaned = new ArrayList<>();
        for (Document d : originDocs) {
            Map<String, Object> meta = new HashMap<>(d.getMetadata());
            meta.put("source", sourceName);
            cleaned.add(new Document(TextCleaner.clean(d.getText()), meta));
        }
        List<Document> docs = new ArrayList<>(newSplitter().transform(cleaned));

        for (Document tableDoc : pdfTableExtractor.extract(pdfPath, sourceName)) {
            Map<String, Object> tableMeta = new HashMap<>(tableDoc.getMetadata());
            tableMeta.put("source", sourceName);
            String title = tableMeta.get("page_number") instanceof Integer page
                    ? "【表格·第" + page + "页】" : "【表格】";
            docs.addAll(splitTable(tableDoc.getText(), tableMeta, title));
        }

        List<Document> finalDocs = enforceMaxSize(docs);
        addInBatches(finalDocs);
        System.out.println("✅【" + sourceName + "】PDF入库完成，切片：" + finalDocs.size());
        return finalDocs.size();
    }

    public int loadWord(Path wordPath, String sourceName) {
        WordDocumentParser.ParsedWord parsed = wordDocumentParser.parse(wordPath);
        List<Document> docs = new ArrayList<>();

        String cleanedText = TextCleaner.clean(parsed.text());
        if (!cleanedText.isBlank()) {
            Map<String, Object> textMeta = new HashMap<>();
            textMeta.put("source", sourceName);
            docs.addAll(newSplitter().transform(List.of(new Document(cleanedText, textMeta))));
        }
        for (String table : parsed.tables()) {
            Map<String, Object> tableMeta = new HashMap<>();
            tableMeta.put("source", sourceName);
            docs.addAll(splitTable(table, tableMeta, "【表格】"));
        }

        if (docs.isEmpty()) {
            System.out.println("⚠️【" + sourceName + "】Word 无可入库内容");
            return 0;
        }
        List<Document> finalDocs = enforceMaxSize(docs);
        addInBatches(finalDocs);
        System.out.println("✅【" + sourceName + "】Word入库完成，切片：" + finalDocs.size());
        return finalDocs.size();
    }

    public int loadText(String textContent, String sourceName) {
        Map<String, Object> meta = Map.of("source", sourceName);
        Document doc = new Document(TextCleaner.clean(textContent), meta);
        List<Document> split = enforceMaxSize(new ArrayList<>(newSplitter().transform(List.of(doc))));
        addInBatches(split);
        System.out.println("✅ 文本入库完成，切片数量：" + split.size());
        return split.size();
    }

    public List<Map<String, Object>> listDocuments() {
        List<Document> all = vectorStore.getAllDocuments();
        Map<String, List<Document>> grouped = all.stream().collect(Collectors.groupingBy(
                d -> String.valueOf(d.getMetadata().getOrDefault("source", "未命名")),
                LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((source, docs) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", source);
            item.put("chunks", docs.size());
            result.add(item);
        });
        return result;
    }

    private TokenTextSplitter newSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withMinChunkSizeChars(350)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();
    }

    /**
     * 最终兜底：任何一条文档超过 MAX_CHARS_PER_CHUNK 就按字符硬切，
     * 保证不会出现「single document exceeds max input tokens」。
     */
    private List<Document> enforceMaxSize(List<Document> docs) {
        List<Document> result = new ArrayList<>();
        for (Document doc : docs) {
            String text = doc.getText();
            if (text == null || text.length() <= MAX_CHARS_PER_CHUNK) {
                result.add(doc);
                continue;
            }
            int step = MAX_CHARS_PER_CHUNK - HARD_SPLIT_OVERLAP;
            for (int i = 0; i < text.length(); i += step) {
                int end = Math.min(text.length(), i + MAX_CHARS_PER_CHUNK);
                result.add(new Document(text.substring(i, end), new HashMap<>(doc.getMetadata())));
                if (end == text.length()) {
                    break;
                }
            }
        }
        return result;
    }

    /** 分批调用 embedding 入库，单批失败自动重试 */
    private void addInBatches(List<Document> docs) {
        List<Document> indexed = withChunkIndex(docs);
        for (int i = 0; i < indexed.size(); i += EMBED_BATCH_SIZE) {
            List<Document> batch = indexed.subList(i, Math.min(i + EMBED_BATCH_SIZE, indexed.size()));
            int attempt = 0;
            while (true) {
                try {
                    vectorStore.add(batch);
                    break;
                } catch (Exception e) {
                    if (++attempt > ADD_MAX_RETRY) {
                        throw e;
                    }
                    System.out.println("⚠️ 入库批次失败，第 " + attempt + " 次重试：" + e.getMessage());
                    try {
                        Thread.sleep(500L * attempt);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    /** 写入切片顺序号（chunkIndex/chunkTotal），便于后台按原顺序查看切分内容 */
    private List<Document> withChunkIndex(List<Document> docs) {
        List<Document> indexed = new ArrayList<>(docs.size());
        int total = docs.size();
        for (int i = 0; i < total; i++) {
            Document doc = docs.get(i);
            Map<String, Object> meta = new HashMap<>(doc.getMetadata());
            meta.put("chunkIndex", i);
            meta.put("chunkTotal", total);
            indexed.add(new Document(doc.getId(), doc.getText(), meta));
        }
        return indexed;
    }

    private List<Document> splitTable(String markdown, Map<String, Object> baseMeta, String title) {
        List<Document> docs = new ArrayList<>();
        List<String> parts = splitMarkdownTable(markdown, MAX_TABLE_ROWS_PER_CHUNK, MAX_TABLE_CHARS_PER_CHUNK);
        for (int i = 0; i < parts.size(); i++) {
            Map<String, Object> meta = new HashMap<>(baseMeta);
            meta.put("contentType", "table");
            String header = parts.size() > 1
                    ? title + "（" + (i + 1) + "/" + parts.size() + "）\n"
                    : title + "\n";
            docs.add(new Document(header + parts.get(i), meta));
        }
        return docs;
    }

    /** 按行分块，同时受行数和字符数限制；每块都重复表头 */
    private List<String> splitMarkdownTable(String markdown, int maxRows, int maxChars) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }
        String[] lines = markdown.strip().split("\n");
        if (lines.length <= 2) {
            return List.of(markdown.strip());
        }
        String header = lines[0];
        String separator = lines[1];

        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder(header).append('\n').append(separator).append('\n');
        int rows = 0;
        for (int i = 2; i < lines.length; i++) {
            String line = lines[i];
            boolean overRows = rows >= maxRows;
            boolean overChars = rows > 0 && current.length() + line.length() + 1 > maxChars;
            if (overRows || overChars) {
                parts.add(current.toString().strip());
                current = new StringBuilder(header).append('\n').append(separator).append('\n');
                rows = 0;
            }
            current.append(line).append('\n');
            rows++;
        }
        if (rows > 0) {
            parts.add(current.toString().strip());
        }
        return parts;
    }
}
