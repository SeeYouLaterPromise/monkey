package org.urbcomp.startdb.compress.elf.compressor;

import org.urbcomp.startdb.compress.elf.utils.mon64;

public abstract class AbstractElfCompressor implements ICompressor {
//    private int count = 1;
    private int size = 0;
    private int lastBetaStar = Integer.MAX_VALUE;

    public void addValue(double v) {
//        long start = System.nanoTime();

        long vLong = Double.doubleToRawLongBits(v);
        long vPrimeLong;

        if (v == 0.0 || Double.isInfinite(v)) {
            size += writeInt(2, 2); // case 10
            vPrimeLong = vLong;
        } else if (Double.isNaN(v)) {
            size += writeInt(2, 2); // case 10
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
                if(alphaAndBetaStar[1] == lastBetaStar) {
                    size += writeBit(false);    // case 0
                } else {
                    size += writeInt(alphaAndBetaStar[1] | 0x30, 6);  // case 11, 2 + 4 = 6
                    lastBetaStar = alphaAndBetaStar[1];
                }
                vPrimeLong = mask & vLong;
            } else {
                // don't perform erasing

                if (v != 0.5 && 52 - Long.numberOfTrailingZeros(vLong) != alphaAndBetaStar[0] + e - 1023){
                    System.out.println("Value: " + v + ", Alpha: " + alphaAndBetaStar[1] + ", Beta: " + alphaAndBetaStar[1]);
                }

                size += writeInt(2, 2); // case 10
                vPrimeLong = vLong;
            }
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
