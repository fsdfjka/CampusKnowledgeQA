package com.campusknowledgeqa.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 PDFBox 3 的表格抽取：按文本坐标聚类出行/列，转成 Markdown。
 * 适用于规则网格表格；复杂合并单元格、扫描件效果有限（可改用多模态模型）。
 */
@Component
public class PdfTableExtractor {

    /** 同一行的 y 坐标容差 */
    private static final float LINE_Y_TOLERANCE = 3.5f;
    /** 同一列的 x 坐标容差 */
    private static final float COLUMN_X_TOLERANCE = 20f;
    /** 单元格间距阈值 = 该系数 * 字号 */
    private static final float CELL_GAP_FACTOR = 1.8f;

    /**
     * 抽取 PDF 中所有表格，每张表作为一个 Document（Markdown 文本）
     */
    public List<Document> extract(Path pdfPath, String sourceName) {
        List<Document> tables = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                List<List<Cell>> rows = toRows(collectPositions(document, page));
                for (String markdown : detectTables(rows)) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("source", sourceName);
                    metadata.put("contentType", "table");
                    metadata.put("page_number", page);
                    tables.add(new Document(markdown, metadata));
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ 表格抽取失败：" + e.getMessage());
        }
        return tables;
    }

    /** 收集指定页所有文本块的坐标信息 */
    private List<TextPosition> collectPositions(PDDocument document, int page) throws IOException {
        List<TextPosition> positions = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void writeString(String text, List<TextPosition> textPositions) {
                positions.addAll(textPositions);
            }
        };
        stripper.setSortByPosition(true);
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        stripper.getText(document);
        return positions;
    }

    /** 按 y 归并成行，再把每行按 x 间隙切成单元格 */
    private List<List<Cell>> toRows(List<TextPosition> positions) {
        List<TextPosition> sorted = new ArrayList<>(positions);
        sorted.sort(Comparator.comparingDouble(TextPosition::getYDirAdj)
                .thenComparingDouble(TextPosition::getXDirAdj));

        List<List<TextPosition>> lines = new ArrayList<>();
        for (TextPosition position : sorted) {
            if (position.getUnicode() == null || position.getUnicode().isBlank()) {
                continue;
            }
            List<TextPosition> line = null;
            for (List<TextPosition> candidate : lines) {
                if (Math.abs(candidate.get(0).getYDirAdj() - position.getYDirAdj()) <= LINE_Y_TOLERANCE) {
                    line = candidate;
                    break;
                }
            }
            if (line == null) {
                line = new ArrayList<>();
                lines.add(line);
            }
            line.add(position);
        }

        List<List<Cell>> rows = new ArrayList<>();
        for (List<TextPosition> line : lines) {
            line.sort(Comparator.comparingDouble(TextPosition::getXDirAdj));
            rows.add(toCells(line));
        }
        return rows;
    }

    private List<Cell> toCells(List<TextPosition> line) {
        List<Cell> cells = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        float cellStart = -1;
        TextPosition prev = null;

        for (TextPosition position : line) {
            if (prev != null) {
                float gap = position.getXDirAdj() - (prev.getXDirAdj() + prev.getWidthDirAdj());
                float threshold = Math.max(prev.getFontSizeInPt(), position.getFontSizeInPt()) * CELL_GAP_FACTOR;
                if (gap > threshold) {
                    cells.add(new Cell(cellStart, buffer.toString().trim()));
                    buffer.setLength(0);
                    cellStart = -1;
                }
            }
            if (cellStart < 0) {
                cellStart = position.getXDirAdj();
            }
            buffer.append(position.getUnicode());
            prev = position;
        }
        if (buffer.length() > 0) {
            cells.add(new Cell(cellStart, buffer.toString().trim()));
        }
        cells.removeIf(cell -> cell.text().isEmpty());
        return cells;
    }

    /** 连续多行（≥2 个单元格）视为一张表 */
    private List<String> detectTables(List<List<Cell>> rows) {
        List<String> result = new ArrayList<>();
        int i = 0;
        while (i < rows.size()) {
            if (rows.get(i).size() < 2) {
                i++;
                continue;
            }
            int j = i;
            while (j < rows.size() && rows.get(j).size() >= 2) {
                j++;
            }
            List<List<Cell>> block = rows.subList(i, j);
            if (block.size() >= 2) {
                result.add(toMarkdown(block));
            }
            i = j;
        }
        return result;
    }

    private String toMarkdown(List<List<Cell>> block) {
        List<Float> columnX = new ArrayList<>();
        for (List<Cell> row : block) {
            for (Cell cell : row) {
                addColumn(columnX, cell.x());
            }
        }
        Collections.sort(columnX);
        int cols = columnX.size();

        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < block.size(); r++) {
            String[] line = new String[cols];
            for (Cell cell : block.get(r)) {
                int c = nearestColumn(columnX, cell.x());
                line[c] = line[c] == null ? cell.text() : line[c] + " " + cell.text();
            }
            sb.append('|');
            for (String value : line) {
                sb.append(' ').append(value == null ? "" : value).append(" |");
            }
            sb.append('\n');
            if (r == 0) {
                sb.append('|');
                for (int c = 0; c < cols; c++) {
                    sb.append(" --- |");
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private void addColumn(List<Float> columnX, float x) {
        for (float existing : columnX) {
            if (Math.abs(existing - x) <= COLUMN_X_TOLERANCE) {
                return;
            }
        }
        columnX.add(x);
    }

    private int nearestColumn(List<Float> columnX, float x) {
        int best = 0;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < columnX.size(); i++) {
            float distance = Math.abs(columnX.get(i) - x);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    private record Cell(float x, String text) {
    }
}
