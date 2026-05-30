package org.example.config;

public final class CurrentUser {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    public static Long get() {
        return USER_ID.get();
    }

    public static long require() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new IllegalStateException("未登录");
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
