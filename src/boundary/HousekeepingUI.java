package boundary;

import java.util.Scanner;

/**
 * Boundary UI class for Housekeeping and Task Log Management.
 * Handles console display, user input collection, and output formatting.
 * 
 * @author Tan Jun Qi
 */
public class HousekeepingUI {

    private static Scanner scanner = new Scanner(System.in);

    public int getMenuChoice() {
        System.out.println("==========================================================================");
        System.out.println("               TARUMT RESORTS - HOUSEKEEPING & TASK LOG                   ");
        System.out.println("==========================================================================");
        System.out.println(" --- [ TASK OPERATIONS ] ---");
        System.out.println(" [1] View All Rooms & Current Statuses");
        System.out.println(" [2] Assign New Cleaning Task to Staff");
        System.out.println(" [3] Update Sequential Cleaning Status (Dirty -> Cleaning -> Inspected -> Ready)");
        System.out.println(" [4] Rollback / Undo Status Change (Stack ADT - Undo mistakes / Late Check-Out)");
        System.out.println();
        System.out.println(" --- [ SEARCH & QUERIES ] ---");
        System.out.println(" [5] Search Room Status or Task History");
        System.out.println();
        System.out.println(" --- [ MANAGEMENT REPORTS ] ---");
        System.out.println(" [6] [Report 1] Room Cleaning Efficiency & Turnaround Summary");
        System.out.println(" [7] [Report 2] Housekeeping Rollback & Exception Audit Log");
        System.out.println();
        System.out.println(" --- [ NAVIGATION ] ---");
        System.out.println(" [0] Back to Main System Menu");
        System.out.println("==========================================================================");
        System.out.print("Select Option Number [0-7]: ");

        if (!scanner.hasNextLine()) {
            return 0;
        }
        String line = scanner.nextLine().trim();
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String inputRoomId() {
        System.out.print("Enter Room ID (e.g., R101) or [0] to Cancel: ");
        if (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim().toUpperCase();
            if ("0".equals(input)) {
                return "";
            }
            return input;
        }
        return "";
    }

    public String inputStaffId() {
        System.out.print("Enter Staff ID (e.g., ST101) or [0] to Cancel: ");
        if (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim().toUpperCase();
            if ("0".equals(input)) {
                return "";
            }
            return input;
        }
        return "";
    }

    public String inputRoomType() {
        System.out.println("\nSelect Room Type:");
        System.out.println(" [1] Standard Deluxe");
        System.out.println(" [2] Executive Suite");
        System.out.println(" [3] Presidential Suite");
        System.out.print("Select Choice [1-3]: ");
        String choice = scanner.hasNextLine() ? scanner.nextLine().trim() : "1";
        switch (choice) {
            case "2": return "Executive Suite";
            case "3": return "Presidential Suite";
            default: return "Standard Deluxe";
        }
    }

    public int inputFloorNumber() {
        System.out.print("Enter Floor Number [1-10]: ");
        int floor = 1;
        if (scanner.hasNextLine()) {
            try {
                floor = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                floor = 1;
            }
        }
        return floor;
    }

    public int selectNextStatusChoice(String currentStatus) {
        System.out.println("\n--------------------------------------------------------------------------");
        System.out.println(" Current Room Task Status: [" + currentStatus + "]");
        System.out.println(" Workflow: [Dirty] -> [Cleaning In Progress] -> [Inspected] -> [Ready for Check-In]");
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("Select New Status Step:");
        
        if ("Dirty".equalsIgnoreCase(currentStatus)) {
            System.out.println(" [1] Cleaning In Progress  <-- (RECOMMENDED NEXT STEP)");
            System.out.println(" [2] Inspected");
            System.out.println(" [3] Ready for Check-In");
        } else if ("Cleaning In Progress".equalsIgnoreCase(currentStatus)) {
            System.out.println(" [1] Cleaning In Progress");
            System.out.println(" [2] Inspected             <-- (RECOMMENDED NEXT STEP)");
            System.out.println(" [3] Ready for Check-In");
        } else if ("Inspected".equalsIgnoreCase(currentStatus)) {
            System.out.println(" [1] Cleaning In Progress");
            System.out.println(" [2] Inspected");
            System.out.println(" [3] Ready for Check-In    <-- (RECOMMENDED NEXT STEP)");
        } else {
            System.out.println(" [1] Cleaning In Progress");
            System.out.println(" [2] Inspected");
            System.out.println(" [3] Ready for Check-In");
        }
        System.out.println(" [0] Cancel & Back to Operations Menu");
        System.out.print("Select Action [0-3]: ");
        int choice = -1;
        if (scanner.hasNextLine()) {
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                choice = -1;
            }
        }
        return choice;
    }

    public boolean confirmAction(String actionName) {
        System.out.println("\n--------------------------------------------------------------------------");
        System.out.println(" CONFIRMATION REQUIRED: " + actionName);
        System.out.println(" [1] Yes, proceed with " + actionName);
        System.out.println(" [0] No, cancel operation");
        System.out.print("Select Choice [0-1]: ");
        if (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();
            return "1".equals(input);
        }
        return false;
    }

    public String inputReason() {
        System.out.print("Enter Reason / Note (e.g., 'Late Check-out', 'Incorrect Input') [Press ENTER for default]: ");
        if (scanner.hasNextLine()) {
            return scanner.nextLine().trim();
        }
        return "";
    }

    public String inputSearchQuery() {
        System.out.print("Enter Room ID (e.g., R101) or Staff ID (e.g., ST101) to Search: ");
        if (scanner.hasNextLine()) {
            return scanner.nextLine().trim().toUpperCase();
        }
        return "";
    }

    public void pressEnterToContinue() {
        System.out.println("Press [ENTER] to return to the Housekeeping Menu...");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }

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
}

