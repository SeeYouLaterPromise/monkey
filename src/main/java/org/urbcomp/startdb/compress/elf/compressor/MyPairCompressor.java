package org.urbcomp.startdb.compress.elf.compressor;

import gr.aueb.delorean.chimp.OutputBitStream;
import org.urbcomp.startdb.compress.elf.xorcompressor.ElfXORCompressor;

public class MyPairCompressor extends AbstractPairCompressor {
    private final MonkeyXORCompressor ValueCompressor;
    private final ElfXORCompressor TimeCompressor;
    public MyPairCompressor() {
        ValueCompressor = new MonkeyXORCompressor();
        TimeCompressor = new ElfXORCompressor();
    }

    @Override protected int writeInt_ValueCmp(int n, int len) {
        OutputBitStream os = ValueCompressor.getOutputStream();
        os.writeInt(n, len);
        return len;
    }

    @Override protected int writeBit_ValueCmp(boolean bit) {
        OutputBitStream os = ValueCompressor.getOutputStream();
        os.writeBit(bit);
        return 1;
    }

    @Override protected int writeInt_TimeCmp(int n, int len) {
        OutputBitStream os = TimeCompressor.getOutputStream();
        os.writeInt(n, len);
        return len;
    }

    @Override protected int writeBit_TimeCmp(boolean bit) {
        OutputBitStream os = TimeCompressor.getOutputStream();
        os.writeBit(bit);
        return 1;
    }

    @Override protected int ValueCompress(long vPrimeLong) {
        return ValueCompressor.addValue(vPrimeLong);
    }

    @Override
    protected int TimeCompress(long tPrimeLong) {
        return TimeCompressor.addValue(tPrimeLong);
    }

    @Override public byte[] getBytes_Value() {
        return ValueCompressor.getOut();
    }

    @Override public void close_Value() {
        // we write one more bit here, for marking an end of the stream.
        writeInt_ValueCmp(2,2);  // case 10 -> for non-erase cooperate with elf flag
        ValueCompressor.close();
    }

    @Override public byte[] getBytes_Time() {
        return TimeCompressor.getOut();
    }

    @Override public void close_Time() {
        // we write one more bit here, for marking an end of the stream.
        writeInt_TimeCmp(2,2);  // case 10 -> for non-erase cooperate with elf flag
        TimeCompressor.close();
    }
}
