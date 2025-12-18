package com.example.algorithm.bubble;

import java.util.Arrays;
import java.util.List;

/**
 * @Auther: cheng.tang
 * @Date: 2025/12/18
 * @Description: dubbo-demo
 */
public class BubbleSortTest {

    public static void main(String[] args) {
        List<Integer> sourceList = Arrays.asList(1, 4, 9, 0, 12, 6);
        System.out.println(sourceList);
        bubbleSort(sourceList);
        System.out.println(sourceList);
    }

    public static void bubbleSort(List<Integer> sourceList) {
        for (int i = 0; i < sourceList.size() - 1; i++) {
            for (int j = 0; j < sourceList.size() - i - 1; j++) {
                /**
                 * j>j+1则调整顺序，说明要调成升序
                 */
                if (sourceList.get(j) > sourceList.get(j + 1)) {
                    Integer temp = sourceList.get(j);
                    sourceList.set(j, sourceList.get(j + 1));
                    sourceList.set(j + 1, temp);
                }
            }
        }
    }

}
