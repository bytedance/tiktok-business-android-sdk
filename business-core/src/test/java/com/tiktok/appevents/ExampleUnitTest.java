package com.tiktok.appevents;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void test(){
        List<Integer> aaa = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9 ,10, 11);

        System.out.println(aaa);

        System.out.println(TTRequest.averageAssign(aaa, TTRequest.countSplitNum(aaa.size(), 100)));
    }

}