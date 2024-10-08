package org.urbcomp.startdb.compress.elf.compressor;

import gr.aueb.delorean.chimp.OutputBitStream;

public class YeXinCompressor extends AbstractManipulationCompressor {
    private final YeXinXORCompressor xorCompressor;

    public YeXinCompressor() {
        xorCompressor = new YeXinXORCompressor();
    }

    @Override protected int writeInt(int n, int len) {
        OutputBitStream os = xorCompressor.getOutputStream();
        os.writeInt(n, len);
        return len;
    }

    @Override protected int writeBit(boolean bit) {
        OutputBitStream os = xorCompressor.getOutputStream();
        os.writeBit(bit);
        return 1;
    }

    @Override protected int xorCompress(long vPrimeLong1, long vPrimeLong2) {
        return xorCompressor.compress(vPrimeLong1, vPrimeLong2);
    }

    @Override public byte[] getBytes() {
        return xorCompressor.getOut();
    }

    @Override public void close() {
        // we write one more bit here, for marking an end of the stream.
        writeInt(0,2);  // case 10 -> for non-erase
        xorCompressor.close();
    }
}
