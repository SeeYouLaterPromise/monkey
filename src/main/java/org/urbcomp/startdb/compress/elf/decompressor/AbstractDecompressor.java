package org.urbcomp.startdb.compress.elf.decompressor;

import org.urbcomp.startdb.compress.elf.utils.mon64;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDecompressor implements IDecompressor {
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
            v = xorDecompress(-1, new int[1]);                    // case 10: non-erase, don't need beta
        } else {
            lastBetaStar = readInt(4);          // case 11: need beta, not equal to last beta value
            v = recoverVByBetaStar();
        }
        return v;
    }

    private Double recoverVByBetaStar() {
        double v;
        int[] theta = new int[1];
        theta[0] = -1;
        Double vPrime = xorDecompress(lastBetaStar, theta);
//        int sp = Elf64Utils.getSP(Math.abs(vPrime));
//        System.out.println("Theta: " + theta[0] + " , " + " SP: " + sp);
        theta[0] = vPrime < 1 || theta[0] == -1 ? mon64.getSP(Math.abs(vPrime)) + 1 : theta[0];
        if (lastBetaStar == 0) {
            v = mon64.get10iN(-theta[0]);
            if (vPrime < 0) {
                v = -v;
            }
        } else {
            int alpha = lastBetaStar - theta[0];
//            int alpha = lastBetaStar - theta[0];
            v = mon64.roundUp(vPrime, alpha);
        }
//        System.out.println("return " + "value of " + v + ", and its theta: " + theta[0]);
//        if(sp != theta[0] - 1) {
//            System.out.println("Which value? " + v + "; Theta: " + theta[0] + " , " + " SP: " + sp);
//        }
        return v;
    }

    protected abstract Double xorDecompress(int beta, int[] theta);

    protected abstract int readInt(int len);

}
