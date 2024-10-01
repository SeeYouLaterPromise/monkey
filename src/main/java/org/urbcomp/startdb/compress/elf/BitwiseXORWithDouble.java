package org.urbcomp.startdb.compress.elf;

import org.urbcomp.startdb.compress.elf.utils.mon64;

import java.util.ArrayList;
import java.util.List;

public class BitwiseXORWithDouble {

    /**
     * Perform bitwise XOR on a double value with bits obtained from a bit stream.
     * Prints each XORed bit during the operation.
     *
     * @param initialValue the initial double value to XOR with
     * @param bitStream    the array of bits to XOR with the double value
     * @return the resulting double after XOR operations
     */
    public static double xorDoubleWithBitStream(double initialValue, int[] bitStream) {
        int bias = 10;
        // Convert the double to a long bit representation
        long doubleBits = Double.doubleToRawLongBits(initialValue);

        // List to store the current bit after each XOR operation
        List<Integer> currentBits = new ArrayList<>();

        // Perform XOR with each bit directly, and print the resulting bit
        for (int i = 0; i < bitStream.length; i++) {
            // XOR each bit from the bit stream with the corresponding bit in doubleBits
            doubleBits ^= ((long) bitStream[i] << (63 - i - bias));

            // Extract the current bit at the XORed position
            int currentBit = (int) ((doubleBits >> (63 - i - bias)) & 1);
            currentBits.add(currentBit); // Store the current XORed bit

            // Print the current bit after XOR
            System.out.println("Current XORed Bit at position " + (63 - i - bias) + ": " + currentBit);
        }

        // Convert back to double after completing the XOR operations
        double finalResult = Double.longBitsToDouble(doubleBits);

        // Print all XORed bits
        System.out.println("All XORed Bits: " + currentBits);

        return finalResult;
    }

    public static void main(String[] args) {
        // Example initial double value
        double initialValue = 3.14;
        mon64.showBinaryString(Double.doubleToLongBits(initialValue));
        // Example bit stream
        int[] bitStream = {1, 0, 1, 1, 0, 1, 0, 0, 1};

        // Perform XOR operation on the double with the bit stream
        double result = xorDoubleWithBitStream(initialValue, bitStream);
        mon64.showBinaryString(Double.doubleToLongBits(result));
        // Print the final result after XOR
        System.out.println("Final Result after XOR: " + result);
    }
}
