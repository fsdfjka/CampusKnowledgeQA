package com.campusknowledgeqa.util;

import java.util.HashMap;
import java.util.Map;

/** 文本清洗：去控制字符、压缩空白、去重复行（页眉/页脚/页码等） */
public final class TextCleaner {

    /** 判定为“重复页眉页脚”的阈值 */
    private static final int REPEAT_MIN_COUNT = 3;
    private static final int REPEAT_MAX_LENGTH = 30;

    private TextCleaner() {
    }

    public static String clean(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text
                .replace('\u00A0', ' ')                               // 不间断空格 → 普通空格
                .replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")          // 去掉控制字符
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")                 // 连续水平空白压缩为一个空格
                .replaceAll(" *\\n *", "\n")                          // 行首尾空白
                .replaceAll("\\n{3,}", "\n\n")                        // 多个空行 → 一个空行
                .trim();
        return dedupeRepeatedLines(normalized);
    }

    /** 删除重复出现且较短的整行（典型是 PDF 每页的页眉、页脚、页码） */
    private static String dedupeRepeatedLines(String text) {
        String[] lines = text.split("\n", -1);

        Map<String, Integer> frequency = new HashMap<>();
        for (String line : lines) {
            String key = line.trim();
            if (!key.isEmpty()) {
                frequency.merge(key, 1, Integer::sum);
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String key = line.trim();
            boolean repeated = !key.isEmpty()
                    && key.length() <= REPEAT_MAX_LENGTH
                    && frequency.getOrDefault(key, 0) >= REPEAT_MIN_COUNT;
            if (repeated) {
                continue;
            }
            sb.append(line).append('\n');
        }
        return sb.toString().replaceAll("\\n{3,}", "\n\n").trim();
    }
}
