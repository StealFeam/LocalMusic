package com.zy.ppmusic;


/**
 * @author y-slience
 * @date 2018/3/17
 */

public class SimpleTest extends B{
    public static String flavor = "";


    public class A{
        public void method1(){
            System.out.println("method1");
            method2();
        }
    }

    private String selfMethod(){
        return "private";
    }

    public final String finalMethod(){
        return "final";
    }

    public static String getColor(){
        return flavor;
    }

}

class B{
    public void method2(){

    }
}