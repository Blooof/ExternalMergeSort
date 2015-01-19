package ru.bloof.external;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.PriorityQueue;

import static java.lang.String.format;

public class ExternalMerger {
    public static final String INPUT_FILE_NAME = "input.txt";
    public static final String OUTPUT_FILE_NAME = "output.txt";
    public static final String TMP_SORT_FILE_PATTERN = "tmp/tmp_step%d_%d.txt";
    public static final double FREE_MEMORY_MULTIPLIER = 0.85;
    public static final int MAX_MERGE_FILES = 10;
    public static final int RUN_COUNT = 1;
    public static int MAX_SIZE_BYTES;

    public static void main(String[] args) throws Exception {
        MAX_SIZE_BYTES = (int) (Runtime.getRuntime().maxMemory() * FREE_MEMORY_MULTIPLIER);
        System.out.println("Max used memory " + MAX_SIZE_BYTES);
        long totalTime = 0;
        createTmpDir();
        for (int i = 1; i <= RUN_COUNT; i++) {
            Calendar startTimeCalendar = Calendar.getInstance();
            long startTimeMillis = startTimeCalendar.getTimeInMillis();
            print(format("Sorting %d. Start time %s", i, startTimeCalendar.getTime()));

            sort();

            Calendar endTimeCalendar = Calendar.getInstance();
            long runTimeMillis = endTimeCalendar.getTimeInMillis() - startTimeMillis;
            print(format("End time %s, run time %d", endTimeCalendar.getTime(), runTimeMillis));
            totalTime += runTimeMillis;
        }
        print("Average time " + (totalTime / RUN_COUNT));
    }

    private static void print(String s) {
        System.out.println(s);
        System.out.flush();
    }

    private static void createTmpDir() throws IOException {
        try {
            Files.createDirectory(Paths.get("tmp/"));
        } catch (FileAlreadyExistsException ignored) {
        }
    }

    private static void sort() throws IOException {
        int outFilesCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE_NAME), (int) (0.1 * MAX_SIZE_BYTES))) {
            print("Reading input file");
            String line;
            long currentSize = 0;
            List<String> stringsToSort = new ArrayList<>(5000);
            while ((line = reader.readLine()) != null) {
                stringsToSort.add(line);
                currentSize += calcStringSizeBytes(line);
                if (currentSize > 0.8 * MAX_SIZE_BYTES) {
                    outFilesCount = sortAndPrintStrings(outFilesCount, stringsToSort);
                    stringsToSort.clear();
                    currentSize = 0;
                    System.gc();
                }
            }
            if (!stringsToSort.isEmpty()) {
                outFilesCount = sortAndPrintStrings(outFilesCount, stringsToSort);
            }
            stringsToSort = null;
        }
        System.gc();

        int step = 1;
        while (outFilesCount > 1) {
            print(format("Merging step %d, in files count %d", step, outFilesCount));
            outFilesCount = mergeStep(outFilesCount, step);
            print(format("Merging step %d, out files count %d", step, outFilesCount));
            step++;
        }
        if (outFilesCount == 1) {
            File tmpOutput = new File(format(TMP_SORT_FILE_PATTERN, step - 1, 0));
            File output = new File(OUTPUT_FILE_NAME);
            output.delete();
            if (!tmpOutput.renameTo(output)) {
                System.err.println("Cannot rename output file");
            }
        }
    }

    private static int calcStringSizeBytes(String line) {
        return line.length() * 2 + 32;
    }

    private static int sortAndPrintStrings(int outFilesCount, List<String> stringsToSort) throws FileNotFoundException {
        print(format("Sorting %d file with %d strings", outFilesCount + 1, stringsToSort.size()));
        stringsToSort.sort(String::compareTo);
        try (PrintWriter outWriter = new PrintWriter(format(TMP_SORT_FILE_PATTERN, 0, ++outFilesCount - 1))) {
            stringsToSort.forEach(outWriter::println);
        }
        return outFilesCount;
    }

    private static int mergeStep(int outFilesCount, int step) throws IOException {
        int outFilesAfterStep = outFilesCount / MAX_MERGE_FILES;
        if (outFilesCount % MAX_MERGE_FILES != 0) {
            outFilesAfterStep++;
        }
        for (int i = 0; i < outFilesAfterStep; i++) {
            try (PrintWriter pw = new PrintWriter(format(TMP_SORT_FILE_PATTERN, step, i))) {
                int firstFileNum = i * MAX_MERGE_FILES;
                int lastFileNum = Math.min(outFilesCount - 1, (i + 1) * MAX_MERGE_FILES - 1);
                int count = lastFileNum - firstFileNum + 1;
                print(format("Merging %d files", count));
                PriorityQueue<FileIterator> heap = new PriorityQueue<>();
                for (int j = firstFileNum; j <= lastFileNum; j++) {
                    heap.add(new FileIterator(new BufferedReader(new FileReader(format(TMP_SORT_FILE_PATTERN, step - 1, j)),
                            MAX_SIZE_BYTES / (3 * count))));
                }

                while (!heap.isEmpty()) {
                    FileIterator min = heap.poll();
                    pw.println(min.current());
                    if (min.next() == null) {
                        min.close();
                    } else {
                        heap.add(min);
                    }
                }
            }
            System.gc();
        }
        return outFilesAfterStep;
    }

    public static class FileIterator implements Comparable<FileIterator> {
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

        @Override
        public int compareTo(FileIterator o) {
            return current.compareTo(o.current);
        }
    }
}