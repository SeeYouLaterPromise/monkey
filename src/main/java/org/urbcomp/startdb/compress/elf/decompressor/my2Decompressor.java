package org.urbcomp.startdb.compress.elf.decompressor;

import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;

public class my2Decompressor extends AbstractDecompressor {
    private final PrimDecompressor xorDecompressor;

    public my2Decompressor(byte[] bytes) {
        xorDecompressor = new PrimDecompressor(bytes);
    }

    @Override protected Double xorDecompress(int beta, int[] theta) {
        return xorDecompressor.readValue(beta);
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
