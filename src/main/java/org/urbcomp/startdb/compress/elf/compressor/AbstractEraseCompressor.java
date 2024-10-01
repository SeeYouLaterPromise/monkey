package org.urbcomp.startdb.compress.elf.compressor;

import org.urbcomp.startdb.compress.elf.utils.mon64;

public abstract class AbstractEraseCompressor implements ICompressor {
    private int size = 0;
    private long vPrimeLong_Last = 0xfffffff;
    private int LastTrailing = -1;
    private int lastBetaStar = Integer.MAX_VALUE;

    public void addValue(double v) {

        long vLong = Double.doubleToRawLongBits(v);
        long vPrimeLong;
        int Beta_Cur = Integer.MAX_VALUE;
        int Alpha_Cur = Integer.MAX_VALUE;
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
            size += xorCompress(vPrimeLong, -1, 0, false);
            return;
        }

//        normal, subnormal not 0
        int[] alphaAndBetaStar = mon64.getAlphaAndBetaStar(v, lastBetaStar);
        Alpha_Cur = alphaAndBetaStar[0];
        Beta_Cur = alphaAndBetaStar[1];
//        Alpha_Cur = Elf64Utils.getDecimalPlaceCount(v);
//        int[] spAndFlag = Elf64Utils.getSPAnd10iNFlag(Math.abs(v));
//        int theta = spAndFlag[0] + 1;
//        Beta_Cur = spAndFlag[1] == 1 ? 0 : Alpha_Cur + theta;



        if(Beta_Cur == lastBetaStar) {
            BetaEqual = true;
        }

        int E = mon64.getEFromValue(vLong);
        int gAlpha = mon64.getFAlpha(Alpha_Cur) + E;
        int eraseBits = 52 - gAlpha;
        long mask = 0xffffffffffffffffL << eraseBits;

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

        // Test Module
        System.out.println("Data Manipulation:");
        System.out.println("Erased Bits: " + eraseBits);
        System.out.println("Before: " + v);
        mon64.showBinaryString(vLong);
        System.out.println("After: " + (Double.longBitsToDouble(vPrimeLong)));
        mon64.showBinaryString(vPrimeLong);
        System.out.println("Last value:");
        String I3E1 = mon64.showBinaryString(vPrimeLong_Last);
        System.out.println("Cur value: ");
        String I3E2 = mon64.showBinaryString(vPrimeLong);


        double v_last = Double.longBitsToDouble(vPrimeLong_Last);
        double v_cur = Double.longBitsToDouble(vPrimeLong);

        System.out.println("T(L): " + Long.numberOfTrailingZeros(vPrimeLong_Last));
        System.out.println("T(C): " + Long.numberOfTrailingZeros(vPrimeLong));
        System.out.println();

//        Need beta for line inferring
        System.out.println(Long.numberOfTrailingZeros(vPrimeLong) < Long.numberOfLeadingZeros(vPrimeLong_Last));

        int CurTrailing = Long.numberOfTrailingZeros(vPrimeLong);
        boolean needBetaToLineInfer = CurTrailing < LastTrailing;

//        need beta to infer the line
        if (deltaEQ0 && (needBetaToLineInfer || vPrimeLong_Last == 0xfffffff)) {
            storeBeta = true;
        }

        vPrimeLong_Last = vPrimeLong;
        LastTrailing = CurTrailing;

        if(storeBeta) {
            System.out.println("Need Beta to do something!");

            if (BetaEqual) {
                size += writeBit(false);   // case 0
            } else {
                lastBetaStar = Beta_Cur;
                size += writeInt(Beta_Cur | 0x30, 6);  // case 11, 2 + 4 = 6
            }

            size += xorCompress(vPrimeLong, lastBetaStar, Alpha_Cur, deltaEQ0);
        }else {
            size += writeInt(2, 2); // case 10

            size += xorCompress(vPrimeLong, -1, Alpha_Cur, deltaEQ0);
        }

    }

    public int getSize() {
        return size;
    }

    protected abstract int writeInt(int n, int len);

    protected abstract int writeBit(boolean bit);

    protected abstract int xorCompress(long vPrimeLong, int beta, int alpha, boolean deltaEQ0);

}
