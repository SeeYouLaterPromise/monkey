package org.urbcomp.startdb.compress.elf.decompressor;

import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;

public class MonkeyDynDe extends AbstractDecompressor2 {
    private final DecompressorDynamic xorDecompressor;

    public MonkeyDynDe(byte[] bytes) {
        xorDecompressor = new DecompressorDynamic(bytes);
    }

    @Override protected Double xorDecompress(int beta, int[] theta, boolean[] deltaEQ0) {
        return xorDecompressor.readValue(beta, theta, deltaEQ0);
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
