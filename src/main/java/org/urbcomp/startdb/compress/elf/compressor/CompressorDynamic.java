package org.urbcomp.startdb.compress.elf.compressor;

import gr.aueb.delorean.chimp.OutputBitStream;
import org.urbcomp.startdb.compress.elf.utils.mon64;

public class CompressorDynamic {
    private int StoredXORLeadCount = 0;
    private int Beta = -1;
    private int Alpha = -1;
    private int GAlpha = -1;
    private boolean DeltaEQ0 = false;
    private long storedVal = 0;
    private boolean first = true;
    private int size;
    private final static double END_SIGN = Double.NaN;

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

    public CompressorDynamic() {
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
    public int addValue(long value, int beta, int alpha, boolean deltaEQ0, int gAlpha) {
        Beta = beta;
        Alpha = alpha;
        DeltaEQ0 = deltaEQ0;
        GAlpha = gAlpha;
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
        if (first) {
            return writeFirst(Double.doubleToRawLongBits(value));
        } else {
            return compressValue(Double.doubleToRawLongBits(value));
        }
    }

    private int writeFirst(long value) {
        first = false;
        storedVal = value;
        int E = mon64.getEFromValue(value);

        if (E == 1024 || E == -1023) {
            out.writeLong(value >>> 52, 12);
            size += 12;
            return 12;
        } else if (Beta == -1) {
            // high precision case
            out.writeLong(value, 64);
            size += 64;
            return 64;
        }

        // 1. first step -> write lead(12 bits)
        out.writeLong(value >>> 52, 12);

        // whether beta equals to zero
        out.writeBit(DeltaEQ0);

        int RightBoundary = DeltaEQ0 ? Alpha + E : GAlpha;
        for(int i = 1; i <= RightBoundary; i++) {
            out.writeLong(value >>> (52 - i), 1);
        }

        size += 13 + RightBoundary;
        return 13 + RightBoundary;
    }

    private int compressValue(long value) {
        long xor = storedVal ^ value;
        if (xor == 0) {
//            write 00
            size += out.writeInt(0, 2);
            return 2;
        }

        int t_size = 0;
        int CurXORLeadRoundCount = leadingRound[Long.numberOfLeadingZeros(xor)];

//        write leadCount or not?
        if (CurXORLeadRoundCount == StoredXORLeadCount) {
            out.writeInt(1, 2);
            t_size += 2;
        } else {
            out.writeBit(true);
            StoredXORLeadCount = CurXORLeadRoundCount;
            out.writeInt(leadingRepresentation[StoredXORLeadCount], 3);
            t_size += 4;
        }
//        from then on, you can see the StoreXORLeadCount as the bits already been read.

//        Best Case: PreValue could infer
        int PreValueTailCount = Long.numberOfTrailingZeros(storedVal);

        if (PreValueTailCount <= Long.numberOfTrailingZeros(value)) {
            out.writeBit(true);
            int CenterBitsCount = 64 - StoredXORLeadCount - PreValueTailCount;
            out.writeLong(xor >>> PreValueTailCount, CenterBitsCount);
            storedVal = value;
            size += t_size + 1 + CenterBitsCount;
            return t_size + 1 + CenterBitsCount;
        }

//
        out.writeBit(false);

        t_size++;

//        write more for E_XOR
        if (StoredXORLeadCount < 12) {
            out.writeLong(xor >>> 52, 12 - StoredXORLeadCount);
            t_size += 12 - StoredXORLeadCount;
        }
        int read = Math.max(StoredXORLeadCount, 12);


        int E = mon64.getEFromValue(value);

        if (E == 1024 || E == -1023) {
            // END_SIGN || value == 0
            storedVal = value;
            size += t_size;
            return t_size;
        } else if (Beta == -1) {
            // high precision case
            out.writeLong(xor, 64 - read);
            storedVal = value;
            size += t_size + 64 - read;
            return t_size + 64 - read;
        }
//        excluding the three special cases

//        whether beta equals to zero
        out.writeBit(DeltaEQ0);
        t_size++;
        int RightBoundary = DeltaEQ0 ? Alpha + E : GAlpha;
        for(int i = read - 11; i <= RightBoundary; i++) {
            out.writeLong(xor >>> (52 - i), 1);
        }

//        update for next compress
        storedVal = value;
        size += t_size + RightBoundary + 12 - read;
        return t_size + RightBoundary + 12 - read;
    }

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() {
        addValue(END_SIGN);
        out.writeBit(false);
        out.flush();
    }

    public int getSize() {
        return size;
    }

    public byte[] getOut() {
        return out.getBuffer();
    }
}


