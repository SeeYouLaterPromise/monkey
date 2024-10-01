package org.urbcomp.startdb.compress.elf.decompressor;

import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class myXORDecompressor {
    private int count = 0;
    private int PreValueTailCount = 0;
    private long storedVal = 0;
    private int StoredXORLeadCount = Integer.MAX_VALUE;
    private boolean first = true;
    private boolean endOfStream = false;
    private final InputBitStream in;
    private final static long END_SIGN = Double.doubleToLongBits(Double.NaN);
    private final static short[] leadingRepresentation = {0, 8, 12, 16, 18, 20, 22, 24};

    public myXORDecompressor(byte[] bs) {
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
            System.out.println("First Value Decompress.\n");

            first = false;
            PreValueTailCount = in.readInt(7);
            if (PreValueTailCount == 64) storedVal = 0;
            else storedVal = ((in.readLong(63 - PreValueTailCount) << 1) + 1) << PreValueTailCount;
//            END
            if (storedVal == END_SIGN) {
                endOfStream = true;
            }
        } else {
            nextValue();
        }
    }

    private void nextValue() throws IOException {
        // test module
        System.out.println("Decompress Start: PreValue(V_t-1) is: ");
        String I3E = Long.toBinaryString(storedVal);
        System.out.println(I3E.length() == 63 ? '0' + I3E : I3E);
        System.out.print("Decompress: PreValueTailCount: ");
        System.out.println(PreValueTailCount);

//        write LeadCount or not?
        if(in.readBit() == 1) {
            StoredXORLeadCount = leadingRepresentation[in.readInt(3)];
        }

        int CurXORTailCount = PreValueTailCount;
//        write delta for modifying or not?
        if(in.readBit() == 1) {
            int delta;
//            optimize by the fee of modifying
            if(in.readBit() == 0) {
                delta = in.readInt(3);
//                Identical case
                if(delta == 0) return;
            }else {
                delta = in.readInt(6);
                if(delta == 0) delta = 64;
            }

            CurXORTailCount = PreValueTailCount - delta;
        }


        int CenterBitsCount = 64 - StoredXORLeadCount - CurXORTailCount;
        long xor = in.readLong(CenterBitsCount) << CurXORTailCount;


        System.out.println("The xor_decompress is: ");
        I3E = Long.toBinaryString(xor);
        System.out.println(I3E.length() == 63 ? '0' + I3E : I3E);

        storedVal ^= xor;

        System.out.println("The value_after_decompress is: ");
        I3E = Long.toBinaryString(storedVal);
        System.out.println(I3E.length() == 63 ? '0' + I3E : I3E);
        System.out.println();

//        update for next compress
        PreValueTailCount = Long.numberOfTrailingZeros(storedVal);
        if (storedVal == END_SIGN) {
            endOfStream = true;
        }
    }
}


