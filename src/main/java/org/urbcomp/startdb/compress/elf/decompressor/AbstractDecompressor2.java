package org.urbcomp.startdb.compress.elf.decompressor;

import org.urbcomp.startdb.compress.elf.utils.mon64;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDecompressor2 implements IDecompressor {
    private int lastBetaStar = Integer.MAX_VALUE;
    private int storedAlpha = Integer.MAX_VALUE;

    public List<Double> decompress() {
        List<Double> values = new ArrayList<>(1024);
        Double value;
        while ((value = nextValue()) != null) {
            values.add(value);
        }
        return values;
    }

    private Double nextValue() {
        Double v;

        if(readInt(1) == 0) {
            v = recoverVByBetaStar();               // case 0: need beta, could use the last beta value
        } else if (readInt(1) == 0) {
            v = xorDecompress(-1, new int[1], new boolean[2]);                    // case 10: non-erase, don't need beta
        } else {
            lastBetaStar = readInt(4);          // case 11: need beta, not equal to last beta value
            v = recoverVByBetaStar();
        }
        return v;
    }

    private Double recoverVByBetaStar() {
        double v;
        int[] alpha = new int[1];
        alpha[0] = -1;
        boolean[] deltaEQ0ANDPreInfer = new boolean[3];
        Double vPrime = xorDecompress(lastBetaStar, alpha, deltaEQ0ANDPreInfer);
        if (deltaEQ0ANDPreInfer[0]) return vPrime;
        else if (deltaEQ0ANDPreInfer[1]) {
            int sp = mon64.getSP(Math.abs(vPrime));
            alpha[0] = lastBetaStar - sp - 1;
        }
//        if (deltaEQ0ANDPreInfer[2]) alpha[0] = storedAlpha
        if (lastBetaStar == 0) {
            v = mon64.get10iN(alpha[0] - lastBetaStar);
            if (vPrime < 0) {
                v = -v;
            }
        } else {
            v = mon64.roundUp(vPrime, alpha[0]);
        }
        return v;
    }

    protected abstract Double xorDecompress(int beta, int[] alpha, boolean[] deltaEQ0);

    protected abstract int readInt(int len);

}
