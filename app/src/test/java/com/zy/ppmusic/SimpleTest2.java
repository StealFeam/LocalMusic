package com.zy.ppmusic;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author y-slience
 * @date 2018/3/17
 */
@RunWith(PowerMockRunner.class)
public class SimpleTest2 {

    @Test
    @PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
    @PrepareForTest(Test3.class)
    public void test() {
//        MathTest mockMath = mock(MathTest.class);
//        when(mockMath.reverseNum(0)).thenReturn(1111111);
//
//        when(mockMath.reverseNum(0)).thenAnswer(new Answer<Integer>() {
//            @Override
//            public Integer answer(InvocationOnMock invocation) throws Throwable {
//                int answer = invocation.getArgument(0);
//                System.out.println("answer中的值===" + answer);
//                return answer + 1;
//            }
//        });
//        int i = mockMath.reverseNum(0);
//        System.out.println("这是动态代理后的值+++++" + i);

        PowerMockito.mockStatic(Test3.class);
        PowerMockito.when(Test3.getId()).thenReturn("代理后");
        Assert.assertEquals("代理后",SimpleTest.getColor());
    }
}
final class Test3{
    public static String getId(){
        return "123456";
    }
}