package boundary;

import java.util.Scanner;

/**
 * Console Boundary UI class for Walk-In Registrations & Standard Booking
 * Procedure.
 * Adheres strictly to the ECB pattern for user input and console rendering.
 * 
 * @author Chan Shao Lun
 */
public class WalkInBookingUI {

    private static Scanner scanner = new Scanner(System.in);

    public int getMenuChoice() {
        while (true) {
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

            if (!scanner.hasNextLine())
                return 0;
            String input = scanner.nextLine().trim();
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 0 && choice <= 7) {
                    return choice;
                }
                displayMessage("Invalid choice! Please select a number between 0 and 7.");
            } catch (NumberFormatException e) {
                displayMessage("Invalid input! Please enter a valid number (0-7).");
            }
        }
    }

    public int inputBookingTypeChoice() {
        while (true) {
            System.out.println("\nSelect Booking Channel Type:");
            System.out.println(" [1] Walk-In Guest (On-the-spot Check-In)");
            System.out.println(" [2] Standard Advance Reservation");
            System.out.println(" [0] Back to Menu");
            System.out.print("Select Option [0-2]: ");
            if (!scanner.hasNextLine())
                return 0;
            String s = scanner.nextLine().trim();
            if ("0".equals(s))
                return 0;
            if ("1".equals(s))
                return 1;
            if ("2".equals(s))
                return 2;
            displayMessage("Invalid option! Please select 0, 1, or 2.");
        }
    }

    public String inputRoomType() {
        while (true) {
            System.out.println("\nSelect Requested Room Type:");
            System.out.println(" [1] Standard Room (RM 200.00 / night)");
            System.out.println(" [2] Deluxe Room   (RM 350.00 / night)");
            System.out.println(" [3] Suite Room    (RM 600.00 / night)");
            System.out.println(" [0] Back to Menu");
            System.out.print("Select Option [0-3]: ");
            if (!scanner.hasNextLine())
                return "CANCEL";
            String s = scanner.nextLine().trim();
            switch (s) {
                case "1":
                    return "Standard Room";
                case "2":
                    return "Deluxe Room";
                case "3":
                    return "Suite Room";
                case "0":
                    return "CANCEL";
                default:
                    displayMessage("Invalid option! Please select 0, 1, 2, or 3.");
            }
        }
    }

    public String inputText(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine())
            return "";
        return scanner.nextLine().trim();
    }

    public String inputRequiredText(String prompt, String errorMessage) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine())
                return "";
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            displayMessage(errorMessage);
        }
    }

    public int inputInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine())
                return 0;
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                displayMessage("Invalid input! Please enter a valid number.");
            }
        }
    }

    public int inputIntInRange(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine())
                return min;
            String input = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(input);
                if (val >= min && val <= max) {
                    return val;
                }
                displayMessage(String.format("Invalid option! Please enter a number between %d and %d.", min, max));
            } catch (NumberFormatException e) {
                displayMessage(
                        String.format("Invalid input! Please enter a valid number between %d and %d.", min, max));
            }
        }
    }

    public boolean confirmAction(String actionDescription) {
        while (true) {
            System.out.println("\n------------------------------------------");
            System.out.println(" CONFIRMATION: " + actionDescription);
            System.out.println(" [1] Yes, Proceed");
            System.out.println(" [0] No, Cancel");
            System.out.print("Select [0-1]: ");
            if (!scanner.hasNextLine())
                return false;
            String s = scanner.nextLine().trim();
            if ("1".equals(s))
                return true;
            if ("0".equals(s))
                return false;
            displayMessage("Invalid choice! Please select 1 (Yes) or 0 (No).");
        }
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
        if (scanner.hasNextLine())
            scanner.nextLine();
    }
}
