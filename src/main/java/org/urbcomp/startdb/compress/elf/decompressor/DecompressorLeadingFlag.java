package org.urbcomp.startdb.compress.elf.decompressor;

import gr.aueb.delorean.chimp.InputBitStream;

import org.urbcomp.startdb.compress.elf.utils.mon64;

import java.io.IOException;


public class DecompressorLeadingFlag {
    private boolean deltaEQ0 = false;
    private boolean negative = false;
    private long storedVal = 0;
    private int E = Integer.MAX_VALUE;
    private int storedE = Integer.MIN_VALUE;
    private int storedThetaCalculationResult = Integer.MIN_VALUE;
    private int Alpha = Integer.MIN_VALUE;
    private int lastBetaStar = Integer.MAX_VALUE;   // Refer to current BetaStar as well.
    private int StoredXORLeadCount = Integer.MAX_VALUE;
    private int storedTrailingZeros = Integer.MAX_VALUE;
    private boolean first = true;
    private boolean endOfStream = false;
    private final InputBitStream in;
    private final static long END_SIGN = Double.doubleToLongBits(Double.NaN);
    private final static short[] leadingRepresentation = {0, 8, 12, 16, 18, 20, 22, 24};

    public DecompressorLeadingFlag(byte[] bs) {
        in = new InputBitStream(bs);
    }

    public InputBitStream getInputStream() {
        return in;
    }

    private Double recoverVByBetaStar() throws IOException {
        next();
        double vPrime = Double.longBitsToDouble(storedVal);

        if (deltaEQ0) {
            deltaEQ0 = false;
            return vPrime;
        }

        double v;
        if (E < 0) {
//            inferred index does not need to calculate alpha to get index.
            Alpha = lastBetaStar - mon64.getSP(Math.abs(vPrime)) - 1;
        }

        if (lastBetaStar == 0) {
            int theta = mon64.getTheta(E);
            v = mon64.get10iN(-theta);
            double temp = 1.0 / (1L << -E);
            if (negative) {
                v = -v;
                temp = -temp;
            }
            storedVal = Double.doubleToLongBits(temp);
            storedTrailingZeros = Long.numberOfTrailingZeros(storedVal);
        } else {
            v = mon64.roundUp(vPrime, Alpha);
        }
        return v;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Double readValue() {
        Double v;
        try {
//            read lastBetaStar
            if(in.readInt(1) == 0) {
                v = recoverVByBetaStar();               // case 0: need BetaStar, could use the last lastBetaStar value
            } else if (in.readInt(1) == 0) {
                // high-precision and precise type without pre-value's information.
                lastBetaStar = Integer.MAX_VALUE;
                next();                                 // case 10: non-erase, don't need lastBetaStar
                v = Double.longBitsToDouble(storedVal);
            } else {
                lastBetaStar = in.readInt(4);       // case 11: need BetaStar, not equal to last lastBetaStar value
                v = recoverVByBetaStar();
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        if (endOfStream) {
            return null;
        }
        return v;
    }

    private void next() throws IOException {
        if (first) {
            firstValue();
        } else {
            nextValue();
        }

//            END
        if (storedVal == END_SIGN) {
            endOfStream = true;
        }
    }

    private void firstValue() throws IOException {
        first = false;

//        1. first step -> extract E
        long lead = in.readLong(12);
        E = mon64.getEFromLead(lead);
        switch (E) {
            case -1023:
                storedVal = 0;
                storedTrailingZeros = Long.numberOfTrailingZeros(storedVal);
                return;
            case 1024:
                storedVal = 0x7ff8000000000000L;
                storedTrailingZeros = Long.numberOfTrailingZeros(storedVal);
                return;
        }

        switch (lastBetaStar) {
            case 0:
                // 10^theta case
                negative = ((lead >>> 11) & ((1L << 1) - 1)) == 1;
                return;
            case Integer.MAX_VALUE:
                // process high precision case subsequently
                storedVal = (lead << 52 | in.readLong(52));
                storedTrailingZeros = Long.numberOfTrailingZeros(storedVal);
                return;
        }

//        whether delta equals to 0
        deltaEQ0 = in.readBit() == 1;

//         2. get alpha from E and lastBetaStar
        int extra = 0;
        storedE = E;
        // store the calculation result, pay attention!!!
        storedThetaCalculationResult = mon64.getTheta(E); // storedThetaCalculationResult is used to reuse the theta calculation result.
        Alpha = lastBetaStar - storedThetaCalculationResult;
//        Revision for integers
        if (E >= 0) {
            double power10theta = mon64.Pow10(storedThetaCalculationResult);
            if ((1L << (E + 1)) > power10theta) {
                long temp = in.readLong(E);
                lead = (lead << E) | temp;
                extra = E;
                long Integer_ = (1L << E) | temp;
                if (Integer_ >= mon64.Pow10(storedThetaCalculationResult)) {
                    Alpha--;
                }
            }
        }
//        3. get the infer line
        int RightBoundary = deltaEQ0 ? Alpha + E : mon64.getFAlpha(Alpha) + E;

//        4. read the left Mantissa, concatenating with lead
        long Mantissa = in.readLong(RightBoundary - extra) << (52 - RightBoundary);
        storedVal = (lead << (52 - extra)) | Mantissa;
        storedTrailingZeros = Long.numberOfTrailingZeros(storedVal);
    }

    private void nextValue() throws IOException {
        if (in.readBit() == 0) {
            if (in.readBit() != 1) {
                storedVal ^= in.readLong(64 - StoredXORLeadCount - storedTrailingZeros) << storedTrailingZeros;
                storedTrailingZeros = Long.numberOfTrailingZeros(storedVal);
            }
            return;
        }
        StoredXORLeadCount = leadingRepresentation[in.readInt(3)];

//        Then, process with 0 and END_SIGN through E value
        if(StoredXORLeadCount < 12) {
            // Do modifications on storedVal directly.
            storedVal ^= (in.readLong(12 - StoredXORLeadCount) << 52);
        }
        E = mon64.getEFromValue(storedVal);
        int read = Math.max(12, StoredXORLeadCount);
        switch (E) {
            case -1023:
                storedVal = 0;
                storedTrailingZeros = Long.numberOfTrailingZeros(storedVal);
                return;
            case 1024:
                storedVal = 0x7ff8000000000000L;
                storedTrailingZeros = Long.numberOfTrailingZeros(storedVal);
                return;
        }

        switch (lastBetaStar) {
            case 0:
                // 10^theta case
                negative = ((storedVal >>> 63) & ((1L << 1) - 1)) == 1;
                return;
            case Integer.MAX_VALUE:
                // process high precision case subsequently
                storedVal ^= in.readLong(64 - read);
                storedTrailingZeros = Long.numberOfTrailingZeros(storedVal);
                return;
        }

//        General Case: index calculation
        // whether delta equals to 0
        deltaEQ0 = in.readBit() == 1;

        // get alpha from E and lastBetaStar
        if (E != storedE) {
            storedE = E;
            storedThetaCalculationResult = mon64.getTheta(E);
        }
        Alpha = lastBetaStar - storedThetaCalculationResult;

        // v < 1 remain non-revision
        if (E > 0) {
            double power10theta = mon64.Pow10(storedThetaCalculationResult);
            if (E > read - 12) {
                storedVal ^= in.readLong(E + 12 - read) << (52 - E);
                read = 12 + E;
            }
            if ((1L << (E + 1)) > power10theta) {
                long temp = (storedVal >>> (52 - E)) & ((1L << E) - 1);
                int IntegerVal = (int)((1L << E) | temp);
                if (IntegerVal >= power10theta) {
                    Alpha--;
                }
            }
        }


        int RightBoundary = deltaEQ0 ? Alpha + E : mon64.getFAlpha(Alpha) + E;
        storedVal ^= in.readLong(RightBoundary + 12 - read) << (52 - RightBoundary);
        storedTrailingZeros = Long.numberOfTrailingZeros(storedVal);
    }
}



