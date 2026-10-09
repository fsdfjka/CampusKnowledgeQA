package com.campusknowledgeqa.controller;

import com.campusknowledgeqa.annotation.OperationLog;
import com.campusknowledgeqa.common.Result;
import com.campusknowledgeqa.config.KnowledgeVectorStore;
import com.campusknowledgeqa.dto.CategoryRequest;
import com.campusknowledgeqa.dto.DocumentUpdateRequest;
import com.campusknowledgeqa.entity.KbCategory;
import com.campusknowledgeqa.entity.KbDocument;
import com.campusknowledgeqa.service.CategoryService;
import com.campusknowledgeqa.service.DocumentLoadService;
import com.campusknowledgeqa.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class DocumentController {

    /** 仅允许访问该目录内的文件，防止路径遍历 */
    private static final Path UPLOAD_DIR = Path.of("uploads").toAbsolutePath().normalize();
    private static final Set<String> SUPPORTED_EXT = Set.of("pdf", "docx", "doc", "txt", "md");

    @Autowired
    private DocumentLoadService documentLoadService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private KnowledgeVectorStore vectorStore;

    /**
     * 统一文件上传入库：落 kb_document（分类/类型/大小/状态/上传人），再解析向量化
     */
    @PostMapping("/uploadDocument")
    @OperationLog(module = "文档管理", action = "上传文档")
    public Result<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(value = "sourceName", required = false) String sourceName,
                                                      @RequestParam(value = "categoryId", required = false) Long categoryId,
                                                      Authentication authentication) throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.fail(4001, "请选择要上传的文件");
        }
        String fileName = sanitizeFileName(file.getOriginalFilename());
        String extension = extensionOf(fileName);
        if (!SUPPORTED_EXT.contains(extension)) {
            return Result.fail(4003, "不支持的文件类型：." + extension);
        }
        Path target = resolveWithinUploads(fileName);
        if (target == null) {
            return Result.fail(4002, "非法的文件名");
        }
        Files.createDirectories(UPLOAD_DIR);
        file.transferTo(target);

        Long uploaderId = currentUserId(authentication);
        KbDocument document = documentService.createDocument(fileName, target, sourceName, categoryId, uploaderId);
        return Result.success(documentService.toView(document));
    }

    /**
     * 纯文本入库接口（直接传文本）
     */
    @PostMapping("/loadText")
    @OperationLog(module = "文档管理", action = "文本入库")
    public Result<String> loadText(@RequestBody Map<String, String> body) {
        documentLoadService.loadText(body.get("text"), body.get("sourceName"));
        return Result.success("✅文本入库成功，来源：" + body.get("sourceName"));
    }

    /** 文档列表（查 kb_document，含分类名/上传人名） */
    @GetMapping("/documents")
    public Result<List<Map<String, Object>>> documents() {
        return Result.success(documentService.list());
    }

    /** 文档详情 */
    @GetMapping("/documents/{id}")
    public Result<Map<String, Object>> document(@PathVariable Long id) {
        KbDocument document = documentService.get(id);
        return document == null ? Result.fail(4004, "文档不存在") : Result.success(documentService.toView(document));
    }

    /** 查看文档切片（按顺序，分页） */
    @GetMapping("/documents/{id}/chunks")
    public Result<Map<String, Object>> chunks(@PathVariable Long id,
                                              @RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "20") long size) {
        return Result.success(documentService.listChunks(id, page, size));
    }

    /** 修改文档（名称/分类；改名会同步更新向量 metadata.source） */
    @PutMapping("/documents/{id}")
    @OperationLog(module = "文档管理", action = "修改文档")
    public Result<Map<String, Object>> updateDocument(@PathVariable Long id,
                                                      @RequestBody DocumentUpdateRequest request) {
        KbDocument document = documentService.update(id, request.name(), request.categoryId());
        return document == null ? Result.fail(4004, "文档不存在") : Result.success(documentService.toView(document));
    }

    /** 重新解析文档 */
    @PostMapping("/documents/{id}/reparse")
    @OperationLog(module = "文档管理", action = "重新解析")
    public Result<Map<String, Object>> reparseDocument(@PathVariable Long id) {
        KbDocument document = documentService.reparse(id);
        return document == null ? Result.fail(4004, "文档不存在") : Result.success(documentService.toView(document));
    }

    /** 删除文档：同时清理向量库中该 source 的切片 */
    @DeleteMapping("/documents/{id}")
    @OperationLog(module = "文档管理", action = "删除文档")
    public Result<String> deleteDocument(@PathVariable Long id) {
        boolean deleted = documentService.delete(id);
        return deleted
                ? Result.success("✅文档及其向量已删除")
                : Result.fail(4004, "文档不存在");
    }

    /** 分类列表 */
    @GetMapping("/categories")
    public Result<List<KbCategory>> categories() {
        return Result.success(categoryService.list());
    }

    /** 新增分类 */
    @PostMapping("/categories")
    @OperationLog(module = "分类管理", action = "新增")
    public Result<KbCategory> createCategory(@RequestBody CategoryRequest request) {
        return Result.success(categoryService.create(request));
    }

    /** 修改分类 */
    @PutMapping("/categories/{id}")
    @OperationLog(module = "分类管理", action = "修改")
    public Result<KbCategory> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {
        KbCategory category = categoryService.update(id, request);
        return category == null ? Result.fail(4004, "分类不存在") : Result.success(category);
    }

    /** 删除分类（该分类下文档的 category_id 会被置空） */
    @DeleteMapping("/categories/{id}")
    @OperationLog(module = "分类管理", action = "删除")
    public Result<String> deleteCategory(@PathVariable Long id) {
        boolean deleted = categoryService.delete(id);
        return deleted ? Result.success("✅分类已删除") : Result.fail(4004, "分类不存在");
    }

    /** 清空向量库 */
    @DeleteMapping("/clearVector")
    @OperationLog(module = "文档管理", action = "清空知识库")
    public Result<String> clearVector() {
        vectorStore.clear();
        return Result.success("✅向量知识库清空成功");
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object details = authentication.getDetails();
        return details instanceof Number number ? number.longValue() : null;
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "unnamed";
        }
        String normalized = name.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase() : "";
    }

    private Path resolveWithinUploads(String userInput) {
        Path resolved = UPLOAD_DIR.resolve(userInput).normalize();
        return resolved.startsWith(UPLOAD_DIR) ? resolved : null;
    }
}
