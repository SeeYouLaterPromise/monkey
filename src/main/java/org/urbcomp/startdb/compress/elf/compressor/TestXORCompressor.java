package org.urbcomp.startdb.compress.elf.compressor;

import gr.aueb.delorean.chimp.OutputBitStream;
import org.urbcomp.startdb.compress.elf.utils.mon64;

public class TestXORCompressor {
    private int count = 0;
    private int StoredXORLeadCount = 0;
    private int Beta = -1;
    private int Alpha = -1;
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

    public TestXORCompressor() {
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
    public int addValue(long value, int beta, int alpha, boolean deltaEQ0) {
        System.out.print("Compress the ");
        System.out.print(++count);
        System.out.println("-th value:");

        Beta = beta;
        Alpha = alpha;
        DeltaEQ0 = deltaEQ0;
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
        System.out.println("First value Compressing!");
        System.out.println();

        first = false;
        storedVal = value;

        int E = mon64.getEFromValue(value);
        if(E == 1024 || (Beta == -1 && E == -1023)) {
            out.writeLong(value >>> 52, 12);
            size += 12;
            return 12;
        }else if (Beta == -1) {
            // high precision case
            out.writeLong(value, 64);
            size += 64;
            return 64;
        }

        // 1. first step -> write lead(12 bits)
        out.writeLong(value >>> 52, 12);

        // whether beta equals to zero
        out.writeBit(DeltaEQ0);

        int read = 12;

        if (mon64.furtherCheck(E)) {
            out.writeLong(value >>> (52 - E), E);
            read += E;
        } else {
            Alpha = Beta - mon64.getTheta(E);
        }

        int ValueTrailCount;

        if (DeltaEQ0) {
            ValueTrailCount = 52 - (Alpha + E);
        }else {
            ValueTrailCount = 52 - (mon64.getFAlpha(Alpha) + E);
        }

        out.writeLong(value >>> ValueTrailCount, 64 - ValueTrailCount - read);
        size += 64 - ValueTrailCount;
        return 64 - ValueTrailCount;

    }

    private int compressValue(long value) {
        long xor = storedVal ^ value;
        if(xor == 0) {
//            write 00
            System.out.println("Identical case!");

            size += out.writeInt(0, 2);
            return 2;
        }

        int t_size = 0;

//        test module
        System.out.println("Compress Start: storedValue(V_t-1) is:");
        String I3E1 = mon64.showBinaryString(storedVal);
        System.out.println("Compress: value(V_t) is: ");
        String I3E2 = mon64.showBinaryString(value);
        System.out.println("Compress: The xor result is: ");
        String I3E3 = mon64.showBinaryString(xor);


        int CurXORLeadCount = Long.numberOfLeadingZeros(xor);
        int CurXORLeadRoundCount = leadingRound[CurXORLeadCount];

//        write leadCount or not?
        if(CurXORLeadRoundCount == StoredXORLeadCount) {
            out.writeInt(1, 2);
            t_size += 2;
        }
        else {
            out.writeBit(true);
            StoredXORLeadCount = CurXORLeadRoundCount;
            out.writeInt(leadingRepresentation[StoredXORLeadCount], 3);

            System.out.print("Write the Lead Count:");
            System.out.println(StoredXORLeadCount);

            t_size += 4;
        }
//        from then on, you can see the StoreXORLeadCount as the bits already been read.

//        Best Case: PreValue could infer
        int PreValueTailCount = Long.numberOfTrailingZeros(storedVal);
        int CurValueTailCount = Long.numberOfTrailingZeros(value);
        int delta = PreValueTailCount - CurValueTailCount;
        int CenterBitsCount;


        if (delta <= 0) {
            System.out.println("PreValue Could Infer!");
            System.out.println();

            out.writeBit(true);

            CenterBitsCount = 64 - StoredXORLeadCount - PreValueTailCount;
            out.writeLong(xor >>> PreValueTailCount, CenterBitsCount);

            storedVal = value;
            size += t_size + CenterBitsCount;
            return t_size + CenterBitsCount;
        }

//
        out.writeBit(false);


//        whether beta equals to zero
        out.writeBit(DeltaEQ0);


//        write more for E_XOR
        if (StoredXORLeadCount < 12) {
            out.writeLong(xor >>> 52, 12 - StoredXORLeadCount);
            t_size += 12 - StoredXORLeadCount;
        }
        int read = Math.max(StoredXORLeadCount, 12);


        int E = mon64.getEFromValue(value);


        if (E == 1024 || (E == -1023 && Beta == -1)) {
            // END_SIGN || value == 0
            storedVal = value;
            size += t_size;
            return t_size;
        }else if (Beta == -1) {
            // high precision case
            out.writeLong(xor, 64 - read);

            storedVal = value;
            size += t_size + 64 - read;
            return t_size + 64 - read;
        }

//        excluding the three special cases



        System.out.println("We should approximate the Trailing Count!");



        if (E < 0) {
//            fractional number cannot get the concise alpha till now!
            Alpha = Beta - mon64.getTheta(E);
        }


        if(mon64.furtherCheck(E) && 12 + E - read > 0) {
//            write part for extract the Integer part
            System.out.println("Read E Part:");
            mon64.showBinaryString(xor >>> (52 - E));
            out.writeLong(xor >>> (52 - E), 12 + E - read);
            read = 12 + E;
        }

//        delta == 0 means this number could be represented concisely in the computer
        if (DeltaEQ0) {
            PreValueTailCount = 52 - (Alpha + E);
        }else {
            PreValueTailCount = 52 - (mon64.getFAlpha(Alpha) + E);
        }

        CenterBitsCount = 64 - read - PreValueTailCount;
        out.writeLong(xor >>> PreValueTailCount, CenterBitsCount);


//        update for next compress
        System.out.println();

        storedVal = value;
        size += t_size + CenterBitsCount;
        return t_size + CenterBitsCount;
    }

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() {
        System.out.println("Compress the END_SIGN!!!!");
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


