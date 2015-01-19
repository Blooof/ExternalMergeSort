package ru.bloof.external;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author <a href="mailto:blloof@gmail.com">Oleg Larionov</a>
 */
public class Checker {
    public static void main(String[] args) throws IOException {
        check(new File(ExternalMerger.OUTPUT_FILE_NAME));
    }

    public static void check(File f) throws IOException {
        try (BufferedReader r = new BufferedReader(new FileReader(f), 10 * 1024 * 1024)) {
            String prevLine = null, line;
            while ((line = r.readLine()) != null) {
                if (prevLine == null) {
                    prevLine = line;
                } else if (line.compareTo(prevLine) < 0) {
                    System.out.println("Oops, file is not sorted");
                }
            }
        };
        System.out.println("All right!");
    }
}
