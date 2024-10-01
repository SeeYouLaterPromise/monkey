package org.urbcomp.startdb.compress.elf.decompressor;

import gr.aueb.delorean.chimp.InputBitStream;
import org.apache.jena.sparql.function.library.leviathan.log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class improvedDecompressor {
    private int count = 0;
    private int preValueTailCount = 0;
    private long storedVal = 0;
    private int storedXORLeadZeros = Integer.MAX_VALUE;
    private int storeXORTailZeros = Integer.MAX_VALUE;
    private boolean first = true;
    private boolean endOfStream = false;

    private final InputBitStream in;

    private final static long END_SIGN = Double.doubleToLongBits(Double.NaN);

    private final static short[] leadingRepresentation = {0, 8, 12, 16, 18, 20, 22, 24};

    public improvedDecompressor(byte[] bs) {
        in = new InputBitStream(bs);
    }

    public List<Double> getValues() {
        List<Double> list = new ArrayList<>(1024);
        Double value = readValue();
        while (value != null) {
            list.add(value);
            value = readValue();
        }
        return list;
    }

    public InputBitStream getInputStream() {
        return in;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Double readValue() {
        try {
            System.out.print("Decompress the ");
            System.out.print(++count);
            System.out.println("-th value:");
            next();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        if (endOfStream) {
            return null;
        }
        return Double.longBitsToDouble(storedVal);
    }

    private void next() throws IOException {
        if (first) {
            first = false;
            preValueTailCount = in.readInt(7);
            if (preValueTailCount < 64) {
                storedVal = ((in.readLong(63 - preValueTailCount) << 1) + 1) << preValueTailCount;
            } else {
                storedVal = 0;
            }
            if (storedVal == END_SIGN) {
                endOfStream = true;
            }
        } else {
            nextValue();
        }
    }

    private void nextValue() throws IOException {
        System.out.println("\nDecompress Start: PreValue(V_t-1) is: ");
        String I3E = Long.toBinaryString(storedVal);
        System.out.println(I3E.length() == 63 ? '0' + I3E : I3E);
        long xor;
        int centerBits;
        int flag = in.readInt(2);
        switch (flag) {
            case 3:
                // case 11
                System.out.println("11: This case didn't store the delta.\n");
                storedXORLeadZeros = leadingRepresentation[in.readInt(3)];
                storeXORTailZeros = preValueTailCount;
                centerBits = 64 - preValueTailCount - storedXORLeadZeros;
//                if(centerBits == 0) {
//                    centerBits = 64;
//                }
                xor = in.readLong(centerBits) << storeXORTailZeros;
                System.out.println("The xor_decompress is: ");
                String I3E1 = Long.toBinaryString(xor);
                System.out.println(I3E.length() == 63 ? '0' + I3E1 : I3E1);

                storedVal = storedVal ^ xor;
                System.out.println("The value_after_decompress is: ");
                String I3E2 = Long.toBinaryString(storedVal);
                System.out.println(I3E.length() == 63 ? '0' + I3E2 : I3E2);

                preValueTailCount = Long.numberOfTrailingZeros(storedVal);
                if (storedVal == END_SIGN) {
                    endOfStream = true;
                }
                break;
            case 2:
                // case 10
                System.out.println("10: This case stored the delta.");
                int leadAndDelta = in.readInt(7);
                storedXORLeadZeros = leadingRepresentation[leadAndDelta >>> 4];
                int delta = leadAndDelta & 0xf;
                System.out.print("delta: ");
                System.out.println(delta);
                storeXORTailZeros = preValueTailCount - delta;
                centerBits = 64 - storedXORLeadZeros - storeXORTailZeros;
//                if(centerBits == 0) {
//                    centerBits = 16;
//                }
                xor = in.readLong(centerBits) << storeXORTailZeros;
                System.out.println("The xor_decompress is: ");
                I3E1 = Long.toBinaryString(xor);
                System.out.println(I3E.length() == 63 ? '0' + I3E1 : I3E1);

                storedVal = storedVal ^ xor;
                System.out.println("The value_after_decompress is: ");
                I3E2 = Long.toBinaryString(storedVal);
                System.out.println(I3E.length() == 63 ? '0' + I3E2 : I3E2);
//                don't forget to update 'preValueTailCount' which is used to mark the next xor result's tailing zero count.
                preValueTailCount = Long.numberOfTrailingZeros(storedVal);
                if (storedVal == END_SIGN) {
                    endOfStream = true;
                }
                break;
            case 1:
                System.out.println("Identical case!\n");
                // case 01, we do nothing, the same value as before
                break;
            default:
                System.out.println("00: only center bits.\n");
                // case 00, curTail of xor > preTail of xor -> just use the last storedTailingZeros, though more bits would be recorded.
                centerBits = 64 - storedXORLeadZeros - storeXORTailZeros;
                xor = in.readLong(centerBits) << storeXORTailZeros;
                System.out.println("The xor_decompress is: ");
                I3E1 = Long.toBinaryString(xor);
                System.out.println(I3E.length() == 63 ? '0' + I3E1 : I3E1);

                storedVal = storedVal ^ xor;
                System.out.println("The value_after_decompress is: ");
                I3E2 = Long.toBinaryString(storedVal);
                System.out.println(I3E.length() == 63 ? '0' + I3E2 : I3E2);
                preValueTailCount = Long.numberOfTrailingZeros(storedVal);
                if (storedVal == END_SIGN) {
                    endOfStream = true;
                }
                break;
        }
    }
}

