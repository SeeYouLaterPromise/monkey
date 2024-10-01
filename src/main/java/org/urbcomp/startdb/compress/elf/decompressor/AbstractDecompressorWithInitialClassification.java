package org.urbcomp.startdb.compress.elf.decompressor;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDecompressorWithInitialClassification implements IDecompressor {
    public List<Double> decompress() {
        List<Double> values = new ArrayList<>(1024);
        Double value;
        while ((value = xorDecompress()) != null) {
            values.add(value);
        }
        return values;
    }
    protected abstract Double xorDecompress();

    protected abstract int readInt(int len);

}
