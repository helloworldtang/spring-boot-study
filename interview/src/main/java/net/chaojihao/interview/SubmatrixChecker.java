package net.chaojihao.interview;

import java.util.Scanner;

public class SubmatrixChecker {
    // 核心方法：判断小矩阵b是否是大矩阵a的子矩阵
    public static boolean isSubmatrix(String[] a, String[] b, int m, int n) {
        // 遍历大矩阵中所有可能的子矩阵左上角位置 (i,j)
        // 边界：i最大为 m-n（因为从i开始要取n行），j同理
        for (int i = 0; i <= m - n; i++) {
            for (int j = 0; j <= m - n; j++) {
                boolean isMatch = true;
                // 逐行对比小矩阵和大矩阵的子块
                for (int di = 0; di < n; di++) {
                    // 截取大矩阵中从(i+di)行、j列开始的n个字符
                    String subRow = a[i + di].substring(j, j + n);
                    // 对比当前行是否匹配
                    if (!subRow.equals(b[di])) {
                        isMatch = false;
                        break; // 一行不匹配，直接跳过当前子块
                    }
                }
                if (isMatch) {
                    return true; // 找到匹配的子矩阵，直接返回
                }
            }
        }
        return false; // 所有子块都不匹配
    }


    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // 读取测试数据组数
        int T = scanner.nextInt();
        scanner.nextLine(); // 吸收换行符，避免影响后续字符串读取

        for (int t = 0; t < T; t++) {
            // 读取m和n（大矩阵/小矩阵的行列数）
            int m = scanner.nextInt();
            int n = scanner.nextInt();
            scanner.nextLine(); // 吸收换行符

            // 读取大矩阵m行数据
            String[] matrixA = new String[m];
            for (int i = 0; i < m; i++) {
                matrixA[i] = scanner.nextLine().trim();
            }

            // 读取小矩阵n行数据
            String[] matrixB = new String[n];
            for (int i = 0; i < n; i++) {
                matrixB[i] = scanner.nextLine().trim();
            }

            // 判断并输出结果
            if (isSubmatrix(matrixA, matrixB, m, n)) {
                System.out.println("Yes");
            } else {
                System.out.println("No");
            }
        }
        scanner.close(); // 关闭输入流
    }
}