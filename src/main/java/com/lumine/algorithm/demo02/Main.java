package com.lumine.algorithm.demo02;

import java.util.*;

/**
 * @Author lancer
 * @Date 2022/4/15 18:08
 * @Description
 */
public class Main {
    public static void main(String[] args) {
        // question01(5);
        // question03(4, 4);
        // question03_V2(4, 4);
        // question02(1000);
        // question04();
        // question05();
    }


    public static void question01(int num) {
        for (int i = (int) Math.pow(10, num - 1) * 5; i < (int) Math.pow(10, num); i++) {
            if (i % 10 == 0) continue;
            // 反转数字
            int tmp = i;
            int res = 0;
            while (tmp != 0) {
                res *= 10;
                res += tmp % 10;
                tmp /= 10;
            }
            if (i % res == 0 && i / res > 1) {
                System.out.println(res + "*" + i / res + "=" + i);
            }
        }
    }

    public static void question02(int n) {
        double max = Double.MIN_VALUE;
        String maxStr = "1/2";
        int[] flag = new int[3001];
        int minC = 1;
        int d;
        for (int i = 1; i <= n; i++) {
            int c = minC;
            d = c + i;
            if (i < n) {
                flag[c] = 1;
                flag[d] = 1;
                double tmp = 1.0 * c / d;
                if (tmp > max) {
                    max = tmp;
                    maxStr = c + "/" + d;
                }
                // 找到最新的c
                while (flag[minC] != 0) {
                    minC++;
                }
            } else {
                System.out.println(c + "/" + d);
            }
        }
        System.out.println(maxStr);
    }

    public static void question03(int p, int q) {
        if (p < q) return;
        int n = 0;
        while (++n > 0) {
            int base = n * 10 + p;
            if (base * q == Integer.parseInt(p + "" + n)) {
                System.out.println("n(" + p + "," + q + ")=" + base);
                break;
            }
        }
    }


    public static void question03_V2(int p, int q) {
        if (p < q) return;
        int n = 0;
        while (++n > 0) {
            // 求出n的位数
            int count = 0;
            int tmp = n;
            while (tmp != 0) {
                tmp /= 10;
                count++;
            }

            int base = n * 10 + p;
            if (p * Math.pow(10, count) + n == base * q) {
                System.out.println("n(" + p + "," + q + ")=" + base);
                break;
            }
        }
    }


    public static void question04() {
        String start = "758064312";
        String end = "758064312";
        Queue<String> queue = new LinkedList<>();
        Map<String, Integer> map = new HashMap<>();

        // BFS
        queue.offer(start);
        // 初始化移动次数
        map.put(start, 0);

        // 四个方向
        int[] dx = {-1, 0, 1, 0}, dy = {0, 1, 0, -1};
        while (!queue.isEmpty()) {
            String curState = queue.poll();
            int distance = map.get(curState);
            if (end.equals(curState)) {
                System.out.println(distance);
                return;
            }

            // 找到0的位置
            int k = curState.indexOf('0');
            // 生成对应的坐标
            int x = k / 3, y = k % 3;
            // 四个方向
            for (int i = 0; i < dx.length; i++) {
                int a = x + dx[i], b = y + dy[i];
                if (a >= 0 && a < 3 && b >= 0 && b < 3) {
                    // 交换位置后的状态
                    char[] chars = curState.toCharArray();
                    char tmp = chars[a * 3 + b];
                    chars[a * 3 + b] = chars[k];
                    chars[k] = tmp;
                    String nextState = new String(chars);

                    if (!map.containsKey(nextState)) {
                        map.put(nextState, distance + 1);
                        queue.offer(nextState);
                    }
                }
            }
        }
        System.out.println(-1);
    }


    public static void question05() {
        Scanner sc = new Scanner(System.in);
        for (int i = 1; i <= 10; i++) {
            int total = sc.nextInt();
            if (total == 0) {
                break;
            }
            System.out.println(solve(total));
        }
    }

    public static int solve(int total) {
        int count = 0;
        while (total > 1) {
            if (total == 2) {
                count++;
                break;
            }
            int getC = total / 3;
            count += getC;
            total = getC + total % 3;
        }
        return count;
    }
}
