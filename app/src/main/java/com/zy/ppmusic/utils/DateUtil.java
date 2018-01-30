package com.zy.ppmusic.utils;

import java.util.Locale;

/**
 * @author ZhiTouPC
 */
public class DateUtil {
    public static DateUtil getInstance() {
        return UtilInner.UTIL;
    }

    public String getTime(String formatStr, Long mis) {
        long h = mis / (60L * 60L * 1000L);
        long m = (mis - h * 60L * 60L * 1000L) / (60L * 1000L);
        long s = (mis % (60L * 1000L)) / 1000L;
        return String.format(Locale.CHINA, formatStr, h, m, s);
    }

    public String getTime(Long mis) {
        long h = mis / (60L * 60L * 1000L);
        long m = (mis - h * 60L * 60L * 1000L) / (60L * 1000L);
        long s = (mis % (60L * 1000L)) / 1000L;
        if (h > 0) {
            return String.format(Locale.CHINA, "%d:%02d:%02d", h, m, s);
        } else {
            return String.format(Locale.CHINA, "%02d:%02d", m, s);
        }
    }

    private static class UtilInner {
        private static final DateUtil UTIL = new DateUtil();
    }

}
