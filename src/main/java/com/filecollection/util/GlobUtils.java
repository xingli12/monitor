package com.filecollection.util;

import java.util.regex.Pattern;

public final class GlobUtils {
    
    private GlobUtils() {
    }
    
    /**
     * 将 glob 模式转换为正则表达式
     * 支持 *, ?, [abc] 等 glob 语法
     */
    public static boolean matchGlob(String name, String pattern) {
        String regex = globToRegex(pattern);
        return Pattern.matches(regex, name);
    }
    
    /**
     * 将 glob 模式转换为正则表达式字符串
     */
    public static String globToRegex(String glob) {
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
                        regex.append(Pattern.quote(glob.substring(i, closeIndex + 1)));
                        i = closeIndex;
                    }
                }
                case '(', ')', '+', '{', '}', '^', '$', '|', '.', '\\' -> 
                    regex.append(Pattern.quote(String.valueOf(c)));
                default -> regex.append(c);
            }
            i++;
        }
        regex.append("$");
        return regex.toString();
    }
}
