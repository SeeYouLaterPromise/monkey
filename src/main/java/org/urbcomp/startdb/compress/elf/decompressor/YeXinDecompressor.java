package org.urbcomp.startdb.compress.elf.decompressor;

import gr.aueb.delorean.chimp.InputBitStream;
import org.urbcomp.startdb.compress.elf.xordecompressor.ElfXORDecompressor;

import java.io.IOException;

public class YeXinDecompressor extends AbstractManipulationDecompressor {
    private final YeXinXORDecompressor xorDecompressor;

    public YeXinDecompressor(byte[] bytes) {
        xorDecompressor = new YeXinXORDecompressor(bytes);
    }

    @Override protected Double xorDecompress(int beta) {
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
