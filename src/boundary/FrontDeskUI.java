package boundary;

import java.util.Scanner;

/**
 * Console Boundary UI class for the Front-Desk Service subsystem.
 * Handles all console display and user input following the ECB pattern.
 * All input methods support [0] to cancel/exit mid-flow.
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
        System.out.println(" [1] Guest Identification & Stay Lookup (8-digit Confirmation Number)");
        System.out.println(" [2] Search Guest by Name / IC Number");
        System.out.println(" [3] Check Room Availability by Type");
        System.out.println(" [4] Billing & Detailed Folio Inquiry");
        System.out.println();
        System.out.println(" --- [ FRONT-DESK RECORD MANAGEMENT ] ---");
        System.out.println(" [5] Update Guest Charges / Extend Stay");
        System.out.println(" [6] Check-Out Guest (Process Payment & Release Room)");
        System.out.println();
        System.out.println(" --- [ MANAGEMENT REPORTS ] ---");
        System.out.println(" [7] Report 1: Daily Occupancy & Room Tier Summary");
        System.out.println(" [8] Report 2: Pending Settlement & High-Value Guest Analysis");
        System.out.println();
        System.out.println(" --- [ NAVIGATION ] ---");
        System.out.println(" [0] Back to Main System Menu");
        System.out.println("==========================================================================");
        System.out.print("Select Option [0-8]: ");

        if (!scanner.hasNextLine()) return 0;
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // =========================================================================
    // VALIDATED INPUT METHODS  (all support "0" to cancel — returns null)
    // =========================================================================

    /**
     * Prompts for IC Number and validates format: XXXXXX-XX-XXXX.
     * Returns null if user types "0" to cancel.
     */
    public String inputIcNumber() {
        while (true) {
            System.out.print("Enter Guest IC Number (format: XXXXXX-XX-XXXX) or [0] to cancel: ");
            if (!scanner.hasNextLine()) return null;
            String input = scanner.nextLine().trim();
            if ("0".equals(input)) return null;
            if (input.isEmpty()) {
                System.out.println("  [!] IC Number cannot be empty. Please try again.");
                continue;
            }
            // Validate format: 6 digits - 2 digits - 4 digits
            if (input.matches("\\d{6}-\\d{2}-\\d{4}")) {
                return input;
            }
            System.out.println("  [!] Invalid IC format. Expected format: 990101-14-5566. Please try again.");
        }
    }

    /**
     * Prompts for a guest full name (letters/spaces only, non-empty).
     * Returns null if user types "0" to cancel.
     */
    public String inputGuestName() {
        while (true) {
            System.out.print("Enter Guest Full Name or [0] to cancel: ");
            if (!scanner.hasNextLine()) return null;
            String input = scanner.nextLine().trim();
            if ("0".equals(input)) return null;
            if (input.isEmpty()) {
                System.out.println("  [!] Name cannot be empty. Please try again.");
                continue;
            }
            // Must contain at least one letter (reject purely numeric input)
            if (!input.matches(".*[a-zA-Z].*")) {
                System.out.println("  [!] Name must contain letters. Please enter a valid full name.");
                continue;
            }
            return input;
        }
    }

    /**
     * Prompts for a contact number (digits/dashes, non-empty).
     * Returns null if user types "0" to cancel.
     */
    public String inputContactNumber() {
        while (true) {
            System.out.print("Enter Guest Contact Number or [0] to cancel: ");
            if (!scanner.hasNextLine()) return null;
            String input = scanner.nextLine().trim();
            if ("0".equals(input)) return null;
            if (input.isEmpty()) {
                System.out.println("  [!] Contact number cannot be empty. Please try again.");
                continue;
            }
            // Allow digits and dashes only
            if (!input.matches("[0-9\\-+]+")) {
                System.out.println("  [!] Invalid contact number. Use digits and dashes only.");
                continue;
            }
            return input;
        }
    }

    /**
     * Prompts for a date in YYYY-MM-DD format.
     * Validates it is a real calendar date.
     * Returns null if user types "0" to cancel.
     */
    public String inputDate(String prompt) {
        while (true) {
            System.out.print(prompt + " (YYYY-MM-DD) or [0] to cancel: ");
            if (!scanner.hasNextLine()) return null;
            String input = scanner.nextLine().trim();
            if ("0".equals(input)) return null;

            // Step 1: format check
            if (!input.matches("\\d{4}-\\d{2}-\\d{2}")) {
                System.out.println("  [!] Invalid format. Please use YYYY-MM-DD format.");
                continue;
            }

            // Step 2: real calendar date check
            try {
                java.time.LocalDate.parse(input);  // throws if impossible date like 12-34
                return input;
            } catch (java.time.format.DateTimeParseException e) {
                System.out.println("  [!] '" + input + "' is not a valid calendar date.");
            }
        }
    }

    /**
     * Prompts for a positive room rate.
     * Returns -1 if user types "0" to cancel.
     */
    public double inputRoomRate() {
        while (true) {
            System.out.print("Enter Room Rate per Night (RM) or [0] to cancel: ");
            if (!scanner.hasNextLine()) return -1;
            String input = scanner.nextLine().trim();
            if ("0".equals(input)) return -1;
            try {
                double rate = Double.parseDouble(input);
                if (rate > 0) return rate;
                System.out.println("  [!] Rate must be greater than RM 0.00. Please try again.");
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid amount. Please enter a numeric value.");
            }
        }
    }

    /**
     * Prompts for a Room ID. Loops until user enters a valid Room ID from the
     * provided list that is "Ready for Check-In", or types "0" to cancel.
     * @param roomList the current list of rooms to validate against
     * @return the selected Room, or null if user cancelled
     */
    public entity.Room inputAndSelectRoom(adt.ListInterface<entity.Room> roomList) {
        while (true) {
            System.out.print("Enter Room ID to assign or [0] to cancel: ");
            if (!scanner.hasNextLine()) return null;
            String input = scanner.nextLine().trim().toUpperCase();

            if ("0".equals(input)) return null;

            if (input.isEmpty()) {
                System.out.println("  [!] Room ID cannot be empty. Please try again.");
                continue;
            }

            // Search for the room in the list
            entity.Room found = null;
            for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
                entity.Room r = roomList.getEntry(i);
                if (r.getRoomId().equalsIgnoreCase(input)) {
                    found = r;
                    break;
                }
            }

            if (found == null) {
                System.out.println("  [!] Room '" + input + "' does not exist. Please choose a Room ID from the table above.");
                continue;
            }

            String status = found.getCurrentStatus();
            if ("Ready for Check-In".equalsIgnoreCase(status)) {
                return found; // Valid room — proceed
            }

            // Room exists but status is not ready — explain and loop
            if ("Dirty".equalsIgnoreCase(status) || "Cleaning In Progress".equalsIgnoreCase(status)) {
                System.out.println("  [!] Room " + found.getRoomId() + " is currently '" + status + "'.");
                System.out.println("      This room is being cleaned by Housekeeping and cannot be assigned.");
            } else if ("Inspected".equalsIgnoreCase(status)) {
                System.out.println("  [!] Room " + found.getRoomId() + " is 'Inspected' and awaiting final approval.");
                System.out.println("      Please choose a room with status 'Ready for Check-In'.");
            } else if ("Occupied".equalsIgnoreCase(status)) {
                System.out.println("  [!] Room " + found.getRoomId() + " is already 'Occupied' by another guest.");
            } else {
                System.out.println("  [!] Room " + found.getRoomId() + " has status '" + status + "' and cannot be assigned.");
            }
            System.out.println("      Please select a different room or enter [0] to cancel.");
        }
    }

    public String inputRoomId() {
        while (true) {
            System.out.print("Enter Room ID to assign or [0] to cancel: ");
            if (!scanner.hasNextLine()) return null;
            String input = scanner.nextLine().trim().toUpperCase();
            if ("0".equals(input)) return null;
            if (input.isEmpty()) {
                System.out.println("  [!] Room ID cannot be empty. Please try again.");
                continue;
            }
            return input;
        }
    }


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
        while (true) {
            System.out.println("\nSelect Room Availability Filter:");
            System.out.println("  [1] Standard Room (RM 200.00 / night)");
            System.out.println("  [2] Deluxe Room   (RM 350.00 / night)");
            System.out.println("  [3] Suite Room    (RM 600.00 / night)");
            System.out.println("  [4] All Room Types (Full Resort Inventory)");
            System.out.println("  [0] Cancel");
            System.out.print("Select Choice [0-4]: ");
            if (!scanner.hasNextLine()) return "";
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": return "Standard Room";
                case "2": return "Deluxe Room";
                case "3": return "Suite Room";
                case "4": return "ALL";
                case "0": return "";
                default:
                    System.out.println("\n  [!] Invalid choice! Please enter 0, 1, 2, 3, or 4.");
            }
        }
    }

    public String inputText(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) return "";
        return scanner.nextLine().trim();
    }

    public double inputDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) return 0.0;
            String input = scanner.nextLine().trim();
            if ("0".equals(input)) return 0.0;
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid number. Please enter a numeric value.");
            }
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
