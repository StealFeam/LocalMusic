package com.zy.ppmusic.utils;

import java.io.Closeable;
import java.io.IOException;

public class IOUtils {
    public static void closeIo(Closeable closeable){
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
