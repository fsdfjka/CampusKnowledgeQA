package com.campusknowledgeqa.service;

import com.campusknowledgeqa.entity.KbDocument;
import com.campusknowledgeqa.mapper.KbDocumentMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

/** 文档解析入库（支持同步/异步） */
@Service
public class DocumentIngestService {

    private final DocumentLoadService documentLoadService;
    private final KbDocumentMapper documentMapper;

    public DocumentIngestService(DocumentLoadService documentLoadService, KbDocumentMapper documentMapper) {
        this.documentLoadService = documentLoadService;
        this.documentMapper = documentMapper;
    }

    /** 同步解析入库，返回切片数 */
    public int ingest(String sourceName, Path file, String ext) throws Exception {
        return switch (ext) {
            case "pdf" -> documentLoadService.loadPdf(file, sourceName);
            case "docx", "doc" -> documentLoadService.loadWord(file, sourceName);
            case "txt", "md" -> documentLoadService.loadText(Files.readString(file), sourceName);
            default -> 0;
        };
    }

    /** 异步解析入库，完成后更新文档状态与切片数 */
    @Async
    public void ingestAsync(Long documentId, String sourceName, Path file, String ext) {
        try {
            int chunks = ingest(sourceName, file, ext);
            updateStatus(documentId, "DONE", chunks);
            System.out.println("✅ 异步入库完成：" + sourceName + "，切片 " + chunks);
        } catch (Exception e) {
            updateStatus(documentId, "FAILED", 0);
            System.out.println("⚠️ 异步入库失败：" + sourceName + " -> " + e.getMessage());
        }
    }

    private void updateStatus(Long documentId, String status, int chunks) {
        KbDocument update = new KbDocument();
        update.setId(documentId);
        update.setStatus(status);
        update.setChunkCount(chunks);
        documentMapper.updateById(update);
    }
}
