package org.urbcomp.startdb.compress.elf.decompressor;

import gr.aueb.delorean.chimp.InputBitStream;

import org.urbcomp.startdb.compress.elf.utils.mon64;

import java.io.IOException;


public class PrimDecompressor {
    private long storedVal = 0;
    private int StoredXORLeadCount = 0;
    private boolean first = true;
    private boolean endOfStream = false;
    private final InputBitStream in;
    private final static long END_SIGN = Double.doubleToLongBits(Double.NaN);
    private final static short[] leadingRepresentation = {0, 8, 12, 16, 18, 20, 22, 24};

    public PrimDecompressor(byte[] bs) {
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
            firstValue(beta);
        } else {
            nextValue(beta);
        }

//            END
        if (storedVal == END_SIGN) {
            endOfStream = true;
        }
    }

    private void firstValue(int beta) throws IOException {
        first = false;
        // 1. first step -> extract E

        long lead = in.readLong(12);
        int E = mon64.getEFromLead(lead);
        if (E == -1023) {
            storedVal = 0;
            return;
        }else if (E == 1024) {
            storedVal = 0x7ff8000000000000L;
            return;
        }else if (beta == -1) {
            storedVal = (lead << 52) | (in.readLong(52));
            return;
        }

        // whether delta equals to 0
        boolean deltaEQ0 = in.readBit() == 1;

        // 2. get alpha from E and beta
        int alpha;
        int extra = 0;
        if(mon64.furtherCheck(E)) {
            // read further to reduce the cost (3 bits)
            long temp = in.readLong(E);
            long Integer_ = ( ( (long)1 ) << E ) | temp;

            int theta  = mon64.getTheta0(E);

            if (Integer_ >= mon64.Pow10(theta)) theta++;

            alpha = beta - theta;


            // concatenate the two part
            lead = (lead << E) | temp;
            extra = E;
        }else {
            alpha = beta - mon64.getTheta0(E);
        }

        // 3. get the infer line
        int ValueTrailCount;
        if (deltaEQ0) {
            // len(meaningful bits) == alpha
            ValueTrailCount = 52 - (alpha + E);
        }else {
            // len(meaningful bits) == gAlpha
            ValueTrailCount = 52 - (mon64.getFAlpha(alpha) + E);
        }

        // 4. read the left Mantissa, concatenating with lead
        long Mantissa = in.readLong(52 - ValueTrailCount - extra) << ValueTrailCount;
        storedVal = (lead << (52 - extra)) | Mantissa;

    }

    private void nextValue(int beta) throws IOException {
        int PreValueTrailCount = Long.numberOfTrailingZeros(storedVal);

//        write LeadCount or not?
        if(in.readBit() == 1) {
            StoredXORLeadCount = leadingRepresentation[in.readInt(3)];
        }else if(in.readBit() == 0) {
            // identical case!

            return;
        }

        long xor;

//        PreValue Could infer
        if (in.readBit() == 1) {
            xor = in.readLong(64 - StoredXORLeadCount - PreValueTrailCount) << PreValueTrailCount;
            storedVal ^= xor;
            return;
        }

//        whether delta equals to 0
        boolean deltaEQ0 = in.readBit() == 1;


//        From then on, we could know the LeadZeroCount, and then infer the next value's E
        long lead_xor = 0;
        int E = mon64.getEFromValue(storedVal);

//        Should read more to get the E
        if(StoredXORLeadCount < 12) {
            lead_xor = in.readLong(12 - StoredXORLeadCount);
            E = (int)( (lead_xor ^ (storedVal  >>> 52) ) & 0x7ff) - 1023;
        }
        int read = Math.max(12, StoredXORLeadCount);

        if (E == -1023) {
            storedVal = 0;
            return;
        }else if (E == 1024) {
            storedVal = 0x7ff8000000000000L;
            return;
        }else if (beta == -1) {
            xor = (lead_xor << (64 - read)) | in.readLong(64 - read);
            storedVal ^= xor;
            return;
        }


//        PreValue couldn't infer



        // get alpha from E and beta
        int alpha;

        if (E > 0 && read >= 12 + E) {
            double v_l = Double.longBitsToDouble(storedVal);
            int theta = mon64.getTheta0(E);
            if (Math.abs(v_l) >= mon64.Pow10(theta)) theta++;
            alpha = beta - theta;
        } else if(mon64.furtherCheck(E)) {
            long temp_xor = in.readLong(12 - read + E);
            lead_xor = (lead_xor << (12 - read + E)) | temp_xor;
            read = 12 + E;
            long mask = (1L << E) - 1;
            long temp = temp_xor ^ ((storedVal >>> (52 - E)) & mask);
            long Integer_ = ( ( (long)1 ) << E ) | temp;
            int theta  = mon64.getTheta0(E);
            if (Integer_ >= mon64.Pow10(theta)) theta++;
            alpha = beta - theta;
        }else {
            // general case: for some non-fractional and all fractional, don't need check further, but may get the concise alpha
            alpha = beta - mon64.getTheta0(E);
        }

        if (deltaEQ0) {
            PreValueTrailCount = 52 - (alpha + E);
        }else {
            PreValueTrailCount = 52 - (mon64.getFAlpha(alpha) + E);
        }

        long Mantissa_xor = in.readLong(64 - read - PreValueTrailCount ) << PreValueTrailCount;
        xor = ( (lead_xor << (64 - read)) | Mantissa_xor );
        storedVal ^= xor;
    }
}



