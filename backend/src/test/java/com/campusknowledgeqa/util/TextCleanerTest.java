package com.campusknowledgeqa.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextCleanerTest {

    @Test
    void cleansText() {
        String input = "第一行\u0000内容\n"
                + "福州理工学院\n"
                + "第1页\n"
                + "福州理工学院\n"
                + "第2页\n"
                + "福州理工学院\n"
                + "第3页\n"
                + "第二行    有很多   空格\n"
                + "\n\n\n\n"
                + "结尾";

        String out = TextCleaner.clean(input);
        System.out.println("===== 清洗结果 =====\n" + out);

        assertFalse(out.contains("福州理工学院"), "重复 3 次的页眉应被删除");
        assertFalse(out.contains("\u0000"), "控制字符应被删除");
        assertFalse(out.contains("    "), "连续空格应被压缩");
        assertTrue(out.contains("第一行内容"), "正文应保留");
        assertTrue(out.contains("第二行 有很多 空格"), "空白应压缩为单个空格");
        assertTrue(out.contains("结尾"), "结尾应保留");
    }
}
