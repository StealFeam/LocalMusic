package com.zy.ppmusic.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author ZhiTouPC
 */
public class StreamUtils {
    protected static void closeIo(Closeable... closeable){
        for (Closeable item : closeable) {
            if (item != null) {
                try {
                    item.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
