package org.urbcomp.startdb.compress.elf.doubleprecision;

import com.github.kutschkem.fpc.FpcCompressor;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.io.compress.brotli.BrotliCodec;
import org.apache.hadoop.hbase.io.compress.lz4.Lz4Codec;
import org.apache.hadoop.hbase.io.compress.xerial.SnappyCodec;
import org.apache.hadoop.hbase.io.compress.xz.LzmaCodec;
import org.apache.hadoop.hbase.io.compress.zstd.ZstdCodec;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.compress.CompressionInputStream;
import org.apache.hadoop.io.compress.CompressionOutputStream;
import org.junit.jupiter.api.Test;
import org.urbcomp.startdb.compress.elf.compressor.*;
import org.urbcomp.startdb.compress.elf.decompressor.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCompressor {
    private static final String FILE_PATH = "src/test/resources/ElfTestData";
//    private static final String FILE_PATH = "src/test/resources/UCR";
//    private static final String[] FILENAMES =
//            {
//                    "/ACSF1_TRAIN.csv",
//                    "/Adiac_TRAIN.csv",
//                    "/AllGestureWiimoteX_TRAIN.csv",
//                    "/AllGestureWiimoteY_TRAIN.csv",
//                    "/AllGestureWiimoteZ_TRAIN.csv",
//                    "/CBF_TRAIN.csv",
//                    "/ChlorineConcentration_TRAIN.csv",
//                    "/Computers_TRAIN.csv",
//                    "/CricketX_TRAIN.csv",
//                    "/CricketY_TRAIN.csv",
//                    "/CricketZ_TRAIN.csv",
//                    "/DistalPhalanxOutlineAgeGroup_TRAIN.csv",
//                    "/DistalPhalanxOutlineCorrect_TRAIN.csv",
//                    "/DistalPhalanxTW_TRAIN.csv",
//                    "/Earthquakes_TRAIN.csv",
//                    "/ECG200_TRAIN.csv",
//                    "/ECG5000_TRAIN.csv",
//                    "/EOGHorizontalSignal_TRAIN.csv",
//                    "/EOGVerticalSignal_TRAIN.csv",
//                    "/EthanolLevel_TRAIN.csv",
//                    "/FaceAll_TRAIN.csv",
//                    "/FaceFour_TRAIN.csv",
//                    "/FacesUCR_TRAIN.csv",
//                    "/FiftyWords_TRAIN.csv",
//                    "/Fish_TRAIN.csv",
//                    "/FordA_TRAIN.csv",
//                    "/FordB_TRAIN.csv",
//                    "/FreezerRegularTrain_TRAIN.csv",
//                    "/GestureMidAirD1_TRAIN.csv",
//                    "/GestureMidAirD2_TRAIN.csv",
//                    "/GestureMidAirD3_TRAIN.csv",
//                    "/GesturePebbleZ1_TRAIN.csv",
//                    "/GesturePebbleZ2_TRAIN.csv",
//                    "/GunPointAgeSpan_TRAIN.csv",
//                    "/GunPointMaleVersusFemale_TRAIN.csv",
//                    "/GunPointOldVersusYoung_TRAIN.csv",
//                    "/Ham_TRAIN.csv",
//                    "/HandOutlines_TRAIN.csv",
//                    "/Haptics_TRAIN.csv",
//                    "/HouseTwenty_TRAIN.csv",
//                    "/InsectWingbeatSound_TRAIN.csv",
//                    "/LargeKitchenAppliances_TRAIN.csv",
//                    "/Lightning2_TRAIN.csv",
//                    "/Lightning7_TRAIN.csv",
//                    "/Mallat_TRAIN.csv",
//                    "/MedicalImages_TRAIN.csv",
//                    "/MelbournePedestrian_TRAIN.csv",
//                    "/MiddlePhalanxOutlineAgeGroup_TRAIN.csv",
//                    "/MiddlePhalanxOutlineCorrect_TRAIN.csv",
//                    "/MiddlePhalanxTW_TRAIN.csv",
//                    "/MixedShapesRegularTrain_TRAIN.csv",
//                    "/MixedShapesSmallTrain_TRAIN.csv",
//                    "/NonInvasiveFetalECGThorax2_TRAIN.csv",
//                    "/OSULeaf_TRAIN.csv",
//                    "/PhalangesOutlinesCorrect_TRAIN.csv",
//                    "/Phoneme_TRAIN.csv",
//                    "/PigAirwayPressure_TRAIN.csv",
//                    "/PigArtPressure_TRAIN.csv",
//                    "/PigCVP_TRAIN.csv",
//                    "/Plane_TRAIN.csv",
//                    "/PowerCons_TRAIN.csv",
//                    "/ProximalPhalanxOutlineAgeGroup_TRAIN.csv",
//                    "/ProximalPhalanxOutlineCorrect_TRAIN.csv",
//                    "/ProximalPhalanxTW_TRAIN.csv",
//                    "/RefrigerationDevices_TRAIN.csv",
//                    "/Rock_TRAIN.csv",
//                    "/ScreenType_TRAIN.csv",
//                    "/SemgHandGenderCh2_TRAIN.csv",
//                    "/SemgHandMovementCh2_TRAIN.csv",
//                    "/SemgHandSubjectCh2_TRAIN.csv",
//                    "/ShapeletSim_TRAIN.csv",
//                    "/ShapesAll_TRAIN.csv",
//                    "/SmallKitchenAppliances_TRAIN.csv",
//                    "/StarLightCurves_TRAIN.csv",
//                    "/Strawberry_TRAIN.csv",
//                    "/SwedishLeaf_TRAIN.csv",
//                    "/SyntheticControl_TRAIN.csv",
//                    "/ToeSegmentation1_TRAIN.csv",
//                    "/TwoPatterns_TRAIN.csv",
//                    "/UWaveGestureLibraryX_TRAIN.csv",
//                    "/UWaveGestureLibraryY_TRAIN.csv",
//                    "/UWaveGestureLibraryZ_TRAIN.csv",
//                    "/Wafer_TRAIN.csv",
//                    "/WordSynonyms_TRAIN.csv",
//                    "/WormsTwoClass_TRAIN.csv",
//                    "/Worms_TRAIN.csv",
//                    "/Yoga_TRAIN.csv",
//                                "/DodgerLoopDay_TRAIN.csv",
//            "/DodgerLoopGame_TRAIN.csv",
//            "/DodgerLoopWeekend_TRAIN.csv",
//        "/init.csv",  // 1  //First run a dataset to ensure the relevant hbase settings of the zstd and snappy compressors
//        "/Air-pressure.csv", // 1
//        "/Air-sensor.csv",   // 1
//        "/Basel-temp.csv",  // 1
//        "/Basel-wind.csv",  // 1
//        "/Bird-migration.csv",  // 1
//        "/Bitcoin-price.csv",   // 1
//        "/Blockchain-tr.csv", // 1
//
//        "/City-temp.csv",  // 1 !
//        "/City-lat.csv",   // 1
//        "/City-lon.csv",   // 1
//        "/Dew-point-temp.csv",  // 1  !
//        "/electric_vehicle_charging.csv",  // 1
//        "/Food-price.csv",   // 1
//
//        "/IR-bio-temp.csv",   // 1
//        "/PM10-dust.csv",  // 1
//        "/SSD-bench.csv",   // 1
//        "/POI-lat.csv",
//
//        "/POI-lon.csv",
//        "/Stocks-DE.csv",   // 1
//        "/Stocks-UK.csv",        // 1
//        "/Stocks-USA.csv",      // 1
//        "/Wind-Speed.csv",      // 1
//            };

        private static final String[] FILENAMES = {
            "/DodgerLoopDay_TRAIN.csv",
            "/DodgerLoopGame_TRAIN.csv",
            "/DodgerLoopWeekend_TRAIN.csv",
//        "/init.csv",  // 1  //First run a dataset to ensure the relevant hbase settings of the zstd and snappy compressors
        "/Air-pressure.csv", // 1
        "/Air-sensor.csv",   // 1
        "/Basel-temp.csv",  // 0
        "/Basel-wind.csv",  // 1
        "/Bird-migration.csv",  // 0
        "/Bitcoin-price.csv",   // 0
        "/Blockchain-tr.csv", // 0
//
        "/City-temp.csv",  // 1 !
        "/City-lat.csv",   // 0
        "/City-lon.csv",   // 0
        "/Dew-point-temp.csv",  // 1  !
        "/electric_vehicle_charging.csv",  // 1
        "/Food-price.csv",   // 0

        "/IR-bio-temp.csv",   // 1
        "/PM10-dust.csv",  // 1
        "/SSD-bench.csv",   // 1
        "/POI-lat.csv",

        "/POI-lon.csv",
        "/Stocks-DE.csv",   // 1
        "/Stocks-UK.csv",        // 1
        "/Stocks-USA.csv",      // 1
        "/Wind-Speed.csv",      // 1
    };
    private static final String STORE_RESULT = "src/test/resources/result/result.csv";

    private static final double TIME_PRECISION = 1000.0;
    List<Map<String, ResultStructure>> allResult = new ArrayList<>();

    @Test
    public void testCompressor() throws IOException {
        Map<String, List<ResultStructure>> accumResults = new HashMap<>();
        for (String filename : FILENAMES) {
//            System.out.println("Test this file: " + filename);
//            testELFCompressor(filename, result);
//            testFPC(filename, result);
//            testSnappy(filename, result);
//            testZstd(filename, result);
//            testLZ4(filename, result);
//            testBrotli(filename, result);
//            testXz(filename, result);
//            for (Map.Entry<String, List<ResultStructure>> kv : result.entrySet()) {
//                Map<String, ResultStructure> r = new HashMap<>();
//                r.put(kv.getKey(), computeAvg(kv.getValue()));
//                allResult.add(r);
//            }
            allResult.add(testXORCompressor(filename, accumResults));
//            if (result.isEmpty()) {
//                System.out.println("The result of the file " + filename +
//                        " is empty because the amount of data is less than one block, and the default is at least 1000.");
//            }

        }
        process(accumResults);
        processAllResultStructures(accumResults);
        storeResult();
    }

//  Method 1
    private void process(Map<String, List<ResultStructure>> result) throws IOException {
        List<Map<String, ResultStructure>> averageResult = new ArrayList<>();
        for (Map.Entry<String, List<ResultStructure>> kv : result.entrySet()) {
//            System.out.println(kv.getKey() + "num : " + kv.getValue().size());
            Map<String, ResultStructure> r = new HashMap<>();
            r.put(kv.getKey(), computeAvg(kv.getValue()));
            averageResult.add(r);
        }
        String filePath = "src/test/resources/result/time-cost0.csv";
        File file = new File(filePath).getParentFile();
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("Create directory failed: " + file);
        }
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write("CompressorName," +
                    "CompressionTime," +
                    "DecompressionTime," +
                    '\n');
            for (Map<String, ResultStructure> r : averageResult) {
                for (ResultStructure ls : r.values()) {
                    String ans = ls.getCompressorName() + ',' + ls.getCompressionTime() + ',' + ls.getDecompressionTime() + ',' + '\n';
                    fileWriter.write(ans);
                }
            }
        }
    }

// Method 2
    private void processAllResultStructures(Map<String, List<ResultStructure>> resultCompressor) throws IOException {
        List<String> AverageResults = new ArrayList<>();
        int numFiles = 0;
        for (Map.Entry<String, List<ResultStructure>> kv : resultCompressor.entrySet()) {
            numFiles = kv.getValue().size();
//            Map<String, ResultStructure> averR = new HashMap<>();
            float numBlock = 0;
            double EncodingDuration = 0;
            double DecodingDuration = 0;
            double compressionRatio = 0;
            for(ResultStructure r: kv.getValue()) {
                numBlock += r.getTotalBlocks();
                EncodingDuration += r.getTotalEncodingDuration();
                DecodingDuration += r.getTotalDecodingDuration();
                compressionRatio += r.getCompressorRatio();
            }
            System.out.println("count Files: " + numFiles);
            System.out.println(kv.getKey() + " : r0: " + compressionRatio);
            System.out.println(kv.getKey() + " : r: " + compressionRatio / numFiles);
            System.out.println(kv.getKey() + " : " + "ED: " + EncodingDuration / numBlock + " DD: " + DecodingDuration / numBlock);
            String result = kv.getKey() + ',' + compressionRatio / numFiles + ',' + EncodingDuration / numBlock + ',' + DecodingDuration / numBlock + ',' +'\n';
//            System.out.println(result);
            AverageResults.add(result);
        }

        String filePath = "src/test/resources/result/time-cost.csv";
        File file = new File(filePath).getParentFile();
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("Create directory failed: " + file);
        }
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write("CompressorName," +
                    "CompressorRatio," +
                    "CompressionTime," +
                    "DecompressionTime," +
                    '\n');
            for(String e: AverageResults) {
                fileWriter.write(e);
            }
        }
    }

    @org.jetbrains.annotations.NotNull
    private Map<String, ResultStructure> testXORCompressor(String fileName, Map<String, List<ResultStructure>> resultAccumulate) throws FileNotFoundException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);

        float totalBlocks = 0;
        double[] values;

        HashMap<String, List<Double>> totalCompressionTime = new HashMap<>();
        HashMap<String, List<Double>> totalDecompressionTime = new HashMap<>();
        HashMap<String, Long> key2TotalSize = new HashMap<>();

        Map<String, ResultStructure> ans = new HashMap<>();

        while ((values = fileReader.nextBlock()) != null) {
            totalBlocks += 1;
            ICompressor[] compressors = new ICompressor[]{
//                new GorillaCompressorOS(),
//                new ElfOnGorillaCompressorOS(),
//                new ChimpCompressor(),
//                new ElfOnChimpCompressor(),
//                new ChimpNCompressor(128),
//                new ElfOnChimpNCompressor(128),
                    new ElfCompressor(),
//                    new my2Compressor(),
//                    new OneOptim(),
//                    new TwoOptim(),
//                    new MonkeyDyn(),
//                    new Monkey(),
            };
            for (int i = 0; i < compressors.length; i++) {

//                System.out.println(fileName + " : " + compressors[i].getKey());

                double encodingDuration;
                double decodingDuration;

                long start = System.nanoTime();
                ICompressor compressor = compressors[i];
                for (double value : values) {
                    compressor.addValue(value);
                }
                compressor.close();
                encodingDuration = System.nanoTime() - start;

                byte[] result = compressor.getBytes();
                IDecompressor[] decompressors = new IDecompressor[]{
//                    new GorillaDecompressorOS(result),
//                    new ElfOnGorillaDecompressorOS(result),
//                    new ChimpDecompressor(result),
//                    new ElfOnChimpDecompressor(result),
//                    new ChimpNDecompressor(result, 128),
//                    new ElfOnChimpNDecompressor(result, 128),
                        new ElfDecompressor(result),
//                        new my2Decompressor(result),
//                        new OneOptimDe(result),
//                        new MonkeyDecompressor(result),
//                        new MonkeyDynDe(result),
//                        new MonkeyDe(result),
                };

                IDecompressor decompressor = decompressors[i];

                start = System.nanoTime();
                List<Double> uncompressedValues = decompressor.decompress();
                decodingDuration = System.nanoTime() - start;

                for (int j = 0; j < values.length; j++) {
                    assertEquals(values[j], uncompressedValues.get(j), "Here is the " + totalBlocks + "-th block, " + (j+1) + "-th value. You can find this value in the file: " + fileName + " ( " + (int)(1000*(totalBlocks-1) + j + 1) + "-th )" + ", Value did not match " + compressor.getKey());
                }

                String key = compressor.getKey();
                if (!totalCompressionTime.containsKey(key)) {
                    totalCompressionTime.put(key, new ArrayList<>());
                    totalDecompressionTime.put(key, new ArrayList<>());
                    key2TotalSize.put(key, 0L);
                }
                totalCompressionTime.get(key).add(encodingDuration / TIME_PRECISION);
                totalDecompressionTime.get(key).add(decodingDuration / TIME_PRECISION);
                key2TotalSize.put(key, compressor.getSize() + key2TotalSize.get(key));
            }
        }

        for (Map.Entry<String, Long> kv : key2TotalSize.entrySet()) {
            String key = kv.getKey();
            Long totalSize = kv.getValue();
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize / (totalBlocks * FileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    totalCompressionTime.get(key),
                    totalDecompressionTime.get(key),
                     totalBlocks
            );
            if (!resultAccumulate.containsKey(key)) {
                resultAccumulate.put(key, new ArrayList<>());
            }
            resultAccumulate.get(key).add(r);
            ans.put(key, r);
        }
        return ans;
    }


    private void testELFCompressor(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws FileNotFoundException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);

        float totalBlocks = 0;
        double[] values;

        HashMap<String, List<Double>> totalCompressionTime = new HashMap<>();
        HashMap<String, List<Double>> totalDecompressionTime = new HashMap<>();
        HashMap<String, Long> key2TotalSize = new HashMap<>();

        while ((values = fileReader.nextBlock()) != null) {
            totalBlocks += 1;
            ICompressor[] compressors = new ICompressor[]{
//                new GorillaCompressorOS(),
//                new ElfOnGorillaCompressorOS(),
//                new ChimpCompressor(),
//                new ElfOnChimpCompressor(),
//                new ChimpNCompressor(128),
//                new ElfOnChimpNCompressor(128),
                    new ElfCompressor(),
//                    new my2Compressor(),
//                    new OneOptim(),
                    new TwoOptim(),
            };
            for (int i = 0; i < compressors.length; i++) {
                double encodingDuration;
                double decodingDuration;

                long start = System.nanoTime();
                ICompressor compressor = compressors[i];
                for (double value : values) {
                    compressor.addValue(value);
                }
                compressor.close();
                encodingDuration = System.nanoTime() - start;

                byte[] result = compressor.getBytes();
                IDecompressor[] decompressors = new IDecompressor[]{
//                    new GorillaDecompressorOS(result),
//                    new ElfOnGorillaDecompressorOS(result),
//                    new ChimpDecompressor(result),
//                    new ElfOnChimpDecompressor(result),
//                    new ChimpNDecompressor(result, 128),
//                    new ElfOnChimpNDecompressor(result, 128),
                        new ElfDecompressor(result),
//                        new my2Decompressor(result),
//                        new OneOptimDe(result),
                        new TwoOptimDe(result),
                };

                IDecompressor decompressor = decompressors[i];

                start = System.nanoTime();
                List<Double> uncompressedValues = decompressor.decompress();
                decodingDuration = System.nanoTime() - start;

                for (int j = 0; j < values.length; j++) {
                    assertEquals(values[j], uncompressedValues.get(j), "Value did not match" + compressor.getKey());
                }

                String key = compressor.getKey();
                if (!totalCompressionTime.containsKey(key)) {
                    totalCompressionTime.put(key, new ArrayList<>());
                    totalDecompressionTime.put(key, new ArrayList<>());
                    key2TotalSize.put(key, 0L);
                }
                totalCompressionTime.get(key).add(encodingDuration / TIME_PRECISION);
                totalDecompressionTime.get(key).add(decodingDuration / TIME_PRECISION);
                key2TotalSize.put(key, compressor.getSize() + key2TotalSize.get(key));
            }
        }

        for (Map.Entry<String, Long> kv : key2TotalSize.entrySet()) {
            String key = kv.getKey();
            Long totalSize = kv.getValue();
            ResultStructure r = new ResultStructure(fileName, key,
                totalSize / (totalBlocks * FileReader.DEFAULT_BLOCK_SIZE * 64.0),
                totalCompressionTime.get(key),
                totalDecompressionTime.get(key),
                    totalBlocks
            );
            if (!resultCompressor.containsKey(key)) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }

    private void testFPC(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws FileNotFoundException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        List<Double> totalCompressionTime = new ArrayList<>();
        List<Double> totalDecompressionTime = new ArrayList<>();

        while ((values = fileReader.nextBlock()) != null) {
            double encodingDuration = 0;
            double decodingDuration = 0;
            FpcCompressor fpc = new FpcCompressor();

            ByteBuffer buffer = ByteBuffer.allocate(FileReader.DEFAULT_BLOCK_SIZE * 10);
            // Compress
            long start = System.nanoTime();
            fpc.compress(buffer, values);
            encodingDuration += System.nanoTime() - start;

            totalSize += buffer.position() * 8L;
            totalBlocks += 1;

            buffer.flip();

            FpcCompressor decompressor = new FpcCompressor();

            double[] dest = new double[FileReader.DEFAULT_BLOCK_SIZE];
            start = System.nanoTime();
            decompressor.decompress(buffer, dest);
            decodingDuration += System.nanoTime() - start;
            assertArrayEquals(dest, values);
            totalCompressionTime.add(encodingDuration / TIME_PRECISION);
            totalDecompressionTime.add(decodingDuration / TIME_PRECISION);
        }
        if (!totalCompressionTime.isEmpty()) {
            String key = "FPC";
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize / (totalBlocks * FileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    totalCompressionTime,
                    totalDecompressionTime, totalBlocks
            );
            if (!resultCompressor.containsKey(key)) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }

    private void testSnappy(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws IOException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        List<Double> totalCompressionTime = new ArrayList<>();
        List<Double> totalDecompressionTime = new ArrayList<>();

        while ((values = fileReader.nextBlock()) != null) {
            double encodingDuration = 0;
            double decodingDuration = 0;

            Configuration conf = HBaseConfiguration.create();
            // ZStandard levels range from 1 to 22.
            // Level 22 might take up to a minute to complete. 3 is the Hadoop default, and will be fast.
            conf.setInt(CommonConfigurationKeys.IO_COMPRESSION_CODEC_ZSTD_LEVEL_KEY, 3);
            SnappyCodec codec = new SnappyCodec();
            codec.setConf(conf);

            ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
            // Compress
            long start = System.nanoTime();
            for (double d : values) {
                bb.putDouble(d);
            }
            byte[] input = bb.array();
            org.apache.hadoop.io.compress.Compressor compressor = codec.createCompressor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CompressionOutputStream out = codec.createOutputStream(baos, compressor);
            out.write(input);
            out.close();
            encodingDuration += System.nanoTime() - start;
            final byte[] compressed = baos.toByteArray();
            totalSize += compressed.length * 8L;
            totalBlocks++;

            final byte[] plain = new byte[input.length];
            org.apache.hadoop.io.compress.Decompressor decompressor = codec.createDecompressor();
            start = System.nanoTime();
            CompressionInputStream in = codec.createInputStream(new ByteArrayInputStream(compressed), decompressor);
            IOUtils.readFully(in, plain, 0, plain.length);
            in.close();
            double[] uncompressed = toDoubleArray(plain);
            decodingDuration += System.nanoTime() - start;
            // Decompressed bytes should equal the original
            for (int i = 0; i < values.length; i++) {
                assertEquals(values[i], uncompressed[i], "Value did not match");
            }
            totalCompressionTime.add(encodingDuration / TIME_PRECISION);
            totalDecompressionTime.add(decodingDuration / TIME_PRECISION);
        }
        if (!totalCompressionTime.isEmpty()) {
            String key = "Snappy";
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize / (totalBlocks * FileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    totalCompressionTime,
                    totalDecompressionTime,
                    totalBlocks
            );
            if (!resultCompressor.containsKey(key)) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }

    private void testZstd(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws IOException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        List<Double> totalCompressionTime = new ArrayList<>();
        List<Double> totalDecompressionTime = new ArrayList<>();

        while ((values = fileReader.nextBlock()) != null) {
            double encodingDuration = 0;
            double decodingDuration = 0;

            Configuration conf = HBaseConfiguration.create();
            // ZStandard levels range from 1 to 22.
            // Level 22 might take up to a minute to complete. 3 is the Hadoop default, and will be fast.
            conf.setInt(CommonConfigurationKeys.IO_COMPRESSION_CODEC_ZSTD_LEVEL_KEY, 3);
            ZstdCodec codec = new ZstdCodec();
            codec.setConf(conf);

            ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
            // Compress
            long start = System.nanoTime();
            for (double d : values) {
                bb.putDouble(d);
            }
            byte[] input = bb.array();
            org.apache.hadoop.io.compress.Compressor compressor = codec.createCompressor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CompressionOutputStream out = codec.createOutputStream(baos, compressor);
            out.write(input);
            out.close();
            encodingDuration += System.nanoTime() - start;
            final byte[] compressed = baos.toByteArray();
            totalSize += compressed.length * 8L;
            totalBlocks++;

            final byte[] plain = new byte[input.length];
            org.apache.hadoop.io.compress.Decompressor decompressor = codec.createDecompressor();
            start = System.nanoTime();
            CompressionInputStream in = codec.createInputStream(new ByteArrayInputStream(compressed), decompressor);
            IOUtils.readFully(in, plain, 0, plain.length);
            in.close();
            double[] uncompressed = toDoubleArray(plain);
            decodingDuration += System.nanoTime() - start;
            // Decompressed bytes should equal the original
            for (int i = 0; i < values.length; i++) {
                assertEquals(values[i], uncompressed[i], "Value did not match");
            }
            totalCompressionTime.add(encodingDuration / TIME_PRECISION);
            totalDecompressionTime.add(decodingDuration / TIME_PRECISION);
        }
        if (!totalCompressionTime.isEmpty()) {
            String key = "Zstd";
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize / (totalBlocks * FileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    totalCompressionTime,
                    totalDecompressionTime, totalBlocks
            );
            if (!resultCompressor.containsKey(key)) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }

    private void testLZ4(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws IOException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        List<Double> totalCompressionTime = new ArrayList<>();
        List<Double> totalDecompressionTime = new ArrayList<>();

        while ((values = fileReader.nextBlock()) != null) {
            double encodingDuration = 0;
            double decodingDuration = 0;

            Lz4Codec codec = new Lz4Codec();

            ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
            // Compress
            long start = System.nanoTime();
            for (double d : values) {
                bb.putDouble(d);
            }
            byte[] input = bb.array();
            org.apache.hadoop.io.compress.Compressor compressor = codec.createCompressor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CompressionOutputStream out = codec.createOutputStream(baos, compressor);
            out.write(input);
            out.close();
            encodingDuration += System.nanoTime() - start;
            final byte[] compressed = baos.toByteArray();
            totalSize += compressed.length * 8L;
            totalBlocks++;

            final byte[] plain = new byte[input.length];
            org.apache.hadoop.io.compress.Decompressor decompressor = codec.createDecompressor();
            start = System.nanoTime();
            CompressionInputStream in = codec.createInputStream(new ByteArrayInputStream(compressed), decompressor);
            IOUtils.readFully(in, plain, 0, plain.length);
            in.close();
            double[] uncompressed = toDoubleArray(plain);
            decodingDuration += System.nanoTime() - start;
            // Decompressed bytes should equal the original
            for (int i = 0; i < values.length; i++) {
                assertEquals(values[i], uncompressed[i], "Value did not match");
            }
            totalCompressionTime.add(encodingDuration / TIME_PRECISION);
            totalDecompressionTime.add(decodingDuration / TIME_PRECISION);
        }
        if (!totalCompressionTime.isEmpty()) {
            String key = "LZ4";
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize / (totalBlocks * FileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    totalCompressionTime,
                    totalDecompressionTime, totalBlocks
            );
            if (!resultCompressor.containsKey(key)) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }

    private void testBrotli(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws IOException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        List<Double> totalCompressionTime = new ArrayList<>();
        List<Double> totalDecompressionTime = new ArrayList<>();

        while ((values = fileReader.nextBlock()) != null) {
            double encodingDuration = 0;
            double decodingDuration = 0;

            BrotliCodec codec = new BrotliCodec();

            ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
            // Compress
            long start = System.nanoTime();
            for (double d : values) {
                bb.putDouble(d);
            }
            byte[] input = bb.array();
            org.apache.hadoop.io.compress.Compressor compressor = codec.createCompressor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CompressionOutputStream out = codec.createOutputStream(baos, compressor);
            out.write(input);
            out.close();
            encodingDuration += System.nanoTime() - start;
            final byte[] compressed = baos.toByteArray();
            totalSize += compressed.length * 8L;
            totalBlocks++;

            final byte[] plain = new byte[input.length];
            org.apache.hadoop.io.compress.Decompressor decompressor = codec.createDecompressor();
            start = System.nanoTime();
            CompressionInputStream in = codec.createInputStream(new ByteArrayInputStream(compressed), decompressor);
            IOUtils.readFully(in, plain, 0, plain.length);
            in.close();
            double[] uncompressed = toDoubleArray(plain);
            decodingDuration += System.nanoTime() - start;
            // Decompressed bytes should equal the original
            for (int i = 0; i < values.length; i++) {
                assertEquals(values[i], uncompressed[i], "Value did not match");
            }
            totalCompressionTime.add(encodingDuration / TIME_PRECISION);
            totalDecompressionTime.add(decodingDuration / TIME_PRECISION);
        }
        if (!totalCompressionTime.isEmpty()) {
            String key = "Brotli";
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize / (totalBlocks * FileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    totalCompressionTime,
                    totalDecompressionTime, totalBlocks
            );
            if (!resultCompressor.containsKey(key)) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }

    private void testXz(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws IOException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        List<Double> totalCompressionTime = new ArrayList<>();
        List<Double> totalDecompressionTime = new ArrayList<>();

        while ((values = fileReader.nextBlock()) != null) {
            double encodingDuration = 0;
            double decodingDuration = 0;

            Configuration conf = new Configuration();
            // LZMA levels range from 1 to 9.
            // Level 9 might take several minutes to complete. 3 is our default. 1 will be fast.
            conf.setInt(LzmaCodec.LZMA_LEVEL_KEY, 3);
            LzmaCodec codec = new LzmaCodec();
            codec.setConf(conf);

            ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
            // Compress
            long start = System.nanoTime();
            for (double d : values) {
                bb.putDouble(d);
            }
            byte[] input = bb.array();
            org.apache.hadoop.io.compress.Compressor compressor = codec.createCompressor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CompressionOutputStream out = codec.createOutputStream(baos, compressor);
            out.write(input);
            out.close();
            encodingDuration += System.nanoTime() - start;
            final byte[] compressed = baos.toByteArray();
            totalSize += compressed.length * 8L;
            totalBlocks++;

            final byte[] plain = new byte[input.length];
            org.apache.hadoop.io.compress.Decompressor decompressor = codec.createDecompressor();
            start = System.nanoTime();
            CompressionInputStream in = codec.createInputStream(new ByteArrayInputStream(compressed), decompressor);
            IOUtils.readFully(in, plain, 0, plain.length);
            in.close();
            double[] uncompressed = toDoubleArray(plain);
            decodingDuration += System.nanoTime() - start;
            // Decompressed bytes should equal the original
            for (int i = 0; i < values.length; i++) {
                assertEquals(values[i], uncompressed[i], "Value did not match");
            }
            totalCompressionTime.add(encodingDuration / TIME_PRECISION);
            totalDecompressionTime.add(decodingDuration / TIME_PRECISION);
        }
        if (!totalCompressionTime.isEmpty()) {
            String key = "Xz";
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize / (totalBlocks * FileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    totalCompressionTime,
                    totalDecompressionTime, totalBlocks
            );
            if (!resultCompressor.containsKey(key)) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }

    private void storeResult() throws IOException {
        String filePath = STORE_RESULT;
        File file = new File(filePath).getParentFile();
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("Create directory failed: " + file);
        }
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write(ResultStructure.getHead());
            for (Map<String, ResultStructure> result : allResult) {
                for (ResultStructure ls : result.values()) {
                    fileWriter.write(ls.toString());
                }
            }
        }
    }

//    private ResultStructure computeAveragePerTask(List<ResultStructure> lr) {
//        return;
//    }


    private ResultStructure computeAvg(List<ResultStructure> lr) {
        int num = lr.size();
        double compressionTime = 0;
        double maxCompressTime = 0;
        double minCompressTime = 0;
        double mediaCompressTime = 0;
        double decompressionTime = 0;
        double maxDecompressTime = 0;
        double minDecompressTime = 0;
        double mediaDecompressTime = 0;
        for (ResultStructure resultStructure : lr) {
            compressionTime += resultStructure.getCompressionTime();
            maxCompressTime += resultStructure.getMaxCompressTime();
            minCompressTime += resultStructure.getMinCompressTime();
            mediaCompressTime += resultStructure.getMediaCompressTime();
            decompressionTime += resultStructure.getDecompressionTime();
            maxDecompressTime += resultStructure.getMaxDecompressTime();
            minDecompressTime += resultStructure.getMinDecompressTime();
            mediaDecompressTime += resultStructure.getMediaDecompressTime();
        }
        return new ResultStructure(lr.get(0).getFilename(),
            lr.get(0).getCompressorName(),
            lr.get(0).getCompressorRatio(),
            compressionTime / num,
            maxCompressTime / num,
            minCompressTime / num,
            mediaCompressTime / num,
            decompressionTime / num,
            maxDecompressTime / num,
            minDecompressTime / num,
            mediaDecompressTime / num
        );
    }

    private static double[] toDoubleArray(byte[] byteArray) {
        int times = Double.SIZE / Byte.SIZE;
        double[] doubles = new double[byteArray.length / times];
        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = ByteBuffer.wrap(byteArray, i * times, times).getDouble();
        }
        return doubles;
    }
}
