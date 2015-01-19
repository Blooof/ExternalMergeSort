package ru.bloof.external;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static final String INPUT_FILE_NAME = "input.txt";
    public static final String OUTPUT_FILE_NAME = "output.txt";
    public static final String TMP_SORT_FILE_PATTERN = "tmp/tmp_step%d_%d.txt";
    public static final int MAX_SIZE_BYTES = 100 * 1024 * 1024;
    public static final int MAX_MERGE_FILES = 10;
    public static final int RUN_COUNT = 1;

    public static void main(String[] args) throws Exception {
        long totalTime = 0;
        for (int i = 1; i <= RUN_COUNT; i++) {
            System.out.println("Sorting " + i);
            long startTimeMillis = System.currentTimeMillis();
            sort();
            long runTimeMillis = System.currentTimeMillis() - startTimeMillis;
            System.out.println("Run time " + runTimeMillis);
            totalTime += runTimeMillis;
        }
        System.out.println("Average time " + (totalTime / RUN_COUNT));
    }

    private static void sort() throws IOException {
        int outFilesCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE_NAME), MAX_SIZE_BYTES / 10)) {
            String line;
            long currentSize = 0;
            List<String> stringsToSort = null;
            while ((line = reader.readLine()) != null) {
                if (stringsToSort == null) {
                    stringsToSort = new ArrayList<>();
                    currentSize = 0;
                }
                stringsToSort.add(line);
                currentSize += line.getBytes().length;
                if (currentSize > MAX_SIZE_BYTES) {
                    stringsToSort.sort(String::compareTo);
                    try (PrintWriter outWriter = new PrintWriter(String.format(TMP_SORT_FILE_PATTERN, 0, ++outFilesCount - 1));) {
                        for (String s : stringsToSort) {
                            outWriter.println(s);
                        }
                    }
                    stringsToSort = null;
                }
            }
            if (stringsToSort != null) {
                stringsToSort.sort(String::compareTo);
                try (PrintWriter outWriter = new PrintWriter(String.format(TMP_SORT_FILE_PATTERN, 0, ++outFilesCount - 1));) {
                    for (String s : stringsToSort) {
                        outWriter.println(s);
                    }
                }
            }
        }
        int step = 1;
        while (outFilesCount > 1) {
            outFilesCount = mergeStep(outFilesCount, step);
            step++;
        }
        if (outFilesCount == 1) {
            File output = new File(String.format(TMP_SORT_FILE_PATTERN, step - 1, 0));
            if (!output.renameTo(new File(OUTPUT_FILE_NAME))) {
                System.err.println("Cannot rename output file");
            }
        }
    }

    private static int mergeStep(int outFilesCount, int step) throws IOException {
        int outFilesAfterStep = outFilesCount / MAX_MERGE_FILES;
        if (outFilesCount % MAX_MERGE_FILES != 0) {
            outFilesAfterStep++;
        }
        for (int i = 0; i < outFilesAfterStep; i++) {
            try (PrintWriter pw = new PrintWriter(String.format(TMP_SORT_FILE_PATTERN, step, i))) {
                int firstFileNum = i * MAX_MERGE_FILES;
                int lastFileNum = Math.min(outFilesCount - 1, (i + 1) * MAX_MERGE_FILES - 1);
                FileIterator incomings[] = new FileIterator[lastFileNum - firstFileNum + 1];
                for (int j = firstFileNum; j <= lastFileNum; j++) {
                    incomings[j - firstFileNum] = new FileIterator(
                            new BufferedReader(new FileReader(String.format(TMP_SORT_FILE_PATTERN, step - 1, j)),
                                    MAX_SIZE_BYTES / (2 * incomings.length)));
                }
                while (true) {
                    FileIterator min = null;
                    for (FileIterator cur : incomings) {
                        if (cur.current() != null) {
                            if (min == null) {
                                min = cur;
                            } else {
                                if (min.current().compareTo(cur.current()) > 0) {
                                    min = cur;
                                }
                            }
                        }
                    }
                    if (min == null) {
                        break;
                    }
                    pw.println(min.current());
                    min.next();
                }
                for (FileIterator fi : incomings) {
                    fi.close();
                }
            }
        }
        return outFilesAfterStep;
    }

    public static class FileIterator {
        BufferedReader reader;
        String current;

        FileIterator(BufferedReader reader) throws IOException {
            this.reader = reader;
            next();
        }

        String current() {
            return current;
        }

        String next() throws IOException {
            current = reader.readLine();
            return current;
        }

        void close() throws IOException {
            reader.close();
        }
    }
}
