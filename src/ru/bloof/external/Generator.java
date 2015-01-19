package ru.bloof.external;

import java.io.PrintWriter;
import java.util.Random;

/**
 * @author <a href="mailto:oleg.larionov@odnoklassniki.ru">Oleg Larionov</a>
 */
public class Generator {
    public static final int MIN_LENGTH = 100;
    public static final int MAX_LENGTH = 10000;
    public static final int STRING_COUNT = 1000000;

    public static void main(String[] args) throws Exception {
        Random rnd = new Random();
        int minCharNum = (int) 'A';
        int maxCharNum = (int) 'z';
        try (PrintWriter pw = new PrintWriter(Main.INPUT_FILE_NAME)) {
            for (int i = 0; i < STRING_COUNT; i++) {
                int length = rnd.nextInt(MAX_LENGTH - MIN_LENGTH) + MIN_LENGTH;
                StringBuilder sb = new StringBuilder(length);
                for (int j = 0; j < length; j++) {
                    int charNum = rnd.nextInt(maxCharNum - minCharNum + 1) + minCharNum;
                    sb.append((char) charNum);
                }
                pw.println(sb.toString());
            }
        }
    }
}
