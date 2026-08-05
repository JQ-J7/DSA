package utility;

import java.util.Scanner;

/**
 * Shared Scanner utility — ensures only ONE Scanner wraps System.in
 * across the entire application, preventing stdin consumption conflicts.
 *
 * @author Mun Jun How
 */
public class ScannerUtil {
    private static final Scanner INSTANCE = new Scanner(System.in);

    private ScannerUtil() {}

    public static Scanner getScanner() {
        return INSTANCE;
    }
}
