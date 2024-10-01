package org.urbcomp.startdb.compress.elf;

public class BitUtils {

    /**
     * Concatenates two long bit sequences, placing the left input in front of the right input.
     *
     * @param left  the left input bits (placed at the front)
     * @param right the right input bits
     * @return the concatenated bits as a long value
     */
    public static long concatenateBits(long left, long right) {
        // Calculate the number of bits in the right input by counting significant bits
        int rightLen = Long.SIZE - Long.numberOfLeadingZeros(right);

        // Shift the left bits left to make space for the right bits and concatenate
        return (left << rightLen) | right;
    }

    public static void main(String[] args) {
        // Example usage
        long leftBits = 0b1011;  // binary: 1011
        long rightBits = 0b110;  // binary: 110

        // Concatenate left and right bits
        long result = concatenateBits(leftBits, rightBits);

        // Output the result
        System.out.println(Long.toBinaryString(result)); // Should print: 1011110
    }
}
