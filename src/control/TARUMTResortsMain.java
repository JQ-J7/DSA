package control;

import boundary.MainSystemUI;
import utility.MessageUI;

/**
 * Main Controller and Entry Point for TARUMT Resorts System.
 * Integrates all 3 team member modules:
 * - Module 1: Walk-In & Standard Booking
 * - Module 2: Front-Desk Service
 * - Module 3: Housekeeping & Task Log
 * 
 * @author Tan Jun Qi
 */
public class TARUMTResortsMain {

    private MainSystemUI mainUI = new MainSystemUI();
    private HousekeepingControl housekeepingControl = new HousekeepingControl();

    public void runMainSystem() {
        int choice;
        do {
            choice = mainUI.getMainSystemMenuChoice();
            switch (choice) {
                case 1:
                    mainUI.displayTeammateModuleNotice("Walk-In Registrations & Standard Booking Procedure");
                    break;
                case 2:
                    mainUI.displayTeammateModuleNotice("Front-Desk Service & Guest Inquiries");
                    break;
                case 3:
                    // Launches User's Housekeeping and Task Log Subsystem
                    housekeepingControl.runHousekeepingSystem();
                    break;
                case 0:
                    mainUI.displayExitSystemMessage();
                    break;
                default:
                    MessageUI.displayInvalidChoiceMessage();
            }
        } while (choice != 0);
    }

    public static void main(String[] args) {
        TARUMTResortsMain system = new TARUMTResortsMain();
        system.runMainSystem();
    }
}
