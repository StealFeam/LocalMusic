package com.zy.ppmusic;

import org.junit.Test;

import java.util.ArrayList;

/**
 * @author y-slience
 * @since 2018/7/22
 */
public class Word implements Cloneable {
    private String mText;
    private ArrayList<String> mImages = new ArrayList<>();

    @Override
    protected Word clone(){
        try {
            Word doc = (Word) super.clone();
            doc.mImages = (ArrayList<String>) this.mImages.clone();
            doc.mText = this.mText;
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //...setter ...getter


    public String getText() {
        return mText;
    }

    public void setText(String text) {
        this.mText = mText;
    }

    public ArrayList<String> getImages() {
        return mImages;
    }

    public void addImages(String images) {
        this.mImages.add(images);
    }

    @Override
    public String toString() {
        return "Word{" +
                "mText='" + mText + '\'' +
                ", mImages=" + mImages +
                '}';
    }

    @Test
    public void test() {
        Word word = new Word();
        word.setText("文本");
        word.addImages("1.png");
        word.addImages("2.png");
        System.out.println(word.toString());
        Word copy = word.clone();
        copy.setText("文本2");
        copy.addImages("3.png");
        System.out.println(copy.toString());
        System.out.println(word.toString());
    }
}
