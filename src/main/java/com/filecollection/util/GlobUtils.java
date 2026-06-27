package com.filecollection.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class GlobUtils {
    
    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();
    
    private GlobUtils() {
    }
    
    /**
     * 将 glob 模式转换为正则表达式并匹配
     */
    public static boolean matchGlob(String name, String pattern) {
        return getCompiledPattern(pattern).matcher(name).matches();
    }
    
    /**
     * 获取编译后的正则 Pattern（带缓存）
     */
    private static Pattern getCompiledPattern(String glob) {
        return PATTERN_CACHE.computeIfAbsent(glob, GlobUtils::compileGlob);
    }
    
    /**
     * 将 glob 模式编译为 Pattern
     */
    private static Pattern compileGlob(String glob) {
        StringBuilder regex = new StringBuilder("^");
        int i = 0;
        while (i < glob.length()) {
            char c = glob.charAt(i);
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append(".");
                case '[' -> {
                    int closeIndex = glob.indexOf(']', i + 1);
                    if (closeIndex == -1) {
                        regex.append("\\[");
                    } else {
                        // 保留括号内容作为正则字符类，并转换否定形式 [!...] -> [^...]
                        String bracketContent = glob.substring(i, closeIndex + 1);
                        if (bracketContent.startsWith("[!") && bracketContent.length() > 3) {
                            bracketContent = "[^" + bracketContent.substring(2);
                        }
                        regex.append(bracketContent);
                        i = closeIndex;
                    }
                }
                case '(', ')', '+', '{', '}', '^', '$', '|', '.', '\\' -> 
                    regex.append("\\").append(c);
                default -> regex.append(c);
            }
            i++;
        }
        regex.append("$");
        return Pattern.compile(regex.toString());
    }
}
