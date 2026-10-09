package com.campusknowledgeqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusknowledgeqa.config.KnowledgeVectorStore;
import com.campusknowledgeqa.entity.KbCategory;
import com.campusknowledgeqa.entity.KbDocument;
import com.campusknowledgeqa.entity.SysUser;
import com.campusknowledgeqa.mapper.KbCategoryMapper;
import com.campusknowledgeqa.mapper.KbDocumentMapper;
import com.campusknowledgeqa.mapper.SysUserMapper;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 知识库文档：落库 kb_document + 解析入库 + 列表查询 */
@Service
public class DocumentService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final KbDocumentMapper documentMapper;
    private final KbCategoryMapper categoryMapper;
    private final SysUserMapper userMapper;
    private final DocumentIngestService documentIngestService;
    private final KnowledgeVectorStore vectorStore;

    public DocumentService(KbDocumentMapper documentMapper, KbCategoryMapper categoryMapper,
                           SysUserMapper userMapper, DocumentIngestService documentIngestService,
                           KnowledgeVectorStore vectorStore) {
        this.documentMapper = documentMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.documentIngestService = documentIngestService;
        this.vectorStore = vectorStore;
    }

    /**
     * 落库 kb_document（状态 PARSING），并异步解析、切片、向量化入库
     */
    public KbDocument createDocument(String originalName, Path file, String sourceName,
                                     Long categoryId, Long uploaderId) {
        String ext = extensionOf(originalName);
        String name = (sourceName == null || sourceName.isBlank()) ? originalName : sourceName;

        KbDocument doc = new KbDocument();
        doc.setName(name);
        doc.setCategoryId(categoryId);
        doc.setFileType(ext.toUpperCase());
        doc.setFilePath(file.toString());
        doc.setChunkCount(0);
        doc.setStatus("PARSING");
        doc.setUploaderId(uploaderId);
        try {
            doc.setFileSize(Files.size(file));
        } catch (Exception e) {
            doc.setFileSize(0L);
        }
        documentMapper.insert(doc);

        // 异步解析入库（完成后更新状态与切片数）
        documentIngestService.ingestAsync(doc.getId(), name, file, ext);
        // 回读以拿到数据库生成的 created_at
        return documentMapper.selectById(doc.getId());
    }

    /** 文档列表（含分类名、上传人名） */
    public List<Map<String, Object>> list() {
        List<KbDocument> docs = documentMapper.selectList(
                new LambdaQueryWrapper<KbDocument>().orderByDesc(KbDocument::getCreatedAt));
        Map<Long, String> categoryNames = categoryNameMap();
        Map<Long, String> uploaderNames = uploaderNameMap();

        List<Map<String, Object>> result = new ArrayList<>();
        for (KbDocument doc : docs) {
            result.add(toView(doc, categoryNames, uploaderNames));
        }
        return result;
    }

    public Map<String, Object> toView(KbDocument doc) {
        return toView(doc, categoryNameMap(), uploaderNameMap());
    }

    /**
     * 查看文档切片：按 metadata.source 查向量库，按 chunkIndex 升序，分页返回。
     */
    public Map<String, Object> listChunks(Long id, long page, long size) {
        KbDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在");
        }
        List<Document> chunks = new ArrayList<>(vectorStore.listChunksBySource(document.getName()));
        chunks.sort(Comparator.comparingInt(this::chunkIndexOf));

        long total = chunks.size();
        long safeSize = Math.max(1, size);
        int from = (int) Math.min(Math.max(0, (Math.max(1, page) - 1) * safeSize), total);
        int to = (int) Math.min(total, from + safeSize);

        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = from; i < to; i++) {
            Document doc = chunks.get(i);
            String content = doc.getText();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("index", i + 1);
            item.put("chars", content == null ? 0 : content.length());
            item.put("contentType", doc.getMetadata().get("contentType"));
            item.put("content", content);
            list.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("page", page);
        data.put("size", safeSize);
        data.put("list", list);
        return data;
    }

    private int chunkIndexOf(Document doc) {
        Object value = doc.getMetadata().get("chunkIndex");
        return value instanceof Number number ? number.intValue() : Integer.MAX_VALUE;
    }

    /**
     * 删除文档：先按 source 删除向量库中的切片，再删除 kb_document
     */
    public boolean delete(Long id) {
        KbDocument document = documentMapper.selectById(id);
        if (document == null) {
            return false;
        }
        try {
            vectorStore.deleteBySource(document.getName());
        } catch (Exception e) {
            System.out.println("⚠️ 删除向量失败：" + e.getMessage());
        }
        documentMapper.deleteById(id);
        System.out.println("🗑️ 已删除文档及其向量：" + document.getName());
        return true;
    }

    public KbDocument get(Long id) {
        return documentMapper.selectById(id);
    }

    /**
     * 修改文档：改名时同步更新向量 metadata.source；可改分类
     */
    public KbDocument update(Long id, String name, Long categoryId) {
        KbDocument document = documentMapper.selectById(id);
        if (document == null) {
            return null;
        }
        String oldName = document.getName();
        if (name != null && !name.isBlank() && !name.equals(oldName)) {
            document.setName(name);
            try {
                vectorStore.updateSource(oldName, name);
            } catch (Exception e) {
                System.out.println("⚠️ 更新向量 source 失败：" + e.getMessage());
            }
        }
        document.setCategoryId(categoryId);
        documentMapper.updateById(document);
        return documentMapper.selectById(id);
    }

    /**
     * 重新解析：删旧向量 -> 按源文件重新解析入库 -> 更新状态/切片数
     */
    public KbDocument reparse(Long id) {
        KbDocument document = documentMapper.selectById(id);
        if (document == null) {
            return null;
        }
        Path file = Path.of(document.getFilePath());
        if (!Files.isRegularFile(file)) {
            document.setStatus("FAILED");
            documentMapper.updateById(document);
            throw new RuntimeException("源文件不存在：" + document.getFilePath());
        }
        try {
            vectorStore.deleteBySource(document.getName());
        } catch (Exception e) {
            System.out.println("⚠️ 删除旧向量失败：" + e.getMessage());
        }
        document.setStatus("PARSING");
        documentMapper.updateById(document);

        try {
            int chunks = documentIngestService.ingest(document.getName(), file, extensionOf(document.getFilePath()));
            document.setStatus("DONE");
            document.setChunkCount(chunks);
        } catch (Exception e) {
            document.setStatus("FAILED");
            documentMapper.updateById(document);
            throw new RuntimeException("重新解析失败：" + e.getMessage(), e);
        }
        documentMapper.updateById(document);
        System.out.println("♻️ 已重新解析文档：" + document.getName());
        return documentMapper.selectById(id);
    }

    private Map<String, Object> toView(KbDocument doc, Map<Long, String> categoryNames,
                                       Map<Long, String> uploaderNames) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", doc.getId());
        view.put("name", doc.getName());
        view.put("categoryId", doc.getCategoryId());
        view.put("categoryName", doc.getCategoryId() == null ? null : categoryNames.get(doc.getCategoryId()));
        view.put("fileType", doc.getFileType());
        view.put("fileSize", doc.getFileSize());
        view.put("chunkCount", doc.getChunkCount());
        view.put("status", doc.getStatus());
        view.put("uploaderId", doc.getUploaderId());
        view.put("uploaderName", doc.getUploaderId() == null ? null : uploaderNames.get(doc.getUploaderId()));
        view.put("createdAt", doc.getCreatedAt() == null ? null : FMT.format(doc.getCreatedAt()));
        return view;
    }

    private Map<Long, String> categoryNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (KbCategory category : categoryMapper.selectList(null)) {
            map.put(category.getId(), category.getName());
        }
        return map;
    }

    private Map<Long, String> uploaderNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (SysUser user : userMapper.selectList(null)) {
            map.put(user.getId(), user.getNickname() != null ? user.getNickname() : user.getUsername());
        }
        return map;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase() : "";
    }
}
