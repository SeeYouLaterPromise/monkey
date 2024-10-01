package org.urbcomp.startdb.compress.elf.compressor;

import org.urbcomp.startdb.compress.elf.utils.mon64;
import org.apache.commons.lang3.tuple.Pair;


public abstract class AbstractSortCompressor implements ICompressor {
    private int size = 0;
    private int lastBetaStar = Integer.MAX_VALUE;
    private Pair<Long, Integer>[] erased;
    private int count = 0;

    public void addValue(double v) {
        long vLong = Double.doubleToRawLongBits(v);
        long vPrimeLong;

        if (v == 0.0 || Double.isInfinite(v)) {
            vPrimeLong = vLong;
        } else if (Double.isNaN(v)) {
            vPrimeLong = 0x7ff8000000000000L;
        } else {
            // C1: v is a normal or subnormal
            int[] alphaAndBetaStar = mon64.getAlphaAndBetaStar(v, lastBetaStar);
            int e = ((int) (vLong >> 52)) & 0x7ff;
            int gAlpha = mon64.getFAlpha(alphaAndBetaStar[0]) + e - 1023;
            int eraseBits = 52 - gAlpha;
            long mask = 0xffffffffffffffffL << eraseBits;
            long delta = (~mask) & vLong;
            if (delta != 0 && eraseBits > 4) {  // C2
                if(alphaAndBetaStar[1] != lastBetaStar) lastBetaStar = alphaAndBetaStar[1];
                vPrimeLong = mask & vLong;
            } else {
                vPrimeLong = vLong;
            }
        }
        Pair<Long, Integer> pair = Pair.of(vPrimeLong, lastBetaStar);
//        erased[count++] = pair;
//        perform insertion sorting
        for(int i = 0; i < ++count; i++) {

        }
        size += xorCompress(vPrimeLong);
    }

    public int getSize() {
        return size;
    }

    protected abstract int writeInt(int n, int len);

    protected abstract int writeBit(boolean bit);

    protected abstract int xorCompress(long vPrimeLong);
}
