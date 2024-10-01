package org.urbcomp.startdb.compress.elf.decompressor;

import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;

public class MonkeyDe extends AbstractDecompressorWithInitialClassification {
    private final DecompressorWithInitialClassification xorDecompressor;

    public MonkeyDe(byte[] bytes) {
        xorDecompressor = new DecompressorWithInitialClassification(bytes);
    }

    @Override protected Double xorDecompress() {
        return xorDecompressor.readValue();
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
