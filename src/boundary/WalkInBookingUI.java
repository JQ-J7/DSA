package boundary;

import java.util.Scanner;

/**
 * Console Boundary UI class for Walk-In Registrations & Standard Booking Procedure.
 * Adheres strictly to the ECB pattern for user input and console rendering.
 * 
 * @author Walk-In Subsystem Lead
 */
public class WalkInBookingUI {

    private static Scanner scanner = new Scanner(System.in);

    public int getMenuChoice() {
        System.out.println("\n==========================================================================");
        System.out.println("   TARUMT RESORTS - WALK-IN REGISTRATIONS & STANDARD BOOKING (LINEAR ADT) ");
        System.out.println("==========================================================================");
        System.out.println(" --- [ CHRONOLOGICAL QUEUE OPERATIONS ] ---");
        System.out.println(" [1] Register New Walk-In / Standard Booking (Enqueue Arrival)");
        System.out.println(" [2] View Pending Chronological Queue (FIFO Order)");
        System.out.println(" [3] Allocate Room to Next Waiting Guest (Dequeue & Assign)");
        System.out.println(" [4] Cancel / Modify Pending Registration");
        System.out.println();
        System.out.println(" --- [ SEARCH & QUERY SERVICES ] ---");
        System.out.println(" [5] Search Registrations (by Booking ID, IC Number, or Guest Name)");
        System.out.println();
        System.out.println(" --- [ MANAGEMENT ANALYTICAL REPORTS ] ---");
        System.out.println(" [6] Report 1: Peak Season Queue Allocation & Efficiency Summary");
        System.out.println(" [7] Report 2: Booking Channel Performance & Financial Forecast");
        System.out.println();
        System.out.println(" --- [ NAVIGATION ] ---");
        System.out.println(" [0] Back to Main System Menu");
        System.out.println("==========================================================================");
        System.out.print("Select Choice [0-7]: ");

        if (!scanner.hasNextLine()) return 0;
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public int inputBookingTypeChoice() {
        System.out.println("\nSelect Booking Channel Type:");
        System.out.println(" [1] Walk-In Guest (On-the-spot Check-In)");
        System.out.println(" [2] Standard Advance Reservation");
        System.out.print("Select Option [1-2]: ");
        if (!scanner.hasNextLine()) return 1;
        String s = scanner.nextLine().trim();
        return "2".equals(s) ? 2 : 1;
    }

    public String inputRoomType() {
        System.out.println("\nSelect Requested Room Type:");
        System.out.println(" [1] Standard Room (RM 200.00 / night)");
        System.out.println(" [2] Deluxe Room   (RM 350.00 / night)");
        System.out.println(" [3] Suite Room    (RM 600.00 / night)");
        System.out.print("Select Option [1-3]: ");
        if (!scanner.hasNextLine()) return "Standard Room";
        String s = scanner.nextLine().trim();
        switch (s) {
            case "2": return "Deluxe Room";
            case "3": return "Suite Room";
            default:  return "Standard Room";
        }
    }

    public String inputText(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) return "";
        return scanner.nextLine().trim();
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

    public boolean confirmAction(String actionDescription) {
        System.out.println("\n------------------------------------------");
        System.out.println(" CONFIRMATION: " + actionDescription);
        System.out.println(" [1] Yes, Proceed");
        System.out.println(" [0] No, Cancel");
        System.out.print("Select [0-1]: ");
        if (!scanner.hasNextLine()) return false;
        return "1".equals(scanner.nextLine().trim());
    }

    public void displayHeader(String title) {
        System.out.println("\n==========================================================================");
        System.out.println(" " + title);
        System.out.println("==========================================================================");
    }

    public void displayMessage(String message) {
        System.out.println("\n>>> " + message + "\n");
    }

    public void printReportHeader(String title) {
        System.out.println("\n==========================================================================");
        System.out.printf(" MANAGEMENT REPORT: %s%n", title);
        System.out.println("==========================================================================");
    }

    public void printReportFooter(int totalRecords) {
        System.out.println("==========================================================================");
        System.out.printf("  Total Records Analyzed: %d%n", totalRecords);
        System.out.println("==========================================================================\n");
    }

    public void pressEnterToContinue() {
        System.out.print("\nPress [ENTER] to return to menu...");
        if (scanner.hasNextLine()) scanner.nextLine();
    }
}
