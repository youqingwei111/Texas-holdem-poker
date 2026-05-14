package com.poker.common;

/**
 * 通用工具类
 */
public class Utils {

    private Utils() {}

    /**
     * 生成随机字符串
     */
    public static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    /**
     * 生成房间号（6位数字）
     */
    public static String generateRoomCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
}