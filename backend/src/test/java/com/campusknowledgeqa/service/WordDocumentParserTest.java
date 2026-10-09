package com.campusknowledgeqa.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordDocumentParserTest {

    @Test
    void parseDocxWithTable() throws Exception {
        File file = File.createTempFile("word-test", ".docx");
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText("奖学金评审标准如下：");

            XWPFTable table = document.createTable(3, 3);
            String[][] data = {
                    {"等级", "金额", "比例"},
                    {"一等", "5000", "5%"},
                    {"二等", "3000", "10%"},
            };
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    table.getRow(r).getCell(c).setText(data[r][c]);
                }
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                document.write(out);
            }
        }

        WordDocumentParser.ParsedWord parsed = new WordDocumentParser().parse(file.toPath());
        System.out.println("===== 正文 =====\n" + parsed.text());
        System.out.println("===== 表格 =====");
        parsed.tables().forEach(System.out::println);

        assertTrue(parsed.text().contains("奖学金"), "正文应包含段落文本");
        assertFalse(parsed.tables().isEmpty(), "应识别出表格");
        assertTrue(parsed.tables().get(0).contains("等级"), "表格应包含表头");
        assertTrue(parsed.tables().get(0).contains("一等"), "表格应包含数据行");
    }
}
