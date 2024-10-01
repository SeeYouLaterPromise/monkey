package org.urbcomp.startdb.compress.elf.compressor;

public abstract class AbstractCompressorWithInitialClassification implements ICompressor {
    private int size = 0;

    public void addValue(double v) {
        size += xorCompress(v);
    }

    public int getSize() {
        return size;
    }

    protected abstract int writeInt(int n, int len);

    protected abstract int writeBit(boolean bit);

    protected abstract int xorCompress(double v);
}
