package org.urbcomp.startdb.compress.elf.compressor;

public interface IPairCompressor {
    void addValue(double t, double v);
    int getSizeValue();
    int getSizeTime();
    byte[] getBytes_Value();
    void close_Value();
    byte[] getBytes_Time();
    void close_Time();
    default String getKey() {
        return getClass().getSimpleName();
    }
}
