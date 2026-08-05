package control;

import adt.HashMap;
import adt.MapInterface;
import boundary.FrontDeskUI;
import entity.Guest;
import entity.Reservation;
import entity.Room;
import utility.MessageUI;

/**
 * Controller for the Front-Desk Service subsystem.
 * Implements full CRUD on a custom HashMap (Non-Linear ADT) keyed by
 * 8-digit confirmation number, plus searching and management reports.
 *
 * ECB Pattern: This class serves as the Control layer.
 *
 * @author Mun Jun How
 */
public class FrontDeskController {

    // -------------------------------------------------------------------------
    // Non-Linear ADT: Custom HashMap<String, Reservation>
    // Key   = 8-digit Confirmation Number (for O(1) lookup)
    // Value = Reservation object
    // -------------------------------------------------------------------------
    private MapInterface<String, Reservation> reservationsMap;

    // Separate array for room inventory
    private Room[] rooms;

    private FrontDeskUI ui;
    private int nextConfirmationSuffix; // Used to generate unique 8-digit IDs

    // =========================================================================
    // CONSTRUCTOR & DATA INITIALISATION
    // =========================================================================

    public FrontDeskController() {
        reservationsMap = new HashMap<>();
        ui = new FrontDeskUI();
        nextConfirmationSuffix = 5;
        initHardcodedData();
    }

    /**
     * Populates the HashMap collection with hard-coded demo data.
     *
     * Per assignment specification: "You may populate the collection objects
     * by reading from a file or using a method which adds hard-coded entity values."
     * This method fulfils that requirement by pre-loading realistic sample
     * reservations so all features (search, reports, CRUD) can be demonstrated.
     */
    private void initHardcodedData() {
        // --- Room Inventory (matches room types: Standard / Deluxe / Suite) ---
        rooms = new Room[]{
            new Room("101", "Standard", 1, "Occupied",           ""),
            new Room("102", "Standard", 1, "Ready for Check-In", ""),
            new Room("201", "Deluxe",   2, "Occupied",           ""),
            new Room("202", "Deluxe",   2, "Ready for Check-In", ""),
            new Room("203", "Deluxe",   2, "Ready for Check-In", ""),
            new Room("301", "Suite",    3, "Occupied",           ""),
            new Room("302", "Suite",    3, "Ready for Check-In", "")
        };

        // --- Guest Entity Objects ---
        Guest g1 = new Guest("990101-14-5566", "Ali Bin Abu",    "012-3456789");
        Guest g2 = new Guest("881212-10-1234", "Chong Wei",      "016-9876543");
        Guest g3 = new Guest("010203-01-9988", "Siti Nurhaliza", "019-1112222");
        Guest g4 = new Guest("750505-08-4433", "John Doe",       "011-5556666");

        // --- Reservation Entities: put into HashMap with 8-digit confirmation number as key ---
        // Reservation 1: Checked-In, Standard Room, has room service charge
        Reservation r1 = new Reservation("12345678", g1, rooms[0], "2026-08-01", "2026-08-05", 200.00, "Checked-In");
        r1.addIncidentalCharge(85.00);  // Room service

        // Reservation 2: Confirmed (not yet checked in), no incidentals
        Reservation r2 = new Reservation("87654321", g2, rooms[2], "2026-08-10", "2026-08-12", 350.00, "Confirmed");

        // Reservation 3: Checked-In, Suite, high-value guest with spa & dining charges
        Reservation r3 = new Reservation("11223344", g3, rooms[5], "2026-08-02", "2026-08-07", 600.00, "Checked-In");
        r3.addIncidentalCharge(650.00); // Spa, minibar & restaurant combined

        // Reservation 4: Confirmed, Deluxe Room, pre-booked packages
        Reservation r4 = new Reservation("99887766", g4, rooms[3], "2026-08-15", "2026-08-20", 350.00, "Confirmed");
        r4.addIncidentalCharge(900.00); // Pre-booked spa & tour packages

        // Insert all reservations into custom HashMap (Non-Linear ADT)
        reservationsMap.put(r1.getConfirmationNumber(), r1);
        reservationsMap.put(r2.getConfirmationNumber(), r2);
        reservationsMap.put(r3.getConfirmationNumber(), r3);
        reservationsMap.put(r4.getConfirmationNumber(), r4);
    }

    // =========================================================================
    // MAIN RUN LOOP
    // =========================================================================

    public void run() {
        int choice;
        do {
            choice = ui.getMenuChoice();
            switch (choice) {
                // --- Inquiry & Search Services ---
                case 1: guestLookupByConfirmation(); break;
                case 2: searchGuestByNameOrIC();     break;
                case 3: checkRoomAvailability();     break;
                case 4: billingFolioInquiry();       break;
                // --- Record Management (CRUD) ---
                case 5: checkInNewGuest();           break;
                case 6: updateGuestCharges();        break;
                case 7: checkOutGuest();             break;
                // --- Reports ---
                case 8: reportDailyOccupancy();              break;
                case 9: reportPendingSettlementHighValue();  break;
                case 0: MessageUI.displayExitMessage();      break;
                default: MessageUI.displayInvalidChoiceMessage();
            }
        } while (choice != 0);
    }

    // =========================================================================
    // SECTION 1: INQUIRY & SEARCH SERVICES
    // =========================================================================

    /**
     * Feature 1: O(1) lookup using the HashMap.get(key) on the 8-digit
     * confirmation number — the core Non-Linear ADT showcase.
     */
    private void guestLookupByConfirmation() {
        ui.displayHeader("GUEST IDENTIFICATION & LOOKUP");
        String confNum = ui.inputConfirmationNumber();
        if (confNum.isEmpty()) { ui.displayMessage("Operation cancelled."); return; }

        // Validate: must be 8 digits
        if (!confNum.matches("\\d{8}")) {
            ui.displayMessage("Invalid format. Confirmation number must be exactly 8 digits.");
            ui.pressEnterToContinue();
            return;
        }

        // O(1) HashMap lookup
        Reservation res = reservationsMap.get(confNum);

        if (res != null) {
            ui.displayReservationDetails(res.toFolioString());
        } else {
            ui.displayMessage("No reservation found for confirmation number: " + confNum);
        }
        ui.pressEnterToContinue();
    }

    /**
     * Feature 2: Guest search by Name or IC number.
     * Demonstrates Linear Search through HashMap values — used when the
     * primary key (confirmation number) is unknown to the guest.
     */
    private void searchGuestByNameOrIC() {
        ui.displayHeader("SEARCH GUEST BY NAME / IC NUMBER");
        String query = ui.inputGuestSearchQuery();
        if (query.isEmpty()) { ui.displayMessage("Search query cannot be empty."); return; }

        ui.displayHeader("SEARCH RESULTS FOR: \"" + query + "\"");
        boolean found = false;
        Object[] allRes = reservationsMap.values();

        // Linear Search through all values
        for (Object obj : allRes) {
            Reservation r = (Reservation) obj;
            if (r.getGuest().getName().toLowerCase().contains(query.toLowerCase()) ||
                r.getGuest().getIcNumber().contains(query)) {
                System.out.println(r);
                found = true;
            }
        }

        if (!found) {
            ui.displayMessage("No guests matching \"" + query + "\" were found.");
        } else {
            ui.displayFooter();
        }
        ui.pressEnterToContinue();
    }

    /**
     * Feature 3: Room Availability Query.
     * Filters the room array for available rooms of the requested type.
     */
    private void checkRoomAvailability() {
        ui.displayHeader("ROOM AVAILABILITY QUERY");
        String type = ui.inputRoomType();

        System.out.println();
        System.out.printf("%-8s | %-15s | %-6s | %-22s%n",
                "Room ID", "Room Type", "Floor", "Status");
        ui.displayFooter();

        int availableCount = 0;
        for (Room r : rooms) {
            if (r.getRoomType().equalsIgnoreCase(type)) {
                System.out.printf("%-8s | %-15s | %-6d | %-22s%n",
                        r.getRoomId(), r.getRoomType(), r.getFloorNumber(), r.getCurrentStatus());
                if (r.getCurrentStatus().equalsIgnoreCase("Ready for Check-In")) {
                    availableCount++;
                }
            }
        }
        ui.displayFooter();
        ui.displayMessage(availableCount + " room(s) of type [" + type + "] are available for Check-In.");
        ui.pressEnterToContinue();
    }

    /**
     * Feature 4: Billing & Folio Inquiry.
     * Retrieves full folio with room rate, incidentals, total, and payment status.
     */
    private void billingFolioInquiry() {
        ui.displayHeader("BILLING & FOLIO INQUIRY");
        String confNum = ui.inputConfirmationNumber();
        if (confNum.isEmpty()) { ui.displayMessage("Operation cancelled."); return; }

        Reservation res = reservationsMap.get(confNum);
        if (res == null) {
            ui.displayMessage("No reservation found for: " + confNum);
            ui.pressEnterToContinue();
            return;
        }

        ui.displayReservationDetails(res.toFolioString());
        ui.pressEnterToContinue();
    }

    // =========================================================================
    // SECTION 2: FRONT-DESK RECORD MANAGEMENT (CRUD)
    // =========================================================================

    /**
     * Feature 5: Check-In New Guest — INSERT into HashMap.
     * Demonstrates the put(key, value) operation on the custom Non-Linear ADT.
     */
    private void checkInNewGuest() {
        ui.displayHeader("CHECK-IN NEW GUEST (Add Reservation to Map)");

        // Collect guest information
        String ic   = ui.inputText("Enter Guest IC Number (e.g., 990101-14-5566): ");
        String name = ui.inputText("Enter Guest Full Name: ");
        String contact = ui.inputText("Enter Guest Contact Number: ");

        if (ic.isEmpty() || name.isEmpty()) {
            ui.displayMessage("IC and Name are required. Operation cancelled.");
            return;
        }

        // Show available rooms
        ui.displayMessage("--- Available Rooms ---");
        boolean anyAvailable = false;
        for (Room r : rooms) {
            if (r.getCurrentStatus().equalsIgnoreCase("Ready for Check-In")) {
                System.out.printf("  Room %-4s | %-10s | Floor %d%n",
                        r.getRoomId(), r.getRoomType(), r.getFloorNumber());
                anyAvailable = true;
            }
        }
        if (!anyAvailable) {
            ui.displayMessage("No rooms are currently available for check-in.");
            ui.pressEnterToContinue();
            return;
        }

        String roomId = ui.inputText("Enter Room ID to assign: ").toUpperCase();
        Room selectedRoom = findRoomById(roomId);

        if (selectedRoom == null) {
            ui.displayMessage("Room " + roomId + " not found.");
            ui.pressEnterToContinue();
            return;
        }
        if (!selectedRoom.getCurrentStatus().equalsIgnoreCase("Ready for Check-In")) {
            ui.displayMessage("Room " + roomId + " is not available (Status: " + selectedRoom.getCurrentStatus() + ").");
            ui.pressEnterToContinue();
            return;
        }

        String checkIn  = ui.inputText("Enter Check-In Date (YYYY-MM-DD): ");
        String checkOut = ui.inputText("Enter Check-Out Date (YYYY-MM-DD): ");
        double rate     = ui.inputDouble("Enter Room Rate per Night (RM): ");

        // Generate unique 8-digit confirmation number
        String confNum = String.format("20260%03d", nextConfirmationSuffix++);

        Guest newGuest       = new Guest(ic, name, contact);
        Reservation newRes   = new Reservation(confNum, newGuest, selectedRoom,
                                               checkIn, checkOut, rate, "Checked-In");

        // Mark room as Occupied
        selectedRoom.setCurrentStatus("Occupied");

        // INSERT into HashMap — key = confirmation number
        reservationsMap.put(confNum, newRes);

        ui.displayMessage("Check-In successful!\nConfirmation Number: " + confNum +
                          "\nGuest: " + name + " | Room: " + roomId);
        ui.pressEnterToContinue();
    }

    /**
     * Feature 6: Update Guest Charges / Extend Stay.
     * Demonstrates retrieving and mutating an object stored in the HashMap.
     */
    private void updateGuestCharges() {
        ui.displayHeader("UPDATE GUEST CHARGES / EXTEND STAY");
        String confNum = ui.inputConfirmationNumber();
        if (confNum.isEmpty()) { ui.displayMessage("Operation cancelled."); return; }

        // O(1) retrieval from HashMap
        Reservation res = reservationsMap.get(confNum);
        if (res == null) {
            ui.displayMessage("Reservation not found: " + confNum);
            ui.pressEnterToContinue();
            return;
        }

        if (!res.getStatus().equalsIgnoreCase("Checked-In")) {
            ui.displayMessage("Can only update records with status 'Checked-In'. Current: " + res.getStatus());
            ui.pressEnterToContinue();
            return;
        }

        System.out.println("Current Folio:");
        System.out.println(res.toFolioString());
        ui.displaySectionDivider();

        int updateChoice = ui.getUpdateChargesChoice();
        switch (updateChoice) {
            case 1:
                // Add incidental charge
                String desc   = ui.inputText("Enter Charge Description (e.g., Room Service, Spa): ");
                double amount = ui.inputDouble("Enter Charge Amount (RM): ");
                if (amount <= 0) {
                    ui.displayMessage("Invalid amount. Must be greater than 0.");
                } else {
                    res.addIncidentalCharge(amount);
                    ui.displayMessage("Charge of RM" + String.format("%.2f", amount) + " (" + desc + ") added.\nNew Total: RM" + String.format("%.2f", res.getTotalAmount()));
                }
                break;
            case 2:
                // Extend stay
                String newCheckOut = ui.inputText("Enter New Check-Out Date (YYYY-MM-DD): ");
                double extraNights  = ui.inputDouble("Enter Number of Additional Nights: ");
                if (extraNights > 0) {
                    res.setCheckOutDate(newCheckOut);
                    res.addIncidentalCharge(extraNights * res.getRoomRate()); // extra night charges
                    ui.displayMessage("Stay extended to " + newCheckOut + ". Extra RM" +
                            String.format("%.2f", extraNights * res.getRoomRate()) + " added.");
                } else {
                    ui.displayMessage("Invalid number of nights.");
                }
                break;
            case 0:
                ui.displayMessage("Update cancelled.");
                return;
            default:
                MessageUI.displayInvalidChoiceMessage();
        }
        ui.pressEnterToContinue();
    }

    /**
     * Feature 7: Check-Out Guest — REMOVE from HashMap.
     * Demonstrates the remove(key) operation on the custom Non-Linear ADT.
     */
    private void checkOutGuest() {
        ui.displayHeader("CHECK-OUT GUEST (Remove from Active Map)");
        String confNum = ui.inputConfirmationNumber();
        if (confNum.isEmpty()) { ui.displayMessage("Operation cancelled."); return; }

        Reservation res = reservationsMap.get(confNum);
        if (res == null) {
            ui.displayMessage("No reservation found for: " + confNum);
            ui.pressEnterToContinue();
            return;
        }

        if (!res.getStatus().equalsIgnoreCase("Checked-In")) {
            ui.displayMessage("Guest is not currently checked in. Status: " + res.getStatus());
            ui.pressEnterToContinue();
            return;
        }

        // Show final folio
        System.out.println("\n--- FINAL FOLIO FOR CHECK-OUT ---");
        System.out.println(res.toFolioString());
        ui.displaySectionDivider();

        if (!ui.confirmAction("Check out guest " + res.getGuest().getName() +
                " and collect RM" + String.format("%.2f", res.getTotalAmount()) + " payment")) {
            ui.displayMessage("Check-out cancelled.");
            ui.pressEnterToContinue();
            return;
        }

        // Mark as paid and checked-out
        res.setPaid(true);
        res.setStatus("Checked-Out");

        // Free up the room
        Room room = res.getRoom();
        if (room != null) {
            room.setCurrentStatus("Dirty"); // Triggers housekeeping cycle
        }

        // REMOVE from active HashMap
        reservationsMap.remove(confNum);

        ui.displayMessage("Check-out successful for " + res.getGuest().getName() +
                ".\nRoom " + (room != null ? room.getRoomId() : "N/A") + " is now Dirty (pending housekeeping).\nPayment collected: RM" +
                String.format("%.2f", res.getTotalAmount()));
        ui.pressEnterToContinue();
    }

    // =========================================================================
    // SECTION 3: MANAGEMENT REPORTS
    // =========================================================================

    /**
     * Report 1: Daily Front-Desk Occupancy & Room Tier Summary.
     * Filters active "Checked-In" reservations, sorts them by Room Type
     * (using Selection Sort), and summarises occupancy by tier.
     */
    private void reportDailyOccupancy() {
        ui.printReportHeader("DAILY FRONT-DESK OCCUPANCY & ROOM TIER SUMMARY");

        Object[] allRes = reservationsMap.values();

        // Filter: only Checked-In
        Reservation[] checkedIn = new Reservation[allRes.length];
        int count = 0;
        for (Object obj : allRes) {
            Reservation r = (Reservation) obj;
            if (r != null && "Checked-In".equalsIgnoreCase(r.getStatus())) {
                checkedIn[count++] = r;
            }
        }
        checkedIn = trimArray(checkedIn, count);

        // Sort by Room Type (alphabetical) using Selection Sort
        selectionSortByRoomType(checkedIn);

        // Print table
        System.out.printf("%-10s | %-20s | %-12s | %-10s | %-12s | %-10s%n",
                "Conf. No.", "Guest Name", "Room Type", "Room ID", "Check-In", "Total (RM)");
        ui.displayFooter();

        int standard = 0, deluxe = 0, suite = 0;
        for (Reservation r : checkedIn) {
            System.out.printf("%-10s | %-20s | %-12s | %-10s | %-12s | RM%-8.2f%n",
                    r.getConfirmationNumber(),
                    r.getGuest().getName(),
                    r.getRoom().getRoomType(),
                    r.getRoom().getRoomId(),
                    r.getCheckInDate(),
                    r.getTotalAmount());
            switch (r.getRoom().getRoomType().toLowerCase()) {
                case "standard": standard++; break;
                case "deluxe":   deluxe++;   break;
                case "suite":    suite++;    break;
            }
        }
        ui.displayFooter();

        // Calculate total rooms per tier for occupancy rate
        int totalStd = 0, totalDlx = 0, totalSte = 0;
        for (Room rm : rooms) {
            switch (rm.getRoomType().toLowerCase()) {
                case "standard": totalStd++; break;
                case "deluxe":   totalDlx++; break;
                case "suite":    totalSte++; break;
            }
        }

        System.out.println("\n  OCCUPANCY SUMMARY BY ROOM TIER:");
        System.out.printf("  %-12s : %d / %d occupied (%.0f%%)%n",
                "Standard", standard, totalStd, totalStd > 0 ? standard * 100.0 / totalStd : 0);
        System.out.printf("  %-12s : %d / %d occupied (%.0f%%)%n",
                "Deluxe", deluxe, totalDlx, totalDlx > 0 ? deluxe * 100.0 / totalDlx : 0);
        System.out.printf("  %-12s : %d / %d occupied (%.0f%%)%n",
                "Suite", suite, totalSte, totalSte > 0 ? suite * 100.0 / totalSte : 0);

        int totalOccupied = standard + deluxe + suite;
        System.out.printf("  %-12s : %d / %d (%.0f%%)%n",
                "Overall", totalOccupied, rooms.length,
                rooms.length > 0 ? totalOccupied * 100.0 / rooms.length : 0);

        ui.printReportFooter(checkedIn.length);
        ui.pressEnterToContinue();
    }

    /**
     * Report 2: Pending Settlement & High-Value Guest Analysis.
     * Filters all reservations where payment is OUTSTANDING,
     * sorted by Total Amount descending (QuickSort) to highlight highest bills.
     */
    private void reportPendingSettlementHighValue() {
        ui.printReportHeader("PENDING SETTLEMENT & HIGH-VALUE GUEST ANALYSIS");

        Object[] allRes = reservationsMap.values();

        // Filter: outstanding (unpaid) reservations
        Reservation[] unpaid = new Reservation[allRes.length];
        int count = 0;
        for (Object obj : allRes) {
            Reservation r = (Reservation) obj;
            if (r != null && !r.isPaid()) {
                unpaid[count++] = r;
            }
        }
        unpaid = trimArray(unpaid, count);

        // Sort by Total Amount descending using QuickSort
        quickSortByAmountDesc(unpaid, 0, unpaid.length - 1);

        // Print table
        System.out.printf("%-10s | %-20s | %-12s | %-10s | %-12s | %-12s | %-10s%n",
                "Conf. No.", "Guest Name", "Room Type", "Room", "Room Rate", "Incidentals", "TOTAL (RM)");
        ui.displayFooter();

        double totalOutstanding = 0;
        for (Reservation r : unpaid) {
            System.out.printf("%-10s | %-20s | %-12s | %-10s | RM%-10.2f | RM%-10.2f | RM%-8.2f%n",
                    r.getConfirmationNumber(),
                    r.getGuest().getName(),
                    r.getRoom().getRoomType(),
                    r.getRoom().getRoomId(),
                    r.getRoomRate(),
                    r.getIncidentalCharges(),
                    r.getTotalAmount());
            totalOutstanding += r.getTotalAmount();
        }
        ui.displayFooter();

        System.out.printf("%n  TOTAL OUTSTANDING BALANCE : RM%.2f%n", totalOutstanding);

        // Highlight high-value guests (> RM1000)
        System.out.println("\n  HIGH-VALUE ALERTS (Outstanding > RM 1,000.00):");
        boolean anyHighValue = false;
        for (Reservation r : unpaid) {
            if (r.getTotalAmount() > 1000.0) {
                System.out.printf("  [!] %s (%s) - Room %s - RM%.2f OUTSTANDING%n",
                        r.getGuest().getName(),
                        r.getGuest().getContactNumber(),
                        r.getRoom().getRoomId(),
                        r.getTotalAmount());
                anyHighValue = true;
            }
        }
        if (!anyHighValue) {
            System.out.println("  No high-value outstanding bills at this time.");
        }

        ui.printReportFooter(unpaid.length);
        ui.pressEnterToContinue();
    }

    // =========================================================================
    // HELPER / UTILITY METHODS
    // =========================================================================

    private Room findRoomById(String roomId) {
        for (Room r : rooms) {
            if (r.getRoomId().equalsIgnoreCase(roomId)) return r;
        }
        return null;
    }

    private Reservation[] trimArray(Reservation[] arr, int size) {
        Reservation[] trimmed = new Reservation[size];
        for (int i = 0; i < size; i++) trimmed[i] = arr[i];
        return trimmed;
    }

    // =========================================================================
    // SORTING ALGORITHMS
    // =========================================================================

    /**
     * Selection Sort — sorts Reservations by Room Type alphabetically (A-Z).
     * Used in Report 1: Daily Occupancy.
     */
    private void selectionSortByRoomType(Reservation[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < n; j++) {
                if (arr[j].getRoom().getRoomType().compareToIgnoreCase(
                        arr[minIdx].getRoom().getRoomType()) < 0) {
                    minIdx = j;
                }
            }
            if (minIdx != i) {
                Reservation temp = arr[i];
                arr[i] = arr[minIdx];
                arr[minIdx] = temp;
            }
        }
    }

    /**
     * QuickSort — sorts Reservations by Total Amount descending.
     * Used in Report 2: Pending Settlement.
     */
    private void quickSortByAmountDesc(Reservation[] arr, int low, int high) {
        if (low < high) {
            int pi = partitionByAmount(arr, low, high);
            quickSortByAmountDesc(arr, low, pi - 1);
            quickSortByAmountDesc(arr, pi + 1, high);
        }
    }

    private int partitionByAmount(Reservation[] arr, int low, int high) {
        double pivot = arr[high].getTotalAmount();
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (arr[j].getTotalAmount() > pivot) { // descending
                i++;
                Reservation temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        Reservation temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;
        return i + 1;
    }
}
