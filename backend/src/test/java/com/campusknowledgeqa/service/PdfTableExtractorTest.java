package com.campusknowledgeqa.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTableExtractorTest {

    @Test
    void extractTableToMarkdown() throws Exception {
        File file = File.createTempFile("table-test", ".pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            String[][] data = {
                    {"Grade", "Amount", "Ratio"},
                    {"First", "5000", "5%"},
                    {"Second", "3000", "10%"},
            };
            float[] xs = {72, 220, 360};

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = 720;
                for (String[] row : data) {
                    for (int c = 0; c < row.length; c++) {
                        cs.beginText();
                        cs.setFont(font, 12);
                        cs.newLineAtOffset(xs[c], y);
                        cs.showText(row[c]);
                        cs.endText();
                    }
                    y -= 24;
                }
            }
            document.save(file);
        }

        List<Document> tables = new PdfTableExtractor().extract(file.toPath(), "奖学金标准.pdf");

        assertFalse(tables.isEmpty(), "应至少识别出一张表");
        String markdown = tables.get(0).getText();
        System.out.println("===== 抽取结果 =====\n" + markdown);

        assertTrue(markdown.contains("Grade"), "应包含表头");
        assertTrue(markdown.contains("First"), "应包含数据行");
        assertTrue(markdown.contains("|"), "应为 Markdown 表格");
    }
}
