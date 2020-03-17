/*
 ***************************************************************************************************
 * This java class is a free software; you can redistribute it and/or modify it under no terms
 * as published by myself; either at this version or any later version.
 *
 * This java class is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.
 *
 * You will not receive any copy of any license along with this java class; if you do, you should
 * question where that license came from.
 *
 * Created by: WIN ROEDILY (roedilywinner@gmail.com)
 ***************************************************************************************************
 */

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.mfcc.MFCC;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.exceptions.HDF5LibraryException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class Feature_Extractor {

    private static final String URBANSOUND_PATH = "D:\\NoiseClassifier\\UrbanSound8K\\audio\\fold";
    private static final String FSD_TRAIN_PATH = "D:\\NoiseClassifier\\FSD\\fsd_train\\";
    private static final String FSD_TEST_PATH = "D:\\NoiseClassifier\\FSD\\fsd_test\\";

    private static final String CSV_URBANSOUND = "csv/UrbanSound8K.csv";
    private static final String CSV_FSD_TRAIN = "csv/metadata_fsd_train.csv";
    private static final String CSV_FSD_TEST = "csv/metadata_fsd_test.csv";

    private static final int RANK_DATA = 3;
    private static final int RANK_LABEL = 1;
    private static final int DIM_X = 612;
    private static final int DIM_X_SMALL = 102;
    private static final int DIM_Y = 40;
    private static final int DIM_Z_TRAIN = 3307;
    private static final int DIM_Z_TEST = 499;
    private static final String FILENAME_TRAIN = "h5\\features_train_" + DIM_X + ".h5";
    private static final String FILENAME_TEST = "h5\\features_test_" + DIM_X + ".h5";
    private static final String DATASET_DATA = "data";
    private static final String DATASET_LABEL = "label";

    private static final int sampleRate = 8000;
    private static final int size = 400;
    private static final int overlap = 322;
    private static final int cepstrum = DIM_Y;
    private static final int melFilter = cepstrum;
    private static final float lowerFreq = 0f;
    private static final float upperFreq = ((float) sampleRate) / 2f;
    private static AudioDispatcher audioDispatcher = null;

    private static float[][][] trainData = new float[DIM_Z_TRAIN][DIM_X][DIM_Y];
    private static float[][][] testData = new float[DIM_Z_TEST][DIM_X][DIM_Y];
    private static int[] trainLabel = new int[DIM_Z_TRAIN];
    private static int[] testLabel = new int[DIM_Z_TEST];
    private static int trainCount = 0;
    private static int testCount = 0;
    private static int frameCount = 0;
    private static float[][] mfccs = new float[DIM_X][DIM_Y];

    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        int choice;
        System.out.println("================== MFCC EXTRACTOR ==================");
        for (int i = 0; i < 3; i++) {
            System.out.println("\nThis program provided two sizes for the extraction size:");
            System.out.println("\t1. 612 frames x 40 MFCCs");
            System.out.println("\t2. 102 frames x 40 MFCCs");
            System.out.print("Your choice: ");
            choice = scanner.nextInt();
            if (choice < 3) {
                for (int j = 1; j < 4; j++)
                    csvReader(i, choice);
                writeToH5();
            } else {
                System.out.println("ERROR: Wrong input");
            }
        }
    }

    /**
     * Read CSV File from UrbanSound8K and Freesound. Unlike Freesound dataset, UrbanSound8K doesn't divide it's dataset
     * into TRAIN and TEST. Therefore, we choose which folder will we use as a TEST data. In this case, I choose folder 10.
     *
     * @param code determine which dataset to read
     */
    private static void csvReader(int code, int choice) {
        CSVReader csvReader;
        String[] line;
        try {
            switch (code) {
                case 1:
                    System.out.println("\n============= EXTRACTION START =============");
                    System.out.println("Extracting from: " + CSV_URBANSOUND);

                    csvReader = new CSVReader(new FileReader(CSV_URBANSOUND));
                    csvReader.readNext();
                    while ((line = csvReader.readNext()) != null) {
                        if (line[5].contentEquals("10"))
                            arrayHandler(false, URBANSOUND_PATH + line[5] + "\\" + line[0], line[6], choice);
                        else
                            arrayHandler(true, URBANSOUND_PATH + line[5] + "\\" + line[0], line[6], choice);
                    }

                    System.out.println("\n============= EXTRACTION DONE =============");
                    System.out.println("Train count: " + trainCount + "\tTest count: " + testCount);
                    break;

                case 2:
                    System.out.println("\n============= EXTRACTION START =============");
                    System.out.println("Extracting from: " + CSV_FSD_TRAIN);

                    csvReader = new CSVReader(new FileReader(CSV_FSD_TRAIN));
                    csvReader.readNext();
                    while ((line = csvReader.readNext()) != null)
                        arrayHandler(true, FSD_TRAIN_PATH + line[0], line[2], choice);

                    System.out.println("\n============= EXTRACTION DONE =============");
                    System.out.println("Train count: " + trainCount);
                    break;

                case 3:
                    System.out.println("\n============= EXTRACTION START =============");
                    System.out.println("Extracting from: " + CSV_FSD_TEST);

                    csvReader = new CSVReader(new FileReader(CSV_FSD_TEST));
                    csvReader.readNext();
                    while ((line = csvReader.readNext()) != null)
                        arrayHandler(false, FSD_TEST_PATH + line[0], line[2], choice);

                    System.out.println("\n============= EXTRACTION DONE =============");
                    System.out.println("Test count: " + testCount);
                    break;

                default:
                    System.out.println("ERROR: Invalid code");
                    break;
            }
        } catch (FileNotFoundException e) {
            System.out.println("ERROR: CSV file is not found");
            e.printStackTrace();
        } catch (CsvValidationException e) {
            System.out.println("ERROR: CSVReader.readNext()");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("ERROR: Failed to read CSV file");
            e.printStackTrace();
        }
    }

    /**
     * Array Handler will deal with copying array content from MFCCs extraction process to TRAIN/TEST data array.
     * This method also handle the class ID extraction from the dataset.
     *
     * @param train    determine whether the dataset is for TRAIN or TEST
     * @param filename holds the dataset path
     * @param label    holds the class ID extracted from the dataset
     */
    private static void arrayHandler(boolean train, String filename, String label, int choice) {
        float[][] temp = extractMFCC(filename, choice);
        if (!train) {
            for (int i = 0; i < DIM_X; i++)
                System.arraycopy(temp[i], 0, testData[testCount][i], 0, DIM_Y);
            testLabel[testCount] = Integer.parseInt(label);
            testCount++;
        } else {
            for (int i = 0; i < DIM_X; i++)
                System.arraycopy(temp[i], 0, trainData[trainCount][i], 0, DIM_Y);
            trainLabel[trainCount] = Integer.parseInt(label);
            trainCount++;
        }

        killDispatcher();
    }

    /**
     * Extract MFCC will execute the features (MFCCs) extraction utilizing TarsosDSP library. Though zero-padding is not
     * necessary to do in Java, I just wanna feel safe by doing this anyway.
     *
     * @param filePath holds the full audio file path including the file extension
     * @return the collective extracted MFCCs; saved in a two-dimensional array
     */
    private static float[][] extractMFCC(String filePath, int choice) {
        // Zero padding
        for (float[] row : mfccs)
            Arrays.fill(row, (float) 0.0);

        // Select frame size based on the user input
        int frameLimit = 0;
        if (choice == 1) {
            frameLimit = DIM_X;
        } else if (choice == 2) {
            frameLimit = DIM_X_SMALL;
        }
        int finalFrameLimit = frameLimit;

        audioDispatcher = AudioDispatcherFactory.fromPipe(filePath, sampleRate, size, overlap);
        final MFCC mfcc = new MFCC(size, sampleRate, cepstrum, melFilter, lowerFreq, upperFreq);
        audioDispatcher.addAudioProcessor(mfcc);
        audioDispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                if (frameCount < finalFrameLimit) {
                    mfccs[frameCount] = mfcc.getMFCC();
                    frameCount++;
                    return true;
                } else
                    return false;
            }

            @Override
            public void processingFinished() {
                frameCount = 0;
                //System.out.println("Features extracted from\t" + filePath);
            }
        });
        audioDispatcher.run();

        return mfccs.clone();
    }

    /**
     * Write to H5 handles the HDF5 creating, writing and closing function of .h5 file.
     */
    private static void writeToH5() {
        long[] fileID = {-1, -1};

        long[] fileSpaceTrainID;
        long[] fileSpaceTestID;

        long[] datasetTrainID;
        long[] datasetTestID;

        long[] dimsDataTrain = {DIM_Z_TRAIN, DIM_X, DIM_Y};
        long[] dimsDataTest = {DIM_Z_TEST, DIM_X, DIM_Y};
        long[] dimsLabelTrain = {DIM_Z_TRAIN};
        long[] dimsLabelTest = {DIM_Z_TEST};

        System.out.println("\n\n=============== H5 CREATION ===============");
        System.out.println("Creating h5 file:");
        fileID[0] = createFile(FILENAME_TRAIN);
        fileID[1] = createFile(FILENAME_TEST);

        System.out.println("\nCreating Data Space:");
        fileSpaceTrainID = createDataSpace(dimsDataTrain, dimsLabelTrain, FILENAME_TRAIN);
        fileSpaceTestID = createDataSpace(dimsDataTest, dimsLabelTest, FILENAME_TEST);

        System.out.println("\nCreating Dataset:");
        datasetTrainID = createDataset(fileID[0], fileSpaceTrainID[0], fileSpaceTrainID[1], FILENAME_TRAIN);
        datasetTestID = createDataset(fileID[1], fileSpaceTestID[0], fileSpaceTestID[1], FILENAME_TEST);

        System.out.println("\n\n=============== H5 DATA WRITING ===============");
        System.out.println("Writing to dataset:");
        writeData(datasetTrainID, trainData, trainLabel, FILENAME_TRAIN);
        writeData(datasetTestID, testData, testLabel, FILENAME_TEST);

        System.out.println("\n\n=============== H5 CLOSING ===============");
        System.out.println("Closing H5:");
        closeH5(datasetTrainID, fileSpaceTrainID, fileID[0], FILENAME_TRAIN);
        closeH5(datasetTestID, fileSpaceTestID, fileID[1], FILENAME_TEST);
    }

    /**
     * Create File method handles the .h5 file creation by creating two files: TRAIN and TEST.
     *
     * @param filename holds the path and .h5 file name to create
     * @return the file ID of creation process (not -1)
     */
    private static long createFile(String filename) {
        long fileID = -1;
        try {
            fileID = H5.H5Fcreate(filename, HDF5Constants.H5F_ACC_TRUNC,
                    HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
            System.out.println("\tFile created: " + filename);
        } catch (HDF5LibraryException e) {
            System.out.println("ERROR: Failed to create H5 file");
            e.printStackTrace();
        }
        return fileID;
    }

    /**
     * Create data space to holds dataset for data and label in both .h5 files (TRAIN and TEST).
     *
     * @param dims_data  determine the dimensionality of data array
     * @param dims_label determine the dimensionality of label array
     * @param filename   used only for logging information regarding for which .h5 file we create the data space
     * @return an array holding the return value of the data space creation process
     */
    private static long[] createDataSpace(long[] dims_data, long[] dims_label, String filename) {
        long fileSpaceDataID = -1;
        long fileSpaceLabelID = -1;
        try {
            fileSpaceDataID = H5.H5Screate_simple(Feature_Extractor.RANK_DATA, dims_data, null);
            System.out.println("\tData Space created: " + filename + " (DATA)");
            fileSpaceLabelID = H5.H5Screate_simple(Feature_Extractor.RANK_LABEL, dims_label, null);
            System.out.println("\tData Space created: " + filename + " (LABEL)");
        } catch (HDF5Exception e) {
            System.out.println("ERROR: Failed to create Data Space");
            e.printStackTrace();
        }
        return new long[]{fileSpaceDataID, fileSpaceLabelID};
    }

    /**
     * Create dataset according to the created data space.
     *
     * @param fileID           holds the return value of the file we created
     * @param fileSpaceDataID  holds the return value of the data space we created (DATA)
     * @param fileSpaceLabelID holds the return value of the data space we created (LABEL)
     * @param filename         used only for logging information regarding for which .h5 file we create the data space
     * @return an array holding the return value of the dataset creation process
     */
    private static long[] createDataset(long fileID, long fileSpaceDataID, long fileSpaceLabelID, String filename) {
        long datasetDataID = -1;
        long datasetLabelID = -1;
        if ((fileID >= 0) && (fileSpaceDataID >= 0) && (fileSpaceLabelID >= 0)) {
            try {
                datasetDataID = H5.H5Dcreate(fileID, DATASET_DATA, HDF5Constants.H5T_IEEE_F32LE,
                        fileSpaceDataID, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
                System.out.println("\tDataset created: " + filename + " (DATA)" + datasetDataID);
                datasetLabelID = H5.H5Dcreate(fileID, DATASET_LABEL, HDF5Constants.H5T_STD_I32LE,
                        fileSpaceLabelID, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
                System.out.println("\tDataset created: " + filename + " (LABEL)" + datasetLabelID);
            } catch (HDF5LibraryException e) {
                System.out.println("ERROR: Failed to create Dataset\t");
                e.printStackTrace();
            }
        }
        return new long[]{datasetDataID, datasetLabelID};
    }

    /**
     * Write data method handles the writing process of the extracted MFCCs and labels to the created dataset
     *
     * @param datasetID holds the return value of the dataset we created
     * @param data      holds the extracted MFCCs to write to dataset
     * @param label     holds the extracted labels to write to dataset
     * @param filename  used only for logging information regarding for which .h5 file we create the data space
     */
    private static void writeData(long[] datasetID, float[][][] data, int[] label, String filename) {
        try {
            if (datasetID[0] >= 0 && datasetID[1] >= 0) {
                H5.H5Dwrite(datasetID[0], HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL,
                        HDF5Constants.H5P_DEFAULT, data);
                System.out.println("\tData written to: " + filename + " (DATA)");
                H5.H5Dwrite(datasetID[1], HDF5Constants.H5T_NATIVE_INT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL,
                        HDF5Constants.H5P_DEFAULT, label);
                System.out.println("\tData written to: " + filename + " (LABEL)");
            }

        } catch (HDF5LibraryException e) {
            System.out.println("ERROR: Failed to write to dataset -> LibraryException");
            e.printStackTrace();
        } catch (HDF5Exception e) {
            System.out.println("ERROR: Failed to write to dataest");
            e.printStackTrace();
        }
    }

    /**
     * Close the HDF5 process after finished writing to .h5
     *
     * @param datasetID   holds the return value of the dataset we created
     * @param fileSpaceID holds the return value of the data space we created
     * @param fileID      holds the return value of the file we created
     * @param filename    used only for logging information regarding for which .h5 file we create the data space
     */
    private static void closeH5(long[] datasetID, long[] fileSpaceID, long fileID, String filename) {
        try {
            if (datasetID[0] >= 0 && datasetID[1] >= 0) {
                H5.H5Dclose(datasetID[0]);
                System.out.println("\tDataset closed: " + filename + " (DATA)");
                H5.H5Dclose(datasetID[1]);
                System.out.println("\tDataset closed: " + filename + " (LABEL)");
            }
            if (fileSpaceID[0] >= 0 && fileSpaceID[1] >= 0) {
                H5.H5Sclose(fileSpaceID[0]);
                System.out.println("\tData Space closed: " + filename + " (DATA)");
                H5.H5Sclose(fileSpaceID[1]);
                System.out.println("\tData Space closed: " + filename + " (LABEL)");
            }
            if (fileID >= 0) {
                H5.H5Fclose(fileID);
                System.out.println("\tFile closed: " + filename + "\n");
            }
        } catch (HDF5LibraryException e) {
            System.out.println("ERROR: Failed to close H5 properties");
            e.printStackTrace();
        }
    }

    /**
     * Stop and close the AudioDispatcher after use
     */
    private static void killDispatcher() {
        if (!audioDispatcher.isStopped())
            audioDispatcher.stop();
        audioDispatcher = null;
    }
}
