package com.campusknowledgeqa.service;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Word 文档解析：
 * - .docx：按正文顺序读取段落 + 表格（表格转 Markdown，保留行列结构）
 * - .doc ：用 HWPF 抽取纯文本（旧格式表格无法结构化）
 */
@Component
public class WordDocumentParser {

    /** 解析结果：正文文本 + 表格（Markdown） */
    public record ParsedWord(String text, List<String> tables) {
    }

    public ParsedWord parse(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".docx")) {
            return parseDocx(path);
        }
        if (name.endsWith(".doc")) {
            return parseDoc(path);
        }
        return new ParsedWord("", List.of());
    }

    private ParsedWord parseDocx(Path path) {
        StringBuilder text = new StringBuilder();
        List<String> tables = new ArrayList<>();
        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(path))) {
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String content = paragraph.getText();
                    if (content != null && !content.isBlank()) {
                        text.append(content).append("\n");
                    }
                } else if (element instanceof XWPFTable table) {
                    tables.add(tableToMarkdown(table));
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ .docx 解析失败：" + e.getMessage());
        }
        return new ParsedWord(text.toString().trim(), tables);
    }

    private ParsedWord parseDoc(Path path) {
        try (HWPFDocument document = new HWPFDocument(Files.newInputStream(path));
             WordExtractor extractor = new WordExtractor(document)) {
            return new ParsedWord(extractor.getText().trim(), List.of());
        } catch (Exception e) {
            System.out.println("⚠️ .doc 解析失败：" + e.getMessage());
            return new ParsedWord("", List.of());
        }
    }

    private String tableToMarkdown(XWPFTable table) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) {
            return "";
        }
        int maxCols = 0;
        for (XWPFTableRow row : rows) {
            maxCols = Math.max(maxCols, row.getTableCells().size());
        }

        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows.size(); r++) {
            List<XWPFTableCell> cells = rows.get(r).getTableCells();
            sb.append('|');
            for (int c = 0; c < maxCols; c++) {
                String value = c < cells.size()
                        ? cells.get(c).getText().replaceAll("\\s+", " ").trim()
                        : "";
                sb.append(' ').append(value).append(" |");
            }
            sb.append('\n');
            if (r == 0) {
                sb.append('|');
                for (int c = 0; c < maxCols; c++) {
                    sb.append(" --- |");
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
