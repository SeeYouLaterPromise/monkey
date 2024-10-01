package org.urbcomp.startdb.compress.elf.decompressor;

import gr.aueb.delorean.chimp.InputBitStream;
import org.urbcomp.startdb.compress.elf.utils.mon64;

import java.io.IOException;

public class YeXinXORDecompressor {
    private int count = 0;
    private long storedVal = 0;
    private int Beta_Last = 0;
    private int StoredXORLeadCount = Integer.MAX_VALUE;
    private boolean first = true;
    private boolean second = true;
    private boolean endOfStream = false;
    private final InputBitStream in;
    private final static long END_SIGN = Double.doubleToLongBits(Double.NaN);
    private final static short[] leadingRepresentation = {0, 8, 12, 16, 18, 20, 22, 24};

    public YeXinXORDecompressor(byte[] bs) {
        in = new InputBitStream(bs);
    }

    public InputBitStream getInputStream() {
        return in;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Double readValue(int beta) {
        try {
            System.out.print("Decompress the ");
            System.out.print(++count);
            System.out.println("-th value:");

            next(beta);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        if (endOfStream) {
            return null;
        }
        return Double.longBitsToDouble(storedVal);
    }

    private void next(int beta) throws IOException {
        if (first) {
            System.out.println("First Value Decompress.\n");

            first = false;
            int PreValueTailCount = in.readInt(7);
            if (PreValueTailCount == 64) storedVal = 0;
            else storedVal = ((in.readLong(63 - PreValueTailCount) << 1) + 1) << PreValueTailCount;
            Beta_Last = beta;
//            END
            if (storedVal == END_SIGN) {
                endOfStream = true;
            }
        } else {
            nextValue(beta);
        }
    }

    private void nextValue(int beta) throws IOException {
        // test module
        System.out.println("Decompress Start: PreValue(V_t-1) is: ");
        String I3E = Long.toBinaryString(storedVal);
        System.out.println(I3E.length() == 63 ? '0' + I3E : I3E);
        System.out.print("Decompress: PreValueTailCount: ");
        System.out.println(Long.numberOfTrailingZeros(storedVal));

        if(in.readBit() == 1) {
            StoredXORLeadCount = leadingRepresentation[in.readInt(3)];
        }else {
            if(in.readBit() == 0) {
//                identical case
                return;
            }
        }

        double v_stored = Double.longBitsToDouble(storedVal);

        // calculate the gAlpha_last
        int sp_last = mon64.getSP(Math.abs(v_stored));
        long ELong_Last = storedVal >> 52;

        System.out.println(Long.toBinaryString(storedVal));
        System.out.println(Long.toBinaryString(ELong_Last));

        int E_last = (((int)ELong_Last) & 0x7ff) - 1023;
        int alpha_last = Beta_Last - sp_last - 1;
        int gAlpha_Last = mon64.getFAlpha(alpha_last) + E_last;

//        after the above code, we can know the leading count
        // attention that maybe
        int need_read = 12 - StoredXORLeadCount;

        long ELong = ELong_Last;
//        based on the last decompressed value V_(t-1)1, through set_operator, transforming into v_(t-1)2
        if(second) {
            second = false;
        }else if(need_read > 0) {
//            now we need to get the v_(t-1)2 based on the v_(t-1)1 (key -> set_operator)
            // re-construct the set_operator from gAlpha_approximate

            // 1. get the E
            long lead_xor = in.readLong(need_read);
            // padding zeros
            lead_xor = lead_xor >> StoredXORLeadCount; // len == 12
            long last_lead = storedVal >> 52;
            ELong = lead_xor ^ last_lead;
        }

        int E = (((int)ELong) & 0x7ff) - 1023;

        // 2. infer gAlpha_approximate from E
        int sign = 1;
        if(E < 0) {
            E = -E;
            sign = -1;
        }
        int theta = mon64.getTheta(E);
        int Alpha_approximate = beta - (sign * theta);
        int gAlpha_approximate = mon64.getFAlpha(Alpha_approximate) + E;

        // compare with the last gAlpha to know how to construct the set_constructor
            // reconstruct set_operator
        if(gAlpha_Last > gAlpha_approximate) {
            gAlpha_approximate = gAlpha_Last;
        }
        long set_operator = (long)1 << (52 - gAlpha_approximate);
        // 3. get V_(t-1)2 from the V_(t-1)1 and the set_operator
        storedVal = storedVal | set_operator;

        v_stored = Double.longBitsToDouble(storedVal);

//       The trailing count of the storedValue could infer the trailing count of the XOR
        int PreValueTailCount = Long.numberOfTrailingZeros(storedVal);

//        remember we read something named 'need_read' before?
        int CenterBitsCount = 64 - StoredXORLeadCount - PreValueTailCount;
        long xor;
        if(!second && need_read > 0) {
//            CenterBitsCount = 64 - StoredXORLeadCount - need_read - PreValueTailCount;
            CenterBitsCount -= need_read;
            ELong = ELong << 52;
            xor = in.readLong(CenterBitsCount) << PreValueTailCount;
            xor += ELong;
        }else xor = in.readLong(CenterBitsCount) << PreValueTailCount; // len == 52

        System.out.println("The xor_decompress is: ");
        I3E = Long.toBinaryString(xor);
        System.out.println(I3E.length() == 63 ? '0' + I3E : I3E);

        storedVal ^= xor; // this is v_t1

        v_stored = Double.longBitsToDouble(storedVal);

        System.out.println("The value_after_decompress is: ");
        I3E = Long.toBinaryString(storedVal);
        System.out.println(I3E.length() == 63 ? '0' + I3E : I3E);
        System.out.println();

//        update for next compress
        Beta_Last = beta;

//        END_SIGN
        if (storedVal == END_SIGN) {
            endOfStream = true;
        }
    }
}


