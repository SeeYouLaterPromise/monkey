package org.urbcomp.startdb.compress.elf;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
public class PerformanceTest {
    private static final double[] testValues = {0.5, 1, 10, 100, 0.01, 0.0001, 1000, 0.0000001, 1000000};
    private final static int[] t_up =
            new int[] {1, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 9, 9, 9, 10, 10, 10, 10, 11, 11, 11, 12,
                    12, 12, 13,};

    private final static int[] t_down =
            new int[] {0, 0, 0, -1, -1, -1, -2, -2, -2, -3, -3, -3, -3, -4, -4, -4, -5, -5, -5, -6, -6, -6, -6, -7, -7,
                    -7, -8, -8, -8, -9, -9, -9, -9, -10, -10, -10, -11, -11, -11, -12};

    private final static long[] mapSPGreater1 =
            new long[] {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000};

    private final static double[] mapSPLess1 =
            new double[] {1, 0.1, 0.01, 0.001, 0.0001, 0.00001, 0.000001, 0.0000001, 0.00000001,
                    0.000000001, 0.0000000001};

    private static final double LOG_10_2 = Math.log(2) / Math.log(10);

    public static void main(String[] args) throws InterruptedException {
        int n = 100000;
        double[] test_v = new double[n];
        double initial = 0;
        double delta = 0.1;
        for(int i = 0; i < n; i++) {
            test_v[i] = initial;
            initial += delta;
        }
        long startTime, endTime;
        startTime = System.nanoTime();
        Thread.sleep(1000);
        endTime = System.nanoTime();
        // Test runtime for Test0
//        startTime = System.nanoTime();
//        for (double v : test_v) {
//            int e = 2;
//            double ans = Math.exp(e);
////            int e = (int)((Double.doubleToRawLongBits(v) >> 52) & 0x7ff) - 1023;
//        }
//        endTime = System.nanoTime();
//        System.out.println(startTime);
//        System.out.println(endTime);
//        System.out.println("GetExponent*    execution time: " + (endTime - startTime) + " ns");

//        startTime = System.nanoTime();
//        Thread.sleep(1000);
//        endTime = System.nanoTime();
        // Test runtime for Test0
//        startTime = System.nanoTime();
//        for (double v : test_v) {
//            int e = 2;
//            double ans = Math.exp(e);
////            int e = (int)((Double.doubleToRawLongBits(v) >> 52) & 0x7ff) - 1023;
//        }
//        endTime = System.nanoTime();
//        System.out.println(startTime);
//        System.out.println(endTime);
//        System.out.println("GetExponent*    execution time: " + (endTime - startTime) + " ns");

        // Test runtime for Test0
        startTime = System.nanoTime();
        for (double v : test_v) {
            int e = Math.getExponent(v);
        }
        endTime = System.nanoTime();
        System.out.println(startTime);
        System.out.println(endTime);
        System.out.println("GetExponent    execution time: " + (endTime - startTime) + " ns");

        // Test runtime for Test0
        startTime = System.nanoTime();
        for (double v : test_v) {
            int e = Math.getExponent(v);
        }
        endTime = System.nanoTime();
        System.out.println("getExponent    execution time: " + (endTime - startTime) + " ns");

        // Test runtime for Test1
        startTime = System.nanoTime();
        for (double v : test_v) {
            MonkeyGetTheta(v);
        }
        endTime = System.nanoTime();
        System.out.println("MonkeyGetTheta execution time: " + (endTime - startTime) + " ns");

        // Test runtime for Test2
        startTime = System.nanoTime();
        for (double v : test_v) {
            ElfGetSP(v);
        }
        endTime = System.nanoTime();
        System.out.println("ElfGetSP       execution time: " + (endTime - startTime) + " ns");
    }

    public static int MonkeyGetTheta(double v) {
        // Copy the implementation of Test1 from your code
        int E = Math.getExponent(v);
//        int E = (int)((Double.doubleToRawLongBits(v) >> 52) & 0x7ff) - 1023;
        if (E >= t_up.length) {
            return (int) Math.ceil(E * LOG_10_2);
        } else if (E >= 0) {
            return t_up[E];
        } else if (E < -t_down.length) {
            return -(int) Math.floor(-E * LOG_10_2);
        } else {
            return t_down[-E - 1];
        }
    }

    public static int ElfGetSP(double v) {
        // Copy the implementation of Test2 from your code
        if (v >= 1) {
            int i = 0;
            while (i < mapSPGreater1.length - 1) {
                if (v < mapSPGreater1[i + 1]) {
                    return i;
                }
                i++;
            }
        } else {
            int i = 1;
            while (i < mapSPLess1.length) {
                if (v >= mapSPLess1[i]) {
                    return -i;
                }
                i++;
            }
        }
        return (int) Math.floor(Math.log10(v));
    }
}
