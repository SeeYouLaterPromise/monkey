package org.urbcomp.startdb.compress.elf.decompressor;

import org.urbcomp.startdb.compress.elf.utils.mon64;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractEraseDecompressor implements IDecompressor {
    private int lastBetaStar = Integer.MAX_VALUE;

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
            v = xorDecompress(-1);                    // case 10: non-erase, don't need beta
        } else {
            lastBetaStar = readInt(4);          // case 11: need beta, not equal to last beta value
            v = recoverVByBetaStar();
        }
        return v;
    }

    private Double recoverVByBetaStar() {
        double v;
        Double vPrime = xorDecompress(lastBetaStar);
        int sp = mon64.getSP(Math.abs(vPrime));
        if (lastBetaStar == 0) {
            v = mon64.get10iN(-sp - 1);
            if (vPrime < 0) {
                v = -v;
            }
        } else {
            int alpha = lastBetaStar - sp - 1;
            v = mon64.roundUp(vPrime, alpha);
        }
        return v;
    }

    protected abstract Double xorDecompress(int beta);

    protected abstract int readInt(int len);

}
