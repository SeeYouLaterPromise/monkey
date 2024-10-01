package org.urbcomp.startdb.compress.elf.compressor;

import org.urbcomp.startdb.compress.elf.utils.mon64;

public abstract class Abstract3Compressor implements ICompressor {
    //    private int count = 1;
    private int size = 0;
    private long vPrimeLong_Last = 0xfffffff;
    private int LastTrailing = -1;
    private int lastBetaStar = Integer.MAX_VALUE;

    public void addValue(double v) {
//        long start = System.nanoTime();

        long vLong = Double.doubleToRawLongBits(v);

//        Infinite shouldn't be talked about here, cus it would be confused with END_SIGN
//        more that, when it comes to compressing, the data are bound to be cleaned, NaN must be END_SIGN

        if (v == 0.0) {
            // subnormal 0
            LastTrailing = 64;
            size += writeInt(2, 2); // case 10
            size += xorCompress(vLong, -1, 0, false);
            return;
        }

//        normal, subnormal not 0
        int[] alphaAndBetaStar = mon64.getAlphaAndBetaStar(v, lastBetaStar);
        int E = mon64.getEFromValue(vLong);
        int gAlpha = mon64.getFAlpha(alphaAndBetaStar[0]) + E;
        int eraseBits = 52 - gAlpha;
        long mask = 0xffffffffffffffffL << eraseBits;

//        delta == 0 means cannot perform erasing
        long delta = (~mask) & vLong;
        if (delta != 0 && eraseBits > 4) {  // C2
            // perform erasing, remove some LSB
            if (alphaAndBetaStar[1] == lastBetaStar) {
                size += writeBit(false);   // case 0
            } else {
                lastBetaStar = alphaAndBetaStar[1];
                size += writeInt(alphaAndBetaStar[1] | 0x30, 6);  // case 11, 2 + 4 = 6
            }
            size += xorCompress(mask & vLong, lastBetaStar, alphaAndBetaStar[0], false);
        } else if(delta == 0 && eraseBits > 4){
            // non-perform erasing, but we know that, we could infer the trailing by alpha
            size += writeInt(2, 2); // case 10
            size += xorCompress(vLong, -1, alphaAndBetaStar[0], true);
        }else {
            // high-precision case
            size += writeInt(2, 2); // case 10
            size += xorCompress(vLong, -1, alphaAndBetaStar[0], false);
        }
    }

    public int getSize() {
        return size;
    }

    protected abstract int writeInt(int n, int len);

    protected abstract int writeBit(boolean bit);

    protected abstract int xorCompress(long vPrimeLong, int beta, int alpha, boolean deltaEQ0);

}
