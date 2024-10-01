package org.urbcomp.startdb.compress.elf.compressor;

import org.urbcomp.startdb.compress.elf.utils.mon64;

public abstract class AbstractManipulationCompressor implements I2Compressor{
    private int size = 0;
    private boolean first = true;
    private boolean erased;
//    first value don't need to reconstruct
    private boolean set = false;
    private int PreBetaStar = Integer.MAX_VALUE;

//    avoid repeating calculation
    private long vPrimeLong;
    private int BetaStar_Last = Integer.MAX_VALUE;
    private int gAlpha_Last = Integer.MAX_VALUE;

    private boolean erasing(double v) {
        long vLong = Double.doubleToLongBits(v);
        if (v == 0.0 || Double.isInfinite(v)) {
            vPrimeLong = vLong;
            return false;
        }else if (Double.isNaN(v)) {
            vPrimeLong = 0x7ff8000000000000L;
            return false;
        }else {
            int E = (((int)(vLong >> 52)) & 0x7ff) - 1023;
            int[] array = mon64.getAlphaAndBetaStar(v, Integer.MAX_VALUE);
            BetaStar_Last = array[1];
            gAlpha_Last = mon64.getFAlpha(array[0]) + E;
            int eraseBits = 52 - gAlpha_Last;
            long mask = 0xffffffffffffffffL << eraseBits;
            long delta = (~mask) & vLong;
            if (delta != 0 && eraseBits > 4) {
                vPrimeLong = vLong & mask;
                return true;
            }else {
                vPrimeLong = vLong;
                return false;
            }
        }
    }

    private long setting(long v_cur, int gAlpha_cur, int gAlpha_next) {
        if(gAlpha_next > gAlpha_cur) {
            gAlpha_cur = gAlpha_next;
        }
        long set_operator = (long)1 << (52 - gAlpha_cur);
        return v_cur | set_operator;
    }

    public void manipulate(double v_cur, double v_next) {
        if(first) {
            erased = erasing(v_cur);
        }


//        get information from last turn of manipulation
        long vPrimeLong_cur1 = vPrimeLong;

        double v_cur_erased = Double.longBitsToDouble(vPrimeLong_cur1);

        int gAlpha_cur = gAlpha_Last;
        int beta_cur = BetaStar_Last;

        if(erased) {
            size += writeBit(true);
        }else {
            size += writeBit(false);
        }

        boolean erased_next = erasing(v_next);
//        long vPrimeLong_next = vPrimeLong; // could be removed

        double v_next_erased = Double.longBitsToDouble(vPrimeLong);

//        based on last turn analysis, we should transform v_(t-1)1 -> v_(t-1)2 to index the trailing count
//        need the next value's beta to build a re-constructable set operator
        if(erased || set) {
            if(beta_cur == PreBetaStar) {
                size += writeBit(false);
            }else {
                size += writeBit(true);
                size += writeInt(beta_cur, 4);
            }
        }

        long vPrimeLong_cur2 = vPrimeLong_cur1;
        int TrailingCountOfNextValue = Long.numberOfTrailingZeros(vPrimeLong);
//        judge the case need to perform set operation or not?
        if(first) {
            size += writeBit(false);
            if(Long.numberOfTrailingZeros(vPrimeLong_cur1) > TrailingCountOfNextValue) {
                long set_operator = (long)1 << TrailingCountOfNextValue;
                vPrimeLong_cur2 = vPrimeLong_cur1 | set_operator;
            }
            first = false;
        }else {
            if(Long.numberOfTrailingZeros(vPrimeLong_cur1) > TrailingCountOfNextValue) {
                size += writeBit(true);
                set = true;
                // calculate the gAlpha_approximate
                int E = (((int)(vPrimeLong >> 52)) & 0x7ff) - 1023;
                int theta = mon64.getTheta(E);
                int Alpha_approximate = beta_cur - theta;
                int gAlpha_approximate = mon64.getFAlpha(Alpha_approximate) + E;
                vPrimeLong_cur2 = setting(vPrimeLong_cur1, gAlpha_cur, gAlpha_approximate);
            }else {
                size += writeBit(false);
                set = false;
            }
        }

        double v_cur_set = Double.longBitsToDouble(vPrimeLong_cur2);

        erased = erased_next;
        PreBetaStar = beta_cur;

        size += xorCompress(vPrimeLong_cur1, vPrimeLong_cur2);
    }

    public int getSize() {
        return size;
    }

    protected abstract int writeInt(int n, int len);

    protected abstract int writeBit(boolean bit);

    protected abstract int xorCompress(long vPrimeLong1, long vPrimeLong2);

}
