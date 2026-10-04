package com.vietduc.plastic.util;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Theo dõi các tính năng người dùng vừa truy cập gần đây,
 * lưu trong HttpSession và sắp xếp theo thời gian gần nhất.
 *
 * Dùng để đẩy 3 mục "Tạo đơn", "Tổng hợp SX", "Báo cáo" ra Dashboard.
 */
public final class RecentFeaturesTracker {

    public static final String SESSION_KEY = "recentFeatures";

    /** Định nghĩa các mục tính năng hiển thị ngoài Dashboard. */
    public static final String FEATURE_CREATE_ORDER   = "CREATE_ORDER";
    public static final String FEATURE_PRODUCTION     = "PRODUCTION";
    public static final String FEATURE_REPORT         = "REPORT";

    public static class Entry {
        private final String key;
        private final LocalDateTime visitedAt;

        public Entry(String key, LocalDateTime visitedAt) {
            this.key = key;
            this.visitedAt = visitedAt;
        }

        public String getKey() { return key; }
        public LocalDateTime getVisitedAt() { return visitedAt; }
    }

    /** Ghi nhận user vừa truy cập feature `key`. */
    @SuppressWarnings("unchecked")
    public static void touch(HttpSession session, String key) {
        if (session == null || key == null) return;
        List<Entry> list = (List<Entry>) session.getAttribute(SESSION_KEY);
        if (list == null) {
            list = new ArrayList<>();
            session.setAttribute(SESSION_KEY, list);
        }
        // Bỏ entry trùng key (nếu có) rồi thêm mới ở cuối
        list.removeIf(e -> key.equals(e.key));
        list.add(new Entry(key, LocalDateTime.now()));
        // Chỉ giữ 10 entry gần nhất
        if (list.size() > 10) {
            list.sort((a, b) -> a.getVisitedAt().compareTo(b.getVisitedAt()));
            while (list.size() > 10) list.remove(0);
        }
    }

    /** Lấy danh sách entry, sắp xếp thời gian giảm dần (gần nhất trước). */
    @SuppressWarnings("unchecked")
    public static List<Entry> getSorted(HttpSession session) {
        if (session == null) return List.of();
        List<Entry> list = (List<Entry>) session.getAttribute(SESSION_KEY);
        if (list == null) return List.of();
        List<Entry> copy = new ArrayList<>(list);
        // Sắp xếp giảm dần: visitedAt lớn hơn (mới hơn) lên trước
        copy.sort((a, b) -> b.getVisitedAt().compareTo(a.getVisitedAt()));
        return copy;
    }

    private RecentFeaturesTracker() {}
}
