package org.urbcomp.startdb.compress.elf.compressor;

import gr.aueb.delorean.chimp.OutputBitStream;
import org.checkerframework.checker.units.qual.C;
import org.checkerframework.checker.units.qual.Current;
import org.urbcomp.startdb.compress.elf.utils.mon64;

public class CompressorWithInitialClassification {
//    Pre-manipulation
    private int LastTrailing = Integer.MAX_VALUE;
    private int CurTrailing = Integer.MAX_VALUE;
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

    public CompressorWithInitialClassification() {
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
            CurTrailing = v == 0.0 ? 64 : 51;
            size += out.writeInt(2, 2); // case 10
            return vLong;
        }

//        get the value of alpha and lastBetaStar
//        int[] alphaAndBetaStar = mon64.ourGetAlphaAndBetaStar(v, lastBetaStar);

        int[] alphaAndBetaStar = mon64.getAlphaAndBetaStar(v, lastBetaStar);
        Alpha = alphaAndBetaStar[0];
        E = (int)((vLong >> 52) & 0x7ff) - 1023;
        GAlpha = mon64.getFAlpha(alphaAndBetaStar[0]) + E;
        int eraseBits = 52 - GAlpha;
        CurTrailing = Long.numberOfTrailingZeros(vLong);
        if (eraseBits <= 4) {

//            System.out.println("Value: " + v + ", Alpha: " + Alpha + ", Beta: " + alphaAndBetaStar[1]);

            lastBetaStar = Integer.MAX_VALUE;
            size += out.writeInt(2, 2); // case 10
            return vLong;
        }else {
            long mask = 0xffffffffffffffffL << eraseBits;
            DeltaEQ0 = ((~mask) & vLong) == 0;
//            DeltaEQ0 = CurTrailing == (52 - Alpha);
//            Precise type without the need of lastBetaStar
            if (DeltaEQ0 && CurTrailing >= LastTrailing) {
                size += out.writeInt(2, 2); // case 10
                return vLong;
            }
//            Approximate type and Precise type with the need of theta.
            if (alphaAndBetaStar[1] == lastBetaStar) {
                size += out.writeBit(false);   // case 0
            } else {
                lastBetaStar = alphaAndBetaStar[1];
                size += out.writeInt(alphaAndBetaStar[1] | 0x30, 6);  // case 11, 2 + 4 = 6
            }
            long vPrimeLong = vLong & mask;
            CurTrailing = Long.numberOfTrailingZeros(vPrimeLong);
            return vPrimeLong;
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

//        1. first step -> write 12 bits
        size += out.writeLong(value >>> 52, 12);
        if (E == 1024 || E == -1023) {
            return size;
        }

        switch (lastBetaStar) {
            case Integer.MAX_VALUE:
                size += out.writeLong(value, 52);
                return size;
            case 0:
                return size;
        }

//        size += out.writeBit(DeltaEQ0);

        if (E < 0){
            storedE = E;
            Theta = mon64.getTheta(E);
            Alpha = lastBetaStar - Theta;
            if(DeltaEQ0) Alpha = lastBetaStar - Theta;
            GAlpha = mon64.getFAlpha(Alpha) + E;
        }

//        int RightBoundary = DeltaEQ0 ? Alpha + E : GAlpha;
        int RightBoundary = GAlpha;
        size += out.writeLong(value >>> (52 - RightBoundary), RightBoundary);
        LastTrailing = CurTrailing;
        return size;
    }

    private int compressValue(long value) {
        if ((storedVal ^ value) == 0) {
            // write 00
            size += out.writeInt(0, 2);
            LastTrailing = CurTrailing;
            return size;
        }

        long xor = storedVal ^ value;
//        1. Determine the left boundary
        int CurXORLeadRoundCount = leadingRound[Long.numberOfLeadingZeros(xor)];
        if (CurXORLeadRoundCount == StoredXORLeadCount) {
//            size += out.writeInt(1, 2);
            size += out.writeBit(true);       // case 1
        } else {
//            size += out.writeBit(true);
            size += out.writeInt(1, 2); // case 01;
            StoredXORLeadCount = CurXORLeadRoundCount;
            size += out.writeInt(leadingRepresentation[StoredXORLeadCount], 3);
        }

//        Then, process with 0 and END_SIGN through E value
        if (StoredXORLeadCount < 12) {
            size += out.writeLong(xor >>> 52, 12 - StoredXORLeadCount);
        }

        int read = Math.max(StoredXORLeadCount, 12);

        if (E == 1024 || E == -1023 || lastBetaStar == 0) {
            // END_SIGN || value == 0
            storedVal = value;
            LastTrailing = CurTrailing;
            return size;
        }

//        Best Case: PreValue could infer
        int PreValueTrailCount = Long.numberOfTrailingZeros(storedVal);
        if (PreValueTrailCount <= Long.numberOfTrailingZeros(value)) {
            size += out.writeBit(true);
            if (64 - PreValueTrailCount <= read) {
                storedVal = value;
                LastTrailing = CurTrailing;
                return size;
            }
            size += out.writeLong(xor >>> PreValueTrailCount, 64 - read - PreValueTrailCount);
            storedVal = value;
            LastTrailing = CurTrailing;
            return size;
        }
        size += out.writeBit(false);

//        Similar to first encoding, high-precision should be processed directly.
        if (lastBetaStar == Integer.MAX_VALUE) {
            size += out.writeLong(xor, 64 - read);
            storedVal = value;
            LastTrailing = CurTrailing;
            return size;
        }

//        General Case: index calculation
//        size += out.writeBit(DeltaEQ0);

//        v < 1 remain non-revision
        if (E < 0) {
            if (E != storedE) {
                storedE = E;
                Theta = mon64.getTheta(E);
            }
            Alpha = lastBetaStar - Theta;
            if(DeltaEQ0) Alpha = lastBetaStar - Theta;
            GAlpha = mon64.getFAlpha(lastBetaStar - Theta) + E;
        }

        int RightBoundary = mon64.getFAlpha(Alpha) + E;
//        int RightBoundary = DeltaEQ0 ? Alpha + E : GAlpha;
        size += out.writeLong(xor >>> (52 - RightBoundary), RightBoundary + 12 - read);

//        update for next compress
        storedVal = value;
        LastTrailing = CurTrailing;
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


