package org.urbcomp.startdb.compress.elf;


import gr.aueb.delorean.chimp.InputBitStream;
import gr.aueb.delorean.chimp.OutputBitStream;
import org.urbcomp.startdb.compress.elf.compressor.*;
import org.urbcomp.startdb.compress.elf.decompressor.*;
import org.urbcomp.startdb.compress.elf.utils.mon64;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class Main {
    private static final double LOG_10_2 = Math.log(2) / Math.log(10);

    public static int getTheta0(int E) {
//        further, I can follow the example as elf to construct an array to improve the performance.
        if (E == 0) return 1;
        else if (E > 0) return (int) Math.ceil(E * LOG_10_2);
        else return -(int) Math.floor(-E * LOG_10_2);
    }

    public static int getTheta1(int E) {
        return (int) Math.ceil(E * LOG_10_2);
    }

    public static int getFAlpha(int alpha) {
        return (int) Math.ceil(alpha * LOG_2_10);
    }


//    public static void main(String[] args) {
//        double v = 0.1;
//        double v2 = 0.01;
//        int theta = Elf64Utils.getTheta(Elf64Utils.getEFromValue(Double.doubleToLongBits(v)));
//        System.out.println("theta of 0.1: " + theta);
//        System.out.println("theta of 0.01: " + Elf64Utils.getTheta(Elf64Utils.getEFromValue(Double.doubleToLongBits(v2))));
//        System.out.println("sp of 0.1: " + Elf64Utils.getSP(v));
//        System.out.println("sp of 0.01: " + Elf64Utils.getSP(v2));
//        double v_re = Elf64Utils.get10iN(-theta);
//        System.out.println("recover by theta: " + v_re);
//        erasing(6.18);
//        erasing(6.28);
//        System.out.println("==============================================");
//        System.out.println(getTheta1(2));
//        for(int i = -10; i <= 10; i++) {
//            if(getTheta0(i) != getTheta1(i)) {
//                System.out.println("getTheta0(" + i + ") = " + getTheta0(i));
//                System.out.println("getTheta1(" + i + ") = " + getTheta1(i));
//            }
//        }
//    }

    private static boolean erasing(double v) {
        long vLong = Double.doubleToLongBits(v);
        long vPrimeLong;
        boolean flag = false;
        if (v == 0.0 || Double.isInfinite(v)) {
            vPrimeLong = vLong;
        }else if (Double.isNaN(v)) {
            vPrimeLong = 0x7ff8000000000000L;
        }else {
            int E = (((int)(vLong >> 52)) & 0x7ff) - 1023;
            int[] array = mon64.getAlphaAndBetaStar(v, Integer.MAX_VALUE);
            int BetaStar = array[1];
            System.out.println("Alpha: " + array[0]);
            System.out.println("E: " + E);
            int gAlpha = mon64.getFAlpha(array[0]) + E;
            System.out.println("gAlpha: " + gAlpha);
            int eraseBits = 52 - gAlpha;
            long mask = 0xffffffffffffffffL << eraseBits;
            long delta = (~mask) & vLong;
            if (delta != 0 && eraseBits > 4) {
                vPrimeLong = vLong & mask;
                flag = true;
            }else {
                if (delta == 0) System.out.println("Delta equals to ZERO! Precise type?");
                else System.out.println("Delta != 0, just because the erased bits are less than 4");
                vPrimeLong = vLong;
            }
        }
        mon64.showBinaryString(vLong);
        mon64.showBinaryString(vPrimeLong);
        System.out.println("Before: " + v);
        System.out.println("After : " + Double.longBitsToDouble(vPrimeLong));
        System.out.println();
        return flag;
    }

    private static void testErasing() {
        double v0 = 10;
        double delta = 1;
        while(v0 < 30) {
            if(erasing(v0 / 10)) {
                System.out.println("Erased!");
            }else {
                System.out.println("Non-erase!");
            }
            System.out.println();
            v0 += delta;
        }
    }

    private static void demo1() {
        int E = 0;
        for(int a = 0; a < 10; a++) {
            int fAlpha =(int) Math.ceil(a * LOG_2_10);
            int gAlpha = fAlpha + E;
            System.out.println("alpha: " + a + ", fAlpha: " + fAlpha + ", gAlpha: " + gAlpha);
        }

        double v = 0;
        long vLong = Double.doubleToLongBits(v);
        int E_ = mon64.getEFromValue(vLong);
        System.out.println(E_);
        System.out.println(mon64.getTheta(-1023));
    }
    private static double[][] DataSetToArray(int[] len) throws FileNotFoundException {
        final String FILE_PATH = "src/test/resources/ElfTestData";
        String fileName = "/DodgerLoopDay_TRAIN.csv";

        FileReadHelper FileReadHelper = new FileReadHelper(FILE_PATH + fileName);
        double[][] values_array = new double[1000][1000];
        double[] values;
        int i = 0;
        while((values = FileReadHelper.nextBlock()) != null) {
            values_array[i++] = values;
        }
        len[0] = i;
        return values_array;
    }

    private static void compareSize(int start, int end) throws FileNotFoundException {
        int[] len = new int[1];
        double[][] values_array = DataSetToArray(len);

//        testMonkeyCompressor(values_array[17]);
//

        double[] value = new double[end - start];
        for (int i = start; i < end; i ++) {
            value[i - start] = values_array[4][i];
        }
        int Monkey = testMonkeyCompressor(value);
        int Monkey0 = testMonkeyCompressor0(value);
//
//        for(int i = 0; i < len[0]; i++) {
//            testMonkeyCompressor(values_array[i]);
//        }

        if (Monkey0 == Monkey) System.out.println("Same");
        else if (Monkey > Monkey0) {
            System.out.println("Monkey more: " + (Monkey - Monkey0) );
        } else {
            System.out.println("Monkey less: " + (Monkey0 - Monkey) );
        }
    }

    private static void testDataset() throws FileNotFoundException {
        int[] len = new int[1];
        double[][] values_array = DataSetToArray(len);

//        testMonkeyCompressor(values_array[17]);
//
        int start = 0;
        int end = 11;
        double[] value = new double[end - start];
        for (int i = start; i < end; i ++) {
            value[i - start] = values_array[4][i];
        }
        int Monkey = testMonkeyCompressor(value);
        int Monkey0 = testMonkeyCompressor0(value);
//
//        for(int i = 0; i < len[0]; i++) {
//            testMonkeyCompressor(values_array[i]);
//        }
    }

    private static final double LOG_2_10 = Math.log(10) / Math.log(2);

    public static void testElfCompressor(double[] vs) throws FileNotFoundException {

        ICompressor compressor = new ElfCompressor();

        for (double v : vs) {
            compressor.addValue(v);

        }

        compressor.close();

        System.out.println(compressor.getKey() + ": Size after compression: " + compressor.getSize());

        byte[] result = compressor.getBytes();
        IDecompressor decompressor = new ElfDecompressor(result);
        List<Double> values = decompressor.decompress();
        assert(values.size() == vs.length);
        for (int i = 0; i < values.size(); i++) {
//            System.out.println(values.get(i));
            assert(vs[i] == values.get(i));
//            assertEquals(vs[i], values.get(i), "Value did not match" + compressor.getKey());
        }
        System.out.println("Validation successfully!");
    }

    public static int testMonkeyCompressor(double[] vs) throws FileNotFoundException {
        ICompressor compressor = new Monkey();
        for (double v : vs) {
            compressor.addValue(v);

        }

        compressor.close();

        System.out.println(compressor.getKey() + ": Size after compression: " + compressor.getSize());


        byte[] result = compressor.getBytes();

        IDecompressor decompressor = new MonkeyDe(result);

        List<Double> values = decompressor.decompress();

        assert(values.size() == vs.length);
        for (int i = 0; i < values.size(); i++) {
            System.out.println(values.get(i));
            assert(vs[i] == values.get(i));
//            assertEquals(vs[i], values.get(i), "Value did not match" + compressor.getKey());
        }

        System.out.println("Validation finished successfully!");
        return compressor.getSize();
    }

    public static int testMonkeyCompressor0(double[] vs) throws FileNotFoundException {
        ICompressor compressor = new TwoOptim();
        for (double v : vs) {
            compressor.addValue(v);

        }

        compressor.close();

        System.out.println(compressor.getKey() + ": Size after compression: " + compressor.getSize());


        byte[] result = compressor.getBytes();

        IDecompressor decompressor = new MonkeyDecompressor(result);

        List<Double> values = decompressor.decompress();

        assert(values.size() == vs.length);
        for (int i = 0; i < values.size(); i++) {
            System.out.println(values.get(i));
            assert(vs[i] == values.get(i));
//            assertEquals(vs[i], values.get(i), "Value did not match" + compressor.getKey());
        }

        System.out.println("Validation finished successfully!");
        return compressor.getSize();
    }

    private static long erase(double v) {
        long vLong = Double.doubleToLongBits(v);
        long vPrimeLong;
        if (v == 0.0 || Double.isInfinite(v)) {
            vPrimeLong = vLong;
        }else if (Double.isNaN(v)) {
            vPrimeLong = 0x7ff8000000000000L;
        }else {
            int E = (((int)(vLong >> 52)) & 0x7ff) - 1023;
            int[] array = mon64.getAlphaAndBetaStar(v, Integer.MAX_VALUE);
            System.out.println("Alpha in erase function: " + array[0]);
            int gAlpha = mon64.getFAlpha(array[0]) + E;
            System.out.println("gAlpha in erase function: " + gAlpha);
            int eraseBits = 52 - gAlpha;
            long mask = 0xffffffffffffffffL << eraseBits;
            long delta = (~mask) & vLong;
            if (delta != 0 && eraseBits > 4) {
                vPrimeLong = vLong & mask;
            }else {
                vPrimeLong = vLong;
            }
        }
        mon64.showBinaryString(vLong);
        mon64.showBinaryString(vPrimeLong);
        System.out.println("Before: " + v);
        System.out.println("After: " + Double.longBitsToDouble(vPrimeLong));
        return vPrimeLong;
    }

    private static void demo2() {
        double v = 0.123;
        long v_erased = erase(v);
        BitReader bitReader = new BitReader(v_erased);
        int first = 12;
        mon64.showBinaryString(bitReader.readBits(first), first);
        int second = 10;
        mon64.showBinaryString(bitReader.readBits(second), first + second);

        bitReader.reset();
        long initial12bits = bitReader.readBits(12);
        mon64.showBinaryString(initial12bits, 12);

        int E = ( (int)(initial12bits & 0x7ff) - 1023 );
        System.out.println("Extract E value: " + E);

        int Theta = mon64.getTheta(E);
        System.out.println("GetTheta: " + Theta);

        int Beta = mon64.getBetaStar(v, -1);
        System.out.println("Beta star value: " + Beta);

        int Alpha = Beta - Theta;
        System.out.println("Alpha value: " + Alpha);

//        In this case, our Theta is (real-theta + 1), so Alpha is real-alpha - 1
        int GAlpha = mon64.getFAlpha(Alpha) + E;
        System.out.println("GAlpha value: " + GAlpha);

        long rest = 0;
        int count = 0;
        double sum0 = Math.pow(2, E);
        System.out.println("Sum: " + sum0);
        while(count < GAlpha) {
            long bit = bitReader.readBits(1);
            count++;
            rest = (rest << 1) | bit;
            System.out.println(Long.toBinaryString(rest));
            if(bit != 0) sum0 += Math.pow(2, E - count);
            if(sum0 > Math.pow(10, Theta)) {
                System.out.println("Sum: " + sum0 + " is Large than " + Math.pow(10, Theta));

//                break;
            }
            System.out.println("Sum: " + sum0);
        }
        mon64.showBinaryString(rest, count);

        System.out.println("***************************************************************************");

        bitReader.reset();
        bitReader.readBits(12);
        rest = 0;
        count = 0;
        double sum = 1.0 / (1 << -E);
        System.out.println("Sum: " + sum);
        boolean flag = true;
        double target = Math.pow(10, Theta);
        while(count < GAlpha) {
            long bit = bitReader.readBits(1);
            count++;
            rest = (rest << 1) | bit;
            System.out.println(Long.toBinaryString(rest));
            if(flag && bit != 0) sum += (double) 1 / (1 << (count - E));
            if(flag && sum > target) {
                System.out.println("Sum: " + sum + " is Large than " + Math.pow(10, Theta));
//                revise alpha ->
                GAlpha = mon64.getFAlpha(Alpha - 1) + E;
                flag = false;
            }
            System.out.println("Sum: " + sum);
        }
        mon64.showBinaryString(rest, count);
    }
//    now we should do two things: -> as the formula to calculate the limitation sum -> to know which need to be checked further
//    -> try reading implementation.

    public static void PrintE() {
        for(int E = 20; E >= -20; E--) {
            System.out.println("E: " + E + ", Theta: " + mon64.getTheta(E));
        }
    }

    public static void TestNeedFurtherCheck() {
        for(int E = -20; E <= 20; E++) {
            if(mon64.getTheta(E - 1) != mon64.getTheta(E + 1)) {
                System.out.println(E + " need further check! " + "Its theta equals to " + mon64.getTheta(E));
            }
        }
    }

    public static void timePerformanceCompare() throws InterruptedException, IOException {
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
        startTime = System.nanoTime();
        for (double v : test_v) {
            int e = Math.getExponent(v);
        }
        endTime = System.nanoTime();
        System.out.println("GetExponent    execution time: " + (endTime - startTime) + " ns");

        // Test runtime for Test0
        startTime = System.nanoTime();
        for (double v : test_v) {
            int e = Math.getExponent(v);
        }
        endTime = System.nanoTime();
        System.out.println("getExponent    execution time: " + (endTime - startTime) + " ns");

        // Test runtime for Test0
        startTime = System.nanoTime();
        for (double v : test_v) {
            demo3(v);
        }
        endTime = System.nanoTime();
        System.out.println("demo3Direct     execution time: " + (endTime - startTime) + " ns");

        // Test runtime for Test0
        startTime = System.nanoTime();
        for (double v : test_v) {
            demo4(v);
        }
        endTime = System.nanoTime();
        System.out.println("demo4Segment    execution time: " + (endTime - startTime) + " ns");

        // Test runtime for Test0
        startTime = System.nanoTime();
        for (double v : test_v) {
            demo4(v);
        }
        endTime = System.nanoTime();
        System.out.println("demo4Segment    execution time: " + (endTime - startTime) + " ns");

        // Test runtime for Test0
        startTime = System.nanoTime();
        for (double v : test_v) {
            demo3(v);
        }
        endTime = System.nanoTime();
        System.out.println("demo3Direct     execution time: " + (endTime - startTime) + " ns");

        // Test runtime for Test0
        startTime = System.nanoTime();
        for (double v : test_v) {
            demo3(v);
        }
        endTime = System.nanoTime();
        System.out.println("demo3Direct     execution time: " + (endTime - startTime) + " ns");

        // Test runtime for Test0
        startTime = System.nanoTime();
        for (double v : test_v) {
            demo4(v);
        }
        endTime = System.nanoTime();
        System.out.println("demo4Segment    execution time: " + (endTime - startTime) + " ns");

        // Test runtime for Test0
        startTime = System.nanoTime();
        for (double v : test_v) {
            demo3(v);
        }
        endTime = System.nanoTime();
        System.out.println("demo3Direct     execution time: " + (endTime - startTime) + " ns");
    }

    public static void demo3(double v) throws IOException {
        long vLong = Double.doubleToLongBits(v);
//        System.out.println("Test encoding directly: ");
        final OutputBitStream out1 = new OutputBitStream(new byte[10000]);
        out1.writeLong(vLong, 64);
        byte[] result1 = out1.getBuffer();
        final InputBitStream in = new InputBitStream(result1);
        long initial12bits = in.readLong(12) << 52;
        long restBits = in.readLong(52);
//        Elf64Utils.showBinaryString(initial12bits);
//        Elf64Utils.showBinaryString(restBits);
        long recover = initial12bits | restBits;
        double v_ = Double.longBitsToDouble(recover);
        System.out.println(v_);
    }

    public static void demo4(double v) throws IOException {
        long vLong = Double.doubleToLongBits(v);
//        System.out.println("Test encoding segmented: ");
        final OutputBitStream out2 = new OutputBitStream(new byte[10000]);
        out2.writeLong(vLong >>> 52, 12);
        out2.writeLong(vLong, 52);
        byte[] result2 = out2.getBuffer();
        final InputBitStream in2 = new InputBitStream(result2);
        long initial12bits = in2.readLong(12) << 52;
        long restBits = in2.readLong(52);
        long recover2 = initial12bits | restBits;
//        Elf64Utils.showBinaryString(initial12bits2);
//        Elf64Utils.showBinaryString(restBits2);
//        long recover2 = initial12bits2 | restBits2;
        double v_2 = Double.longBitsToDouble(recover2);
        System.out.println(v_2);
    }

    private static void demo5() {
        final OutputBitStream out = new OutputBitStream(new byte[10000]);
        double v0 = 12;
        long storedValue = 0;
        double v1 = 8;
        double v2 = 2;

        storedValue = Double.doubleToLongBits(v0);
        out.writeLong(storedValue >>> 52, 12);


        byte[] result2 = out.getBuffer();
        final InputBitStream in = new InputBitStream(result2);

    }

    private static void demo6(double[] vs) throws IOException {
        ICompressor compressor = new Monkey();
        for (double v : vs) {
            compressor.addValue(v);

        }
        compressor.close();
        System.out.println(compressor.getKey() + ": Size after compression: " + compressor.getSize());

        byte[] result = compressor.getBytes();
        final InputBitStream in = new InputBitStream(result);
        long output = in.readLong(compressor.getSize());
        System.out.println(Long.toBinaryString(output));
    }
    private static final double[] value_test = new double[] {
            1.034960853885393,
//                    -1.5258347060087982,
//            0.9184407337675271,
//                    -1.6438868373589703,
//            0.30111777733713274,
//                    -1.2445118712713676,
            0.136761091304189,
    };

    public static String decimalToBinary(double decimal) {
        // Separate the integer and fractional parts
        long integerPart = (long) decimal;
        double fractionalPart = decimal - integerPart;

        // Convert the integer part to binary
        String integerBinary = Long.toBinaryString(integerPart);

        // Convert the fractional part to binary
        StringBuilder fractionalBinary = new StringBuilder();
        while (fractionalPart > 0) {
            if (fractionalBinary.length() > 63) { // Limit the precision to avoid infinite loops
                return integerBinary + "." + fractionalBinary + "...";
            }
            fractionalPart *= 2;
            if (fractionalPart >= 1) {
                fractionalBinary.append(1);
                fractionalPart -= 1;
            } else {
                fractionalBinary.append(0);
            }
        }

        return integerBinary + (fractionalBinary.length() > 0 ? "." + fractionalBinary.toString() : "");
    }

    public static void whichType(double v) {
        long vLong = Double.doubleToLongBits(v);
        mon64.showBinaryString(vLong);
        int e = Math.getExponent(v);
        double sum = Math.pow(2, e);
        long mask = (1L << 1) - 1;
        for (int i = 13; i <= 64; i++) {
            long bit = (vLong >>> (64 - i)) & mask;
            if (bit == 1) sum += Math.pow(2, e - i + 12);
        }
        if (sum == v) {
            System.out.println(sum);
            System.out.println("Precise Type.");
        }
        else {
            System.out.println(sum);
            System.out.println("Approximate Type.");
        }
    }

    private static void demo7() {
//        To demonstrate the result of log_2^{10} * ceil(E * log_{10}^2) whether E or not.
        for (int i = -5; i <= 5; i++) {
            System.out.println("Now E value: " + i);
            double middleItem = Math.ceil(i * LOG_10_2);
            double left = LOG_2_10 * Math.ceil(i * LOG_10_2);
            System.out.println("Middle Item: " + middleItem);
            System.out.println("Left Item :  " + left);
//            if (LOG_2_10 * Math.ceil(i * LOG_10_2) != i) {
//                System.out.println("doesn't match:");
//            }
//            if(left != i) {
//                System.out.println("Left doesn't equals to Right!");
//            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
//        erasing(0.25);
//        erasing(0.26);
//        erasing(0.73);
//        erasing(0.75);
//        erasing(0.76);

        double[] values = {1000, 123.333,100.00, 0.011, 0.00022, 0.14, 0.12, 0.17, 0.005, 0.2, 0.1, 0.01, 0.001111, 0.5, 0.21};
        double v0 = 0.011;
        System.out.println(v0 * 100);
        System.out.println(v0 * 1000);
        System.out.println(v0 * 10000);

        System.out.println("See new method:");
        for (double value : values) {
            System.out.println("Value: " + value + ", Beta: " + mon64.ourGetBeta(value, Integer.MAX_VALUE));
        }
        System.out.println("Elf's method");
        for (double value : values) {
            System.out.println("Value: " + value + ", Beta: " + mon64.getBetaStar(value, Integer.MAX_VALUE));
        }
//
//        double v = 0.14;
//        int sp = mon64.getSP(v);
//        int beta = mon64.getSignificantCount(v, sp, Integer.MAX_VALUE);
//        System.out.println("Value: " + v + ", sp: " + sp + ", beta: " + beta);
//        System.out.println("Beta: " + mon64.getSP(-0.14));


//        double v = 0.5 + Math.pow(2, -10);
//        mon64.getTheta()
//        System.out.println(v);
//        erasing(v);
//        demo7();
//        System.out.println(1.0 / (1L << 15));
//        erasing(value_test[1]);
//        double v1 = 0.14;
//        int[] alphaAndBeta1 = mon64.getAlphaAndBetaStar(0.14, Integer.MAX_VALUE);
//        System.out.println(Long.numberOfTrailingZeros(Double.doubleToLongBits(Double.NaN)));
//        erasing(31.846154);
//        erasing(32.115385);

//        for (int i = -10; i <= 10; i++) {
//            System.out.println("E = " + i + ", Theta = " + mon64.getTheta(i));
//        }

//        compareSize(55, 58);

//        testDataset();

//        testMonkeyCompressor0(value_test);

//        testMonkeyCompressor(value_test);
//
//        testElfCompressor(value_test);

//        Discover that 10^(theta) case can be deduced from E value directly.
//        double v = 0.001;
//        int E = Math.getExponent(v);
//        System.out.println(E);
//        int theta = mon64.getTheta(E);
//        System.out.println(theta);
//        System.out.println(Math.pow(10, theta));
    }



//    public static void main(String[] args) throws FileNotFoundException {
////        double v = 9;
////        int lastBetaStar = Integer.MAX_VALUE;
////        long vLong = Double.doubleToLongBits(v);
////        System.out.println(Double.longBitsToDouble(vLong));
////        int[] alphaAndBetaStar = Elf64Utils.getAlphaAndBetaStar(v, lastBetaStar);
////        int E = Elf64Utils.getEFromValue(vLong);
////        int gAlpha = Elf64Utils.getFAlpha(alphaAndBetaStar[0]) + E;
////        int eraseBits = 52 - gAlpha;
////        System.out.println("erasedBits: " + eraseBits);
////        long mask = 0xffffffffffffffffL << eraseBits;
////        Elf64Utils.showBinaryString(mask);
//////        delta == 0 means cannot perform erasing
////        long delta = (~mask) & vLong;
////        Elf64Utils.showBinaryString(vLong);
////        Elf64Utils.showBinaryString(delta);
////        double v = 3;
////        System.out.println(Math.getExponent(v));
////        System.out.println(Elf64Utils.getEFromValue(Double.doubleToLongBits(v)));
////        for (int i = -40; i <= 40; ++i) {
////            System.out.println("E = " + i + " Theta: " + Elf64Utils.getTheta(i));
////        }
////        double v = 0.49424548146184016;
////        double value = 42.333333;
////        BigDecimal bigValue = BigDecimal.valueOf(value);
////        int alpha = bigValue.scale();
////        int[] spANDFlag = Elf64Utils.getSPAnd10iNFlag(value);
////        int beta = spANDFlag[1] == 1 ? 0 : alpha + spANDFlag[0] + 1;
////        System.out.println(bigValue.scale());
////        System.out.println(Elf64Utils.getDecimalPlaceCount(v));
////          double v = 88.51872;
////          erasing(v);
////          erasing(88.51789);
////        testDataset();
//        testCompressor(value_test);
//        testCompressor2(value_test);
//
////        double[] value1 = {1.1, 1.2, 1.3};
////        testCompressor(value1);
//    }





}
