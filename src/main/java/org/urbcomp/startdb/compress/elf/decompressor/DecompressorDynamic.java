package org.urbcomp.startdb.compress.elf.decompressor;

import gr.aueb.delorean.chimp.InputBitStream;

import org.urbcomp.startdb.compress.elf.utils.mon64;

import java.io.IOException;


public class DecompressorDynamic {
    private long storedVal = 0;
    private int storedE = Integer.MIN_VALUE;
    private int storedTheta = Integer.MIN_VALUE;
    private int StoredXORLeadCount = 0;
    private boolean first = true;
    private boolean endOfStream = false;
    private final InputBitStream in;
    private final static long END_SIGN = Double.doubleToLongBits(Double.NaN);
    private final static short[] leadingRepresentation = {0, 8, 12, 16, 18, 20, 22, 24};

    public DecompressorDynamic(byte[] bs) {
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
    public Double readValue(int Beta, int[] alpha, boolean[] deltaEQ0) {
        try {
            next(Beta, alpha, deltaEQ0);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        if (endOfStream) {
            return null;
        }
        return Double.longBitsToDouble(storedVal);
    }

    private void next(int Beta, int[] alpha, boolean[] deltaEQ0) throws IOException {
        if (first) {
            firstValue(Beta, alpha, deltaEQ0);
        } else {
            nextValue(Beta, alpha, deltaEQ0);
        }

//            END
        if (storedVal == END_SIGN) {
            endOfStream = true;
        }
    }

    private void firstValue(int Beta, int[] alpha, boolean[] deltaEQ0) throws IOException {
        first = false;
        // 1. first step -> extract E

        long lead = in.readLong(12);
        int E = mon64.getEFromLead(lead);

        switch (E) {
            case -1023:
                storedVal = 0;
                return;
            case 1024:
                storedVal = 0x7ff8000000000000L;
                return;
        }

        if (Beta == -1) {
            storedVal = (lead << 52) | (in.readLong(52));
            return;
        }

        storedE = E;
        int Theta = mon64.getTheta(E);
        storedTheta = Theta;
        int Alpha = Beta - Theta;

        // whether delta equals to 0
        deltaEQ0[0] = in.readBit() == 1;

        int GAlpha = deltaEQ0[0] ? Alpha + E : mon64.getFAlpha(Alpha) + E;
        boolean needConfirm = true;
        double sum = E >= 0 ? 1L << E : 1.0 / (1L << -E);
        double target = mon64.Pow10(Theta);
        for (int count = 1; count <= GAlpha; count++) {
            long single = in.readBit();
            lead = (lead << 1) | single;
            if(needConfirm && single != 0)  {
                sum += E - count >= 0 ? 1L << (E - count) : 1.0 / (1L << (count - E));
            }
            if(needConfirm && sum >= target) {
                needConfirm = false;
                Alpha--;
                GAlpha = deltaEQ0[0] ? Alpha + E : mon64.getFAlpha(Alpha) + E;
            }
        }
        alpha[0] = Alpha;
        storedVal = lead << (52 - GAlpha);
    }

    private void nextValue(int Beta, int[] alpha, boolean[] deltaEQ0ANDPreInfer) throws IOException {
//        write LeadCount or not?
        if(in.readBit() == 1) {
            StoredXORLeadCount = leadingRepresentation[in.readInt(3)];
        }else if(in.readBit() == 0) {
            // identical case!
            deltaEQ0ANDPreInfer[1] = true;
            return;
        }

        long xor;

//        PreValue Could infer
        if (in.readBit() == 1) {
            deltaEQ0ANDPreInfer[1] = true;
            int PreValueTrailCount = Long.numberOfTrailingZeros(storedVal);
            xor = in.readLong(64 - StoredXORLeadCount - PreValueTrailCount) << PreValueTrailCount;
            storedVal ^= xor;
            return;
        }

//        From then on, we could know the LeadZeroCount, and then infer the next value's E
        if(StoredXORLeadCount < 12) {
            // Should read more to get the E
            storedVal ^= (in.readLong(12 - StoredXORLeadCount) << 52);
        }

        int E = mon64.getEFromValue(storedVal);
        int read = Math.max(12, StoredXORLeadCount);

        switch (E) {
            case -1023:
                storedVal = 0;
                return;
            case 1024:
                storedVal = 0x7ff8000000000000L;
                return;
        }

//        high-precision case read 64 bits for decoding directly.
        if (Beta == -1) {
            storedVal ^= in.readLong(64 - read);
            return;
        }
//        PreValue couldn't infer

        // get alpha from E and Beta
//        int Theta = E != storedE ? Elf64Utils.getTheta(E) : storedTheta;
        if(E != storedE) {
            storedE = E;
            storedTheta = mon64.getTheta(E);
        }

//        See which type to choose different formula
        deltaEQ0ANDPreInfer[0] = in.readBit() == 1;

        int Theta = storedTheta;   // Theta can be removed
        int Alpha = Beta - Theta;  // Alpha also can be removed
        int GAlpha = deltaEQ0ANDPreInfer[0] ? Alpha + E : mon64.getFAlpha(Alpha) + E;
        boolean needConfirm = true;
        double sum = E >= 0 ? 1L << E : 1.0 / (1L << -E);
//        if(StoredXORLeadCount > 12) {
////            extract the interget part, that's for integer part!
////            sum += (vLong >> (52 - E)) & ((1L << E) - 1)
//        }
        double target = mon64.Pow10(Theta);
        for (int count = read - 11; count <= GAlpha; count++) {
            storedVal ^= ( (long) in.readBit() << (52 - count) );
            int single = (int) (( storedVal >> (52 - count) )) & 1;
            if(needConfirm && single != 0)  {
                sum += E - count >= 0 ? 1L << (E - count) : 1.0 / (1L << (count - E));
            }
            if(needConfirm && sum >= target) {
                needConfirm = false;
                Alpha--;
                GAlpha = deltaEQ0ANDPreInfer[0] ? Alpha + E : mon64.getFAlpha(Alpha) + E;
            }
        }
        alpha[0] = Alpha;
    }
}



