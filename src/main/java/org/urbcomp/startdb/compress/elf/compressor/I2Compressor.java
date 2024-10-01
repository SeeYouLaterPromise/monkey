package org.urbcomp.startdb.compress.elf.compressor;

public interface I2Compressor {
    void manipulate(double v_c, double v_n);
    int getSize();
    byte[] getBytes();
    void close();
    default String getKey() {
        return getClass().getSimpleName();
    }
}
