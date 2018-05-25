package com.zy.ppmusic;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * @author y-slience
 * @date 2018/5/19
 */
public class ByteTest {
    @Test
    public void testCharLength() throws UnsupportedEncodingException {
        String c = "我";
        String a = "a";
        byte[] bytesUtf = c.getBytes("UTF-8");
        System.out.println("length="+bytesUtf.length);
        byte[] byteGBK = c.getBytes("GBK");
        System.out.println("GBklength="+byteGBK.length);
        byte[] aUtf = a.getBytes("UTF-8");
        byte[] aGBk = a.getBytes("GBK");
        System.out.println("utfLengt="+aUtf.length);
        System.out.println("gbkLength="+aGBk.length);
    }

    @Test
    public void testIntegerLength(){
        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;
        int zero = 0;
        byte[] minBytes = String.valueOf(min).getBytes(Charset.forName("UTF-8"));
        System.out.println("value="+min+"minLength="+minBytes.length);
        byte[] maxBytes = String.valueOf(max).getBytes(Charset.forName("UTF-8"));
        System.out.println("value="+max+"minLength="+maxBytes.length);
        byte[] zeroBytes = String.valueOf(zero).getBytes(Charset.forName("UTF-8"));
        System.out.println("value="+zero+"minLength="+zeroBytes.length);
    }
}
