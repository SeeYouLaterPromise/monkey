package org.urbcomp.startdb.compress.elf.compressor;

import org.urbcomp.startdb.compress.elf.utils.Couple;
import org.urbcomp.startdb.compress.elf.utils.mon64;


public abstract class AbstractPairCompressor implements IPairCompressor {
    private int size_v = 0;
    private int size_t = 0;
    private int count = 0;
    Couple<Long, Integer>[] blockValue = new Couple[10];
    double[] blockTime = new double[10];
    private int lastBetaStar = Integer.MAX_VALUE;

    public void addValue(double t, double v) {
//        first adding
        if(count == 0) {
            blockValue[count] = erase(v);
            blockTime[count++] = t;
            return;
        }
//        sort array by #TailingZeros after erasing  ->Simple version: insertion sort
        for(int i = 1; i < ++count; i++) {
            int j = i - 1;
            Couple<Long, Integer> temp = erase(v);
            int CurValueTailCount = Long.numberOfTrailingZeros(temp.getL());
            while (j >= 0 && Long.numberOfTrailingZeros(blockValue[j].getL()) > CurValueTailCount) {
                blockValue[j + 1] = blockValue[j];
                blockTime[j + 1] = blockTime[j];
                j--;
            }
            blockValue[j + 1] = temp;
            blockTime[j + 1] = t;
        }
//        perform compressing this blockValue values
        if(count % 10 == 0) {
//            Reset
            count = 0;
            for(int i = 0; i < 10; i++) {
                lastBetaStar = blockValue[i].getR();
                if(lastBetaStar == -1) size_v += writeInt_ValueCmp(2, 2);
                else size_v += writeInt_ValueCmp(3, 2);
                size_v += ValueCompress(blockValue[i].getL());
                size_t += TimeCompress(Double.doubleToLongBits(blockTime[i]));
            }
        }
    }

    public Couple<Long, Integer> erase(double v) {
        long vLong = Double.doubleToRawLongBits(v);
        long vPrimeLong;

        if (v == 0.0 || Double.isInfinite(v)) {
            lastBetaStar = -1;
            vPrimeLong = vLong;
        } else if (Double.isNaN(v)) {
            lastBetaStar = -1;
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
                lastBetaStar = alphaAndBetaStar[1];
                vPrimeLong = mask & vLong;
            } else {
                // don't perform erasing
                lastBetaStar = -1;
                vPrimeLong = vLong;
            }
        }
        return new Couple<Long, Integer>(vPrimeLong, lastBetaStar);
    }

    public int getSizeValue() {
        return size_v;
    }

    public int getSizeTime() {
        return size_t;
    }

    protected abstract int writeInt_ValueCmp(int n, int len);

    protected abstract int writeBit_ValueCmp(boolean bit);

    protected abstract int writeInt_TimeCmp(int n, int len);

    protected abstract int writeBit_TimeCmp(boolean bit);

    protected abstract int ValueCompress(long vPrimeLong);

    protected abstract int TimeCompress(long tPrimeLong);

}
