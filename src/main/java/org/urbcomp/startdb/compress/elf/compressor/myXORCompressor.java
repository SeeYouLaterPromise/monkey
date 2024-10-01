package org.urbcomp.startdb.compress.elf.compressor;

import gr.aueb.delorean.chimp.OutputBitStream;

public class myXORCompressor {
    private int count = 0;
    private int PreValueTailCount = Integer.MAX_VALUE;
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

    public myXORCompressor() {
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
        System.out.println();
        first = false;
        storedVal = value;
        if(value == 0) {
            out.writeInt(64, 7);
            size += 7;
            return 7;
        }
        PreValueTailCount = Long.numberOfTrailingZeros(value);
        out.writeInt(PreValueTailCount, 7);
        out.writeLong(storedVal >>> (PreValueTailCount + 1), (63 - PreValueTailCount));
        size += 69 - PreValueTailCount;
        return 69 - PreValueTailCount;
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
        long xor = storedVal ^ value;
        if(xor == 0) {
            System.out.println("Identical Case.\n");
//            write 001 -> add a delta == 0, cus in my design, delta are not bound to zero.
            out.writeInt(2, 3);
            out.writeInt(0, 3);
            size += 6;
            return 6;
        }
        int t_size = 0;
//        test module
        System.out.println("Compress Start: storedValue(V_t-1) is:");
        String I3E1 = Long.toBinaryString(storedVal);
        System.out.println(I3E1.length() == 63 ? '0' + I3E1 : I3E1);

        System.out.println("Compress: value(V_t) is: ");
        String I3E2 = Long.toBinaryString(value);
        System.out.println(I3E2.length() == 63 ? '0' + I3E2 : I3E2);

        System.out.println("Compress: The xor result is: ");
        String I3E3 = Long.toBinaryString(xor);
        System.out.println(I3E3.length() == 63 ? '0' + I3E3 : I3E3);

        int CurXORLeadCount = Long.numberOfLeadingZeros(xor);
//        write leadCount or not?
        if(CurXORLeadCount == StoredXORLeadCount) out.writeBit(false);
        else {
            System.out.println("Write the LeadCount.\n");

            out.writeBit(true);
            StoredXORLeadCount = leadingRound[Long.numberOfLeadingZeros(xor)];
            out.writeInt(leadingRepresentation[StoredXORLeadCount], 3);
            t_size += 3;
        }

        int CurValueTailCount = Long.numberOfTrailingZeros(value);
        int CurXORTailCount = Long.numberOfTrailingZeros(xor);
//        boolean SpecialCase = CurXORTailCount > Math.max(PreValueTailCount, CurValueTailCount);
        int delta = PreValueTailCount - CurValueTailCount;
//        write delta or not?
//          write center bits
        int CenterBitsCount;
        if(delta <= 0) {
            out.writeBit(false);
//            for special case(PreValueTailCount == CurValueTailCount), CurXORTailCount >= PreValueTailCount + 1
            CenterBitsCount = 64 - StoredXORLeadCount - PreValueTailCount;
            out.writeLong(xor >>> PreValueTailCount, CenterBitsCount);
        } else {
            System.out.print("Write the delta: ");
            System.out.println(delta);
            System.out.println();

            out.writeBit(true);

//            write delta is expensive or not?
            if(delta <= 7) {
                out.writeBit(false);
                out.writeInt(delta, 3);
                t_size = 4;
            }else {
                out.writeBit(true);
//                leverage each bit
                if(delta == 64) delta = 0;
                out.writeInt(delta, 6);
                t_size += 7;
            }
            CenterBitsCount = 64 - StoredXORLeadCount - CurXORTailCount;
            out.writeLong(xor >>> CurXORTailCount, CenterBitsCount);
        }

        System.out.print("CenterBitsCount: ");
        System.out.println(CenterBitsCount);
        System.out.println();

//        update for next compress
        storedVal = value;
        PreValueTailCount = Long.numberOfTrailingZeros(value);
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


