package org.urbcomp.startdb.compress.elf.compressor;

import gr.aueb.delorean.chimp.OutputBitStream;
import org.checkerframework.checker.units.qual.C;
import org.checkerframework.checker.units.qual.Current;
import org.urbcomp.startdb.compress.elf.utils.mon64;

public class CompressorLeadingFlag {
    //    Pre-manipulation
    private int storedTrailingZeros = Integer.MAX_VALUE;
    private int lastBetaStar = Integer.MAX_VALUE;
    //    XORCompress
    private int StoredXORLeadCount = 0;
    private int Alpha = -1;
    private int GAlpha = -1;
    private int E = Integer.MIN_VALUE;
    private int storedE = Integer.MIN_VALUE;
    private int Theta = Integer.MIN_VALUE;
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
    private final OutputBitStream out;

    public CompressorLeadingFlag() {
        out = new OutputBitStream(
                new byte[10000]);  // for elf, we need one more bit for each at the worst case
    }

    public OutputBitStream getOutputStream() {
        return this.out;
    }

    private long erasing(double v) {
        long vLong = Double.doubleToRawLongBits(v);
        if (v == 0.0 || Double.isNaN(v)) {
            // subnormal 0 || NaN
            lastBetaStar = -1;
            E = v == 0.0 ? 1024 : -1023;
            size += out.writeInt(2, 2); // case 10
            return vLong;
        }

//        get the value of alpha and lastBetaStar
        int[] alphaAndBetaStar = mon64.getAlphaAndBetaStar(v, lastBetaStar);
        Alpha = alphaAndBetaStar[0];
        E = (int)((vLong >> 52) & 0x7ff) - 1023;
        GAlpha = mon64.getFAlpha(alphaAndBetaStar[0]) + E;
        int eraseBits = 52 - GAlpha;
        if (eraseBits <= 4) {
            lastBetaStar = Integer.MAX_VALUE;
            size += out.writeInt(2, 2); // case 10
            return vLong;
        }else {
            long mask = 0xffffffffffffffffL << eraseBits;
            DeltaEQ0 = ((~mask) & vLong) == 0;
//            Precise type without the need of lastBetaStar
//            if (DeltaEQ0 && CurTrailing > LastTrailing) {
//                size += out.writeInt(2, 2); // case 10
//                return vLong;
//            }
//            Approximate type and Precise type with the need of theta.
            if (alphaAndBetaStar[1] == lastBetaStar) {
                size += out.writeBit(false);   // case 0
            } else {
                lastBetaStar = alphaAndBetaStar[1];
                size += out.writeInt(alphaAndBetaStar[1] | 0x30, 6);  // case 11, 2 + 4 = 6
            }
            return vLong & mask;
        }
    }

    /**
     * Adds a new double value to the series. Note, values must be inserted in order.
     *
     * @param v next floating point value in the series
     */
    public int addValue(double v) {
        size = 0;
        long v_ = erasing(v);
        if (first) {
            first = false;
            return writeFirst(v_);
        } else {
            return compressValue(v_);
        }
    }

    private int writeFirst(long value) {
        storedVal = value;
        storedTrailingZeros = Long.numberOfTrailingZeros(value);
//        1. first step -> write 12 bits
        size += out.writeLong(value >>> 52, 12);
        if (E == 1024 || E == -1023) {
            return size;
        }

        switch (lastBetaStar) {
            case 0:
                // 10^theta case
                return size;
            case Integer.MAX_VALUE:
                // process high precision case subsequently
                size += out.writeLong(value, 52);
                return size;
        }

//        whether lastBetaStar equals to zero
        size += out.writeBit(DeltaEQ0);

        if (E < 0){
            storedE = E;
            Theta = mon64.getTheta(E);
            Alpha = lastBetaStar - Theta;
            if(DeltaEQ0) Alpha = lastBetaStar - Theta;
            else GAlpha = mon64.getFAlpha(lastBetaStar - Theta) + E;
        }

        int RightBoundary = DeltaEQ0 ? Alpha + E : GAlpha;
        size += out.writeLong(value >>> (52 - RightBoundary), RightBoundary);
        return size;
    }

    private int compressValue(long value) {
        if ((storedVal ^ value) == 0) {
            // case 01
            size += out.writeInt(1, 2);
            return size;
        }

        long xor = storedVal ^ value;
        int CurXORLeadRoundCount = leadingRound[Long.numberOfLeadingZeros(xor)];
        int CurValueTrailingZeros = Long.numberOfTrailingZeros(value);

        if (CurXORLeadRoundCount == StoredXORLeadCount && CurValueTrailingZeros >= storedTrailingZeros) {
            // case 00
            int centerBits = 64 - StoredXORLeadCount - storedTrailingZeros;
            int len = 2 + centerBits;
            if(len > 64) {
                out.writeInt(0, 2);
                out.writeLong(xor >>> storedTrailingZeros, centerBits);
            } else {
                out.writeLong(xor >>> storedTrailingZeros, len);
            }
            size += len;
            return size;
        }
//        case 1
        size += out.writeBit(true);
        size += out.writeInt(leadingRepresentation[CurXORLeadRoundCount], 3);
        StoredXORLeadCount = CurXORLeadRoundCount;
        storedTrailingZeros = CurValueTrailingZeros;

//        Then, process with 0 and END_SIGN through E value
        if (StoredXORLeadCount < 12) {
            size += out.writeLong(xor >>> 52, 12 - StoredXORLeadCount);
        }
        int read = Math.max(StoredXORLeadCount, 12);

        if (E == 1024 || E == -1023 || lastBetaStar == 0) {
            // END_SIGN || value == 0
            storedVal = value;
            return size;
        }

//        Similar to first encoding, high-precision should be processed directly.
        if (lastBetaStar == Integer.MAX_VALUE) {
            size += out.writeLong(xor, 64 - read);
            storedVal = value;
            return size;
        }

//        General Case: index calculation
        size += out.writeBit(DeltaEQ0);

//        v < 1 remain non-revision
        if (E < 0) {
            if (E != storedE) {
                storedE = E;
                Theta = mon64.getTheta(E);
            }
            if(DeltaEQ0) Alpha = lastBetaStar - Theta;
            else GAlpha = mon64.getFAlpha(lastBetaStar - Theta) + E;
        }

        int RightBoundary = DeltaEQ0 ? Alpha + E : GAlpha;
        size += out.writeLong(xor >>> (52 - RightBoundary), RightBoundary + 12 - read);

//        update for next compress
        storedVal = value;
        return size;
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


