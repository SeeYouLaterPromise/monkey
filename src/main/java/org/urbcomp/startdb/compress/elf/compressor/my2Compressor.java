package org.urbcomp.startdb.compress.elf.compressor;

import gr.aueb.delorean.chimp.OutputBitStream;

public class my2Compressor extends AbstractCompressor {
    private final PrimCompressor xorCompressor;

    public my2Compressor() {
        xorCompressor = new PrimCompressor();
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

    @Override protected int xorCompress(long vPrimeLong, int beta, int alpha, boolean deltaEQ0) {
        return xorCompressor.addValue(vPrimeLong, beta, alpha, deltaEQ0);
    }

    @Override public byte[] getBytes() {
        return xorCompressor.getOut();
    }

    @Override public void close() {
        // we write one more bit here, for marking an end of the stream.
        writeInt(2,2);  // case 10 -> for non-erase
        xorCompressor.close();
    }
}
