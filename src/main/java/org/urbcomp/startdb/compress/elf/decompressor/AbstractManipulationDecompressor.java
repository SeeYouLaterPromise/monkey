package org.urbcomp.startdb.compress.elf.decompressor;

import org.urbcomp.startdb.compress.elf.utils.mon64;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractManipulationDecompressor implements IDecompressor {
    private int lastBetaStar = Integer.MAX_VALUE;
    private int set;

    public List<Double> decompress() {
        List<Double> values = new ArrayList<>(1024);
        Double value;
        while ((value = nextValue()) != null) {
            values.add(value);
        }
        return values;
    }

    private Double nextValue() {
        Double v = null;
        int erased = readInt(1);
        int readBeta = 0;
        if (set == 1 || erased == 1) {
            readBeta = readInt(0);
        }
        int flag = 0;


        switch (flag) {
            case 0:
                v = xorDecompress(-1);
                break;
            case 2:
//                only erasing
                if(readBeta == 1) {
                    lastBetaStar = readInt(4);
                }
                v = recoverVByPlus();
                break;
            case 1:
//                only setting
                if(readBeta == 1) {
                    lastBetaStar = readInt(4);
                }
                v = recoverVByMinus();
                break;
            case 3:
//                erasing, then setting
                if(readBeta == 1) {
                    lastBetaStar = readInt(4);
                }
                v = recoverVByPlus();
                break;
        }
        set =  readInt(1);
        return v;
    }

    private Double recoverVByPlus() {
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

    private Double recoverVByMinus() {
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
            v = mon64.roundDown(vPrime, alpha);
        }
        return v;
    }

    protected abstract Double xorDecompress(int beta);

    protected abstract int readInt(int len);

}
