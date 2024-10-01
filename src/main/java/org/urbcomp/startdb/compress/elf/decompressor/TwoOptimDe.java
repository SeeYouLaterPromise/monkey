package org.urbcomp.startdb.compress.elf.decompressor;

import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;

public class TwoOptimDe extends AbstractDecompressor {
    private final Decompressor xorDecompressor;

    public TwoOptimDe(byte[] bytes) {
        xorDecompressor = new Decompressor(bytes);
    }

    @Override protected Double xorDecompress(int beta, int[] theta) {
        return xorDecompressor.readValue(beta, theta);
    }

    @Override protected int readInt(int len) {
        InputBitStream in = xorDecompressor.getInputStream();
        try {
            return in.readInt(len);
        } catch (IOException e) {
            throw new RuntimeException("IO error: " + e.getMessage());
        }
    }
}
