package boundary;

import java.util.Scanner;

/**
 * Main System Boundary UI class for TARUMT Resorts Luxury Hospitality Chain.
 * Serves as the main menu entry point integrating all team member modules.
 * 
 * @author Tan Jun Qi
 */
public class MainSystemUI {

    private static Scanner scanner = new Scanner(System.in);

    public int getMainSystemMenuChoice() {
        System.out.println("==========================================================================");
        System.out.println("            TARUMT RESORTS - INTEGRATED MANAGEMENT SYSTEM                 ");
        System.out.println("==========================================================================");
        System.out.println("1. Walk-In Registrations & Standard Booking Procedure (Linear ADT)");
        System.out.println("2. Front-Desk Service & Guest Inquiries (Non-Linear ADT & Searching)");
        System.out.println("3. Housekeeping & Task Log Subsystem (Linear ADT)");
        System.out.println("0. Exit Application");
        System.out.println("==========================================================================");
        System.out.print("Select Subsystem (0-3): ");

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

    public void displayTeammateModuleNotice(String moduleName) {
        System.out.println("--------------------------------------------------------------------------");
        System.out.println(" [TEAMMATE MODULE]: " + moduleName);
        System.out.println("--------------------------------------------------------------------------");
        System.out.println(" This module scope is assigned to your team member.");
        System.out.println(" When integrated, your teammate's control class will launch here.");
        System.out.println("--------------------------------------------------------------------------\n");
    }

    public void displayExitSystemMessage() {
        System.out.println("Thank you for using TARUMT Resorts Integrated Management System. Goodbye!");
    }
}
