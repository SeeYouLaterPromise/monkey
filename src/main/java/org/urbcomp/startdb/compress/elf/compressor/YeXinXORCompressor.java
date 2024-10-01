package org.urbcomp.startdb.compress.elf.compressor;

import gr.aueb.delorean.chimp.OutputBitStream;

public class YeXinXORCompressor {
    private int count = 0;
    private int StoredXORLeadCount = Integer.MAX_VALUE;
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

    public YeXinXORCompressor() {
        out = new OutputBitStream(
                new byte[10000]);  // for elf, we need one more bit for each at the worst case
        size = 0;
    }

    public OutputBitStream getOutputStream() {
        return this.out;
    }

    /**
     * Two forms after data manipulation
     * @param vPrimeLong1: after erasing operation
     * @param vPrimeLong2: after setting operation
     * @return size
     */
    public int compress(long vPrimeLong1, long vPrimeLong2) {
        if (first) {
            return addValue(vPrimeLong2);
        }else {
            int size = addValue(vPrimeLong1);
//            store the second kind to compress the next one
            storedVal = vPrimeLong2;
            return size;
        }
    }

    /**
     * Adds a new long value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    private int addValue(long value) {
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

    private int writeFirst(long value) {
        System.out.println();
        first = false;
        storedVal = value;
        if(value == 0) {
            out.writeInt(64, 7);
            size += 7;
            return 7;
        }
        int TrailingCount = Long.numberOfTrailingZeros(value);
        out.writeInt(TrailingCount, 7);
        out.writeLong(storedVal >>> (TrailingCount + 1), (63 - TrailingCount));
        size += 69 - TrailingCount;
        return 69 - TrailingCount;
    }

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() {
        addValue(END_SIGN);
        out.writeBit(false);
        out.flush();
    }

    /**
     * In fact, the decompressed value is the xor
     * @param value
     * @return
     */
    private int compressValue(long value) {
        long xor = storedVal ^ value;
        if(xor == 0) {
            System.out.println("Identical Case.\n");
            // case 10 -> Identical case
            out.writeInt(0, 2);
            size += 2;
            return 2;
        }
        int t_size = 0;
//        test module
        System.out.println("Compress Start: storedValue(V_t-1) is:");
        String I3E1 = Long.toBinaryString(storedVal);
        double V_t_1_2 = Double.longBitsToDouble(storedVal);
        System.out.println(I3E1.length() == 63 ? '0' + I3E1 : I3E1);

        System.out.println("Compress: value(V_t) is: ");
        String I3E2 = Long.toBinaryString(value);
        double V_t_1 = Double.longBitsToDouble(value);
        System.out.println(I3E2.length() == 63 ? '0' + I3E2 : I3E2);

        System.out.println("Compress: The xor result is: ");
        String I3E3 = Long.toBinaryString(xor);
        System.out.println(I3E3.length() == 63 ? '0' + I3E3 : I3E3);

        int CurXORLeadCount = Long.numberOfLeadingZeros(xor);
//        write leadCount or not?
        if(CurXORLeadCount == StoredXORLeadCount) out.writeInt(1, 2);
        else {
            System.out.println("Write the LeadCount.\n");

            out.writeBit(true);
            StoredXORLeadCount = leadingRound[Long.numberOfLeadingZeros(xor)];
            out.writeInt(leadingRepresentation[StoredXORLeadCount], 3);
            t_size += 2;
        }
        int TrailingCount = Long.numberOfTrailingZeros(storedVal);

        int CenterBitsCount = 64 - StoredXORLeadCount - TrailingCount;
        out.writeLong(xor >>> TrailingCount, CenterBitsCount);

        System.out.print("CenterBitsCount: ");
        System.out.println(CenterBitsCount);
        System.out.println();

//        update for next compress
//        storedVal = value;
        size += 2 + t_size + CenterBitsCount;
        return 2 + t_size + CenterBitsCount;
    }

    public int getSize() {
        return size;
    }

    public byte[] getOut() {
        return out.getBuffer();
    }
}


