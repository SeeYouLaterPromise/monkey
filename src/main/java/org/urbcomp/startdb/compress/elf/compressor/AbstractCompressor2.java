package org.urbcomp.startdb.compress.elf.compressor;

import org.urbcomp.startdb.compress.elf.utils.mon64;

public abstract class AbstractCompressor2 implements ICompressor {
    //    private int count = 1;
    private int size = 0;
    private long vPrimeLong_Last = 0xfffffff;
    private int LastTrailing = -1;
    private int lastBetaStar = Integer.MAX_VALUE;

    public void addValue(double v) {
//        long start = System.nanoTime();

        long vLong = Double.doubleToRawLongBits(v);
        long vPrimeLong;
//        int alphaAndBetaStar[1] = Integer.MAX_VALUE;
//        int Alpha_Cur = Integer.MAX_VALUE;
        boolean storeBeta = false;
        boolean BetaEqual = false;
        boolean deltaEQ0 = false;

//        Infinite shouldn't be talked about here, cus it would be confused with END_SIGN
//        more that, when it comes to compressing, the data are bound to be cleaned, NaN must be END_SIGN

        if (v == 0.0) {
            // subnormal 0
            vPrimeLong = vLong;
            LastTrailing = 64;
            size += writeInt(2, 2); // case 10
            size += xorCompress(vPrimeLong, -1, 0, false, -1);
            return;
        }

//        normal, subnormal not 0
        int[] alphaAndBetaStar = mon64.getAlphaAndBetaStar(v, lastBetaStar);


        if(alphaAndBetaStar[1] == lastBetaStar) {
            BetaEqual = true;
        }

        int E = mon64.getEFromValue(vLong);
        int gAlpha = mon64.getFAlpha(alphaAndBetaStar[0]) + E;
        int eraseBits = 52 - gAlpha;
        long mask = 0xffffffffffffffffL << eraseBits;

//        delta == 0 means cannot perform erasing
        long delta = (~mask) & vLong;
        if (delta != 0 && eraseBits > 4) {  // C2
            // perform erasing, remove some LSB
            storeBeta = true;
            vPrimeLong = mask & vLong;
        } else if(delta == 0 && eraseBits > 4){
            // non-perform erasing, but we know that, we could infer the trailing by alpha
            deltaEQ0 = true;
            vPrimeLong = vLong;
        }else {
            // high-precision case
            vPrimeLong = vLong;
        }


        int CurTrailing = Long.numberOfTrailingZeros(vPrimeLong);
        boolean needBetaToLineInfer = CurTrailing < LastTrailing;

//        need beta to infer the line
        if (deltaEQ0 && (needBetaToLineInfer || vPrimeLong_Last == 0xfffffff)) {
            storeBeta = true;
        }

        vPrimeLong_Last = vPrimeLong;
        LastTrailing = CurTrailing;

        if(storeBeta) {

            if (BetaEqual) {
                size += writeBit(false);   // case 0
            } else {
                lastBetaStar = alphaAndBetaStar[1];
                size += writeInt(alphaAndBetaStar[1] | 0x30, 6);  // case 11, 2 + 4 = 6
            }

            size += xorCompress(vPrimeLong, lastBetaStar, alphaAndBetaStar[0], deltaEQ0, gAlpha);
        }else {
            size += writeInt(2, 2); // case 10
            size += xorCompress(vPrimeLong, -1, alphaAndBetaStar[0], deltaEQ0, -1);
        }
    }

    public int getSize() {
        return size;
    }

    protected abstract int writeInt(int n, int len);

    protected abstract int writeBit(boolean bit);

    protected abstract int xorCompress(long vPrimeLong, int beta, int alpha, boolean deltaEQ0, int GAlpha);

}
