package org.urbcomp.startdb.compress.elf;

public class BitReader {
    private final long bits; // the long value that holds the binary data
    private int position; // current position in the bit stream

    // Constructor initializes the bit reader with the given long value
    public BitReader(long bits) {
        this.bits = bits;
        this.position = 0;
    }

    /**
     * Reads the specified number of bits from the current position.
     *
     * @param len the number of bits to read (1 to 64)
     * @return the value of the read bits as a long, or -1 if the read exceeds the limit
     */
    public long readBits(int len) {
        // Check if reading the bits would exceed the available length (64 bits)
        if (len <= 0 || len > 64 || position + len > 64) {
            return -1; // Return -1 or throw an exception if the read is invalid
        }

        // Calculate the mask to extract the required bits
        long mask = (1L << len) - 1;

        // Shift the bits to the right position and apply the mask
        long result = (bits >> (64 - position - len)) & mask;

        // Update the current position
        position += len;

        return result;
    }

    // Resets the reader to start from the beginning
    public void reset() {
        position = 0;
    }

    public static void main(String[] args) {
        BitReader reader = new BitReader(0b1101011001111000101010101110001110101110010101010010110111010101L);

//        System.out.println(reader.readBits(4));  // Example: Read 4 bits
//        Elf64Utils.showBinaryString(reader.readBits(4));
        System.out.println(Long.toBinaryString(reader.readBits(6)));  // Example: Read 6 bits
        System.out.println(Long.toBinaryString(reader.readBits(8)));  // Example: Read 8 bits
        reader.reset(); // Reset the position if you want to start over
        System.out.println(Long.toBinaryString(reader.readBits(3)));  // Example: Read 3 bits after reset
    }
}

