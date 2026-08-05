package boundary;

import java.util.Scanner;

/**
 * Console Boundary UI class for the Front-Desk Service subsystem.
 * Handles all console display and user input following the ECB pattern.
 *
 * @author Mun Jun How
 */
public class FrontDeskUI {

    private static Scanner scanner = new Scanner(System.in);

    public FrontDeskUI() {
    }

    // =========================================================================
    // MAIN MENU
    // =========================================================================

    public int getMenuChoice() {
        System.out.println("\n==========================================================================");
        System.out.println("          TARUMT RESORTS - FRONT-DESK SERVICE & GUEST INQUIRIES");
        System.out.println("==========================================================================");
        System.out.println(" --- [ INQUIRY & SEARCH SERVICES ] ---");
        System.out.println(" [1] Guest Lookup by Confirmation Number (8-digit)");
        System.out.println(" [2] Search Guest by Name / IC Number");
        System.out.println(" [3] Check Room Availability by Type");
        System.out.println(" [4] Billing & Folio Inquiry");
        System.out.println();
        System.out.println(" --- [ FRONT-DESK RECORD MANAGEMENT ] ---");
        System.out.println(" [5] Check-In New Guest (Add Reservation)");
        System.out.println(" [6] Update Guest Charges / Extend Stay");
        System.out.println(" [7] Check-Out Guest (Remove & Archive)");
        System.out.println();
        System.out.println(" --- [ MANAGEMENT REPORTS ] ---");
        System.out.println(" [8] Report 1: Daily Occupancy & Room Tier Summary");
        System.out.println(" [9] Report 2: Pending Settlement & High-Value Guest Analysis");
        System.out.println();
        System.out.println(" --- [ NAVIGATION ] ---");
        System.out.println(" [0] Back to Main System Menu");
        System.out.println("==========================================================================");
        System.out.print("Select Option [0-9]: ");

        if (!scanner.hasNextLine()) return 0;
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // =========================================================================
    // INPUT METHODS
    // =========================================================================

    public String inputConfirmationNumber() {
        System.out.print("Enter 8-digit Confirmation Number (or [0] to cancel): ");
        if (!scanner.hasNextLine()) return "";
        String s = scanner.nextLine().trim();
        return "0".equals(s) ? "" : s;
    }

    public String inputGuestSearchQuery() {
        System.out.print("Enter Guest Name or IC Number to Search: ");
        if (!scanner.hasNextLine()) return "";
        return scanner.nextLine().trim();
    }

    public String inputRoomType() {
        System.out.println("\nSelect Room Type:");
        System.out.println(" [1] Standard");
        System.out.println(" [2] Deluxe");
        System.out.println(" [3] Suite");
        System.out.print("Select Choice [1-3]: ");
        if (!scanner.hasNextLine()) return "Standard";
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "2": return "Deluxe";
            case "3": return "Suite";
            default:  return "Standard";
        }
    }

    public String inputText(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) return "";
        return scanner.nextLine().trim();
    }

    public double inputDouble(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) return 0.0;
        try {
            return Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public int inputInt(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) return 0;
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int getUpdateChargesChoice() {
        System.out.println("\n--- Update Guest Record ---");
        System.out.println(" [1] Add Incidental Charge (Room Service / Spa / Minibar)");
        System.out.println(" [2] Extend Stay (Update Check-Out Date)");
        System.out.println(" [0] Cancel");
        System.out.print("Select Option [0-2]: ");
        if (!scanner.hasNextLine()) return 0;
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public boolean confirmAction(String actionDescription) {
        System.out.println("\n------------------------------------------");
        System.out.println(" CONFIRMATION: " + actionDescription);
        System.out.println(" [1] Yes, Proceed");
        System.out.println(" [0] No, Cancel");
        System.out.print("Select [0-1]: ");
        if (!scanner.hasNextLine()) return false;
        return "1".equals(scanner.nextLine().trim());
    }

    // =========================================================================
    // DISPLAY METHODS
    // =========================================================================

    public void displayMessage(String message) {
        System.out.println("\n>>> " + message + "\n");
    }

    public void displayHeader(String title) {
        System.out.println("\n==========================================================================");
        System.out.println(" " + title);
        System.out.println("==========================================================================");
    }

    public void displayFooter() {
        System.out.println("--------------------------------------------------------------------------");
    }

    public void displaySectionDivider() {
        System.out.println("------------------------------------------");
    }

    public void displayReservationDetails(String folio) {
        System.out.println("\n==========================================");
        System.out.println("         GUEST FOLIO / RESERVATION DETAILS");
        System.out.println("==========================================");
        System.out.println(folio);
        System.out.println("==========================================");
    }

    public void printReportHeader(String title) {
        System.out.println("\n==========================================================================");
        System.out.printf("  REPORT: %s%n", title);
        System.out.println("==========================================================================");
    }

    public void printReportFooter(int totalRecords) {
        System.out.println("==========================================================================");
        System.out.println("  Total Records: " + totalRecords);
        System.out.println("==========================================================================\n");
    }

    public void pressEnterToContinue() {
        System.out.print("\nPress [ENTER] to return to Front-Desk Menu...");
        if (scanner.hasNextLine()) scanner.nextLine();
    }
}
