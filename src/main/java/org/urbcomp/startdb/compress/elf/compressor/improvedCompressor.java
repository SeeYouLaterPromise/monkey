package org.urbcomp.startdb.compress.elf.compressor;

import gr.aueb.delorean.chimp.OutputBitStream;

public class improvedCompressor {
    private int count = 0;
    private int preValueTailCount = Integer.MAX_VALUE;
    private int storedXORLeadCount = Integer.MAX_VALUE;

    private int storedXORTailCount = Integer.MAX_VALUE;
    private long storedVal = 0;
    private boolean first = true;
    private int size;
    private final static long END_SIGN = Double.doubleToLongBits(Double.NaN);

    public final static short[] leadingRepresentation = {
            0, 0, 0, 0, 0, 0, 0, 0,
            1, 1, 1, 1, 2, 2, 2, 2,
            3, 3, 4, 4, 5, 5, 6, 6,
            7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7
    };

    public final static short[] leadingRound = {
            0, 0, 0, 0, 0, 0, 0, 0,
            8, 8, 8, 8, 12, 12, 12, 12,
            16, 16, 18, 18, 20, 20, 22, 22,
            24, 24, 24, 24, 24, 24, 24, 24,
            24, 24, 24, 24, 24, 24, 24, 24,
            24, 24, 24, 24, 24, 24, 24, 24,
            24, 24, 24, 24, 24, 24, 24, 24,
            24, 24, 24, 24, 24, 24, 24, 24
    };
    //    public final static short FIRST_DELTA_BITS = 27;

    private final OutputBitStream out;

    public improvedCompressor() {
        out = new OutputBitStream(
                new byte[10000]);  // for elf, we need one more bit for each at the worst case
        size = 0;
    }

    public OutputBitStream getOutputStream() {
        return this.out;
    }

    /**
     * Adds a new long value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    public int addValue(long value) {
        System.out.print("Compress the ");
        System.out.print(++count);
        System.out.println("-th value:");
        if (first) {
            return writeFirst(value);
        } else {
            return compressValue(value);
        }
    }

    /**
     * Adds a new double value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    public int addValue(double value) {
        System.out.print("Compress the ");
        System.out.print(++count);
        System.out.println("-th value:");
        if (first) {
            return writeFirst(Double.doubleToRawLongBits(value));
        } else {
            return compressValue(Double.doubleToRawLongBits(value));
        }
    }

    private int writeFirst(long value) {
        first = false;
        storedVal = value;
        preValueTailCount = Long.numberOfTrailingZeros(value);
        out.writeInt(preValueTailCount, 7);
        if (preValueTailCount < 64) {
            out.writeLong(storedVal >>> (preValueTailCount + 1), 63 - preValueTailCount);
            size += 70 - preValueTailCount;
            return 70 - preValueTailCount;
        } else {
            size += 7;
            return 7;
        }
    }

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() {
        addValue(END_SIGN);
        out.writeBit(false);
        out.flush();
    }

    private int compressValue(long value) {
        int thisSize = 0;
        long xor = storedVal ^ value;
        System.out.println("\nCompress Start: storedValue(V_t-1) is:");
        String I3E1 = Long.toBinaryString(storedVal);
        System.out.println(I3E1.length() == 63 ? '0' + I3E1 : I3E1);

        System.out.println("Compress: value(V_t) is: ");
        String I3E2 = Long.toBinaryString(value);
        System.out.println(I3E2.length() == 63 ? '0' + I3E2 : I3E2);

        System.out.println("Compress: The xor result is: ");
        String I3E3 = Long.toBinaryString(xor);
        System.out.println(I3E3.length() == 63 ? '0' + I3E3 : I3E3);
        if (xor == 0) {
            // case 01
            System.out.println("Identical case.\n");
            out.writeInt(1, 2);
            size += 2;
            thisSize += 2;
        } else {
            int leadingZeros = leadingRound[Long.numberOfLeadingZeros(xor)];
            int trailingZeros = Long.numberOfTrailingZeros(xor);

            if (leadingZeros == storedXORLeadCount && trailingZeros >= storedXORTailCount) {
                // case 00
                int centerBits = 64 - storedXORLeadCount - storedXORTailCount;
                int len = 2 + centerBits;
                if(len > 64) {
                    out.writeInt(0, 2);
                    out.writeLong(xor >>> storedXORTailCount, centerBits);
                } else {
                    out.writeLong(xor >>> storedXORTailCount, len);
                }
                preValueTailCount = Long.numberOfTrailingZeros(value);
                size += len;
                thisSize += len;
            } else {
                storedXORLeadCount = leadingZeros;
                storedXORTailCount = trailingZeros;
                int curValueTailCount = Long.numberOfTrailingZeros(value);
                if(storedXORTailCount > Math.max(curValueTailCount, preValueTailCount) || preValueTailCount <= curValueTailCount) {
                    // case 11
                    System.out.println("11: don't need.\n");
                    // when curTailCount == preValueTailCount, it may generate a gap between the value level and the xor level.
                    int centerBits = 64 - storedXORLeadCount - preValueTailCount;
                    out.writeInt((((0x3 << 3) | leadingRepresentation[storedXORLeadCount])), 5);
                    out.writeLong(xor >>> preValueTailCount, centerBits);
                    size += 5 + centerBits;
                    thisSize += 5 + centerBits;
                } else {
                    // case 10, need to store a delta
                    int centerBits = 64 - storedXORLeadCount - storedXORTailCount;
                    System.out.print("10: Here need to store a delta: ");
                    System.out.println(preValueTailCount - storedXORTailCount);
                    System.out.println();
                    out.writeInt((((0x2 << 3) | leadingRepresentation[storedXORLeadCount]) << 4) | ((preValueTailCount - storedXORTailCount) & 0xf), 9);
                    out.writeLong(xor >>> storedXORTailCount, centerBits);
                    size += 9 + centerBits;
                    thisSize += 9 + centerBits;
                }
                preValueTailCount = curValueTailCount;
            }
            storedVal = value;
        }

        return thisSize;
    }

    public int getSize() {
        return size;
    }

    public byte[] getOut() {
        return out.getBuffer();
    }
}

