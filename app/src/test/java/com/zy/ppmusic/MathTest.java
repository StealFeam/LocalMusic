package com.zy.ppmusic;

import com.zy.ppmusic.utils.PrintOut;

import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;

/**
 * @author ZhiTouPC
 * @date 2017/12/20
 * 算法练习
 */
public class MathTest {
    /**
     * simple
     *
     * @param array  [2,7,11,15]
     * @param target 9
     * @return bacause 2+7=9 so return [0,1]
     */
    private int[] getNumSum(int[] array, int target) {
        int[] result = new int[2];
        for (int i = 0; i < array.length; i++) {
            for (int j = i + 1; j < array.length; j++) {
                if (array[i] + array[j] == target) {
                    result[0] = i;
                    result[1] = j;
                    return result;
                }
            }
        }
        return result;
    }

    /**
     * simple
     *
     * @param array  [2,7,11,15]
     * @param target 9
     * @return bacause 2+7=9 so return [0,1]
     */
    private int[] twoSum(int[] array, int target) {
        HashMap<Integer, Integer> cache = new HashMap<>();
        /*
            map集合key 保存target与当前array数值的差，value保存当前位置
            如果之后的数值出现与前面key的值相同的话就返回
         */
        for (int i = 0; i < array.length; i++) {
            if (cache.containsKey(array[i])) {
                return new int[]{cache.get(array[i]), i};
            }
            cache.put(target - array[i], i);
        }
        PrintOut.print(cache.toString());
        return null;
    }


    /**
     * @param num 123
     * @return 321
     */
    private int reverseNum(int num) {
        //simple 123 -> 321
        long result = 0;
        for (; num != 0; num /= 10) {
            result = result * 10 + num % 10;
        }
        return result >= Integer.MAX_VALUE || result <= Integer.MIN_VALUE ? 0 : (int) result;
    }


    /**
     * 计算一个数字与它的反向数值是否相同
     *
     * @param num 1234321
     * @return boolean
     */
    private boolean reverseNumIsEqual(int num) {
        int copyNum = num, reverseNum = 0;
        for (; copyNum != 0; copyNum /= 10) {
            reverseNum = reverseNum * 10 + copyNum % 10;
        }
        PrintOut.print(String.format(Locale.CHINA, "reverse=%d,num=%d", reverseNum, num));
        return reverseNum == num;
    }

    /**
     * 计算一个数字与它的反向数值是否相同
     * up
     *
     * @param num 1234321
     * @return boolean
     */
    private boolean reverseNumIsEqualTwo(int num) {
        int reverseNum = 0;
        for (; num > reverseNum; num /= 10) {
            reverseNum = reverseNum * 10 + num % 10;
        }
        PrintOut.print(String.format(Locale.CHINA, "reverse=%d,num=%d", reverseNum, num));
        return reverseNum == num || reverseNum / 10 == num;
    }


    @Test
    public void test() {
        PrintOut.print(reverseNum(1234567891));
        int[] numSum = getNumSum(new int[]{2, 7, 11, 14}, 21);
        printIntArray(numSum);

        int[] ints = twoSum(new int[]{2, 7, 11, 14}, 21);
        if (ints != null) {
            printIntArray(ints);
        }

        PrintOut.print(reverseNumIsEqual(75614));
        PrintOut.print(reverseNumIsEqualTwo(1234321));



    }

    private void printIntArray(int[] array) {
        for (int i : array) {
            PrintOut.print(i);
        }
    }


}
