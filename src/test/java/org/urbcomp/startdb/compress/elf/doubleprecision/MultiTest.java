package org.urbcomp.startdb.compress.elf.doubleprecision;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class MultiTest {
    public static void main(String[] args) throws IOException {
        // Define the base directory path correctly
        String baseDirOfResults = "D:\\yexin\\Documents\\experiment\\";
        String Dir = baseDirOfResults + "MonTC3";

        // Ensure the directory exists or create it if it does not
        createParentDirectoryIfNotExists(Paths.get(Dir));

        // Instantiate your TestCompressor class
        TestCompressor test = new TestCompressor();

        // Loop to perform file operations
        for (int i = 0; i <= 10; i++) {
            System.out.println(i);
            test.testCompressor();

            // Define the source and destination paths correctly without repeating baseDirOfResults
            Path sourcePath = Paths.get("D:\\yexin\\Documents\\elf\\src\\test\\resources\\result\\result.csv");
            Path destinationPath = Paths.get(Dir + "\\RAW\\raw" + i + ".csv");
            Path sourcePath1 = Paths.get("D:\\yexin\\Documents\\elf\\src\\test\\resources\\result\\time-cost.csv");
            Path destinationPath1 = Paths.get(Dir + "\\Method2\\aver-one-" + i + ".csv");
            Path sourcePath2 = Paths.get("D:\\yexin\\Documents\\elf\\src\\test\\resources\\result\\time-cost0.csv");
            Path destinationPath2 = Paths.get(Dir + "\\Method1\\aver-two-" + i + ".csv");

            try {
                // Ensure each destination directory exists
                createParentDirectoryIfNotExists(destinationPath);
                createParentDirectoryIfNotExists(destinationPath1);
                createParentDirectoryIfNotExists(destinationPath2);

                // Copy the files from source to destination
                Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                Files.copy(sourcePath1, destinationPath1, StandardCopyOption.REPLACE_EXISTING);
                Files.copy(sourcePath2, destinationPath2, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("File copied successfully.");
            } catch (IOException e) {
                System.err.println("Error occurred while copying the file.");
                e.printStackTrace();
            }
        }
    }

    // Method to create parent directory if it does not exist
    private static void createParentDirectoryIfNotExists(Path path) throws IOException {
        Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            System.out.println("Created directory: " + parentDir);
        }
    }
}
