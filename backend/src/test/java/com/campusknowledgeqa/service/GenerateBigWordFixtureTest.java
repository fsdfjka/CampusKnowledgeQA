package com.campusknowledgeqa.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;

/** 仅用于生成一个“大表格”docx，供手工/端到端测试用 */
class GenerateBigWordFixtureTest {

    @Test
    void generate() throws Exception {
        File out = new File("C:/Users/34055/AppData/Local/Temp/opencode/big-table.docx");
        if (out.getParentFile() != null) {
            out.getParentFile().mkdirs();
        }
        try (XWPFDocument document = new XWPFDocument()) {
            document.createParagraph().createRun().setText("大型表格测试文档");
            XWPFTable table = document.createTable(401, 4);
            table.getRow(0).getCell(0).setText("学号");
            table.getRow(0).getCell(1).setText("姓名");
            table.getRow(0).getCell(2).setText("学院");
            table.getRow(0).getCell(3).setText("备注");
            for (int r = 1; r <= 400; r++) {
                table.getRow(r).getCell(0).setText("2026" + String.format("%04d", r));
                table.getRow(r).getCell(1).setText("学生" + r);
                table.getRow(r).getCell(2).setText("计算机与信息科学学院");
                table.getRow(r).getCell(3).setText("备注信息备注信息备注信息备注信息");
            }
            try (FileOutputStream outStream = new FileOutputStream(out)) {
                document.write(outStream);
            }
        }
        System.out.println("written: " + out.getAbsolutePath() + " size=" + out.length());
    }
}
