package control;

import adt.ArrayList;
import adt.HashMap;
import adt.ListInterface;
import adt.MapInterface;
import boundary.FrontDeskUI;
import boundary.HousekeepingUI;
import dao.FrontDeskDAO;
import dao.HousekeepingDAO;
import dao.WalkInBookingDAO;
import entity.Guest;
import entity.Reservation;
import entity.Room;
import entity.WalkInBooking;
import utility.MessageUI;

/**
 * Control Class for the Front-Desk Service subsystem.
 * Implements full CRUD on a custom HashMap (Non-Linear ADT) keyed by
 * 8-digit confirmation number, plus searching and management reports.
 *
 * ECB Pattern: This class serves as the Control layer.
 *
 * @author Mun Jun How
 */
public class FrontDeskControl {

    // -------------------------------------------------------------------------
    // Non-Linear ADT: Custom HashMap<String, Reservation>
    // Key   = 8-digit Confirmation Number (for O(1) lookup)
    // Value = Reservation object
    // -------------------------------------------------------------------------
    private MapInterface<String, Reservation> reservationsMap = new HashMap<>();
    private ListInterface<Room> roomList = new ArrayList<>();
    private ListInterface<Reservation> checkoutHistory = new ArrayList<>();

    private FrontDeskDAO dao = new FrontDeskDAO();
    private HousekeepingDAO housekeepingDAO = new HousekeepingDAO();
    private WalkInBookingDAO walkInBookingDAO = new WalkInBookingDAO();
    private FrontDeskUI ui = new FrontDeskUI();
    private int nextConfirmationSuffix = 5; // Used to generate unique 8-digit IDs

    // =========================================================================
    // CONSTRUCTOR & DATA INITIALISATION
    // =========================================================================

    public FrontDeskControl() {
        ui = new FrontDeskUI();
        nextConfirmationSuffix = 5;

        // Synchronize with shared rooms.dat
        roomList = housekeepingDAO.retrieveRoomsFromFile();
        if (roomList == null || roomList.isEmpty()) {
            addDefaultDemoRooms();
        } else {
            addDefaultDemoRooms();
        }

        // Load reservations from binary file via DAO
        reservationsMap = dao.retrieveReservationsFromFile();

        // Load past check-out history from binary file
        checkoutHistory = dao.retrieveHistoryFromFile();

        // Populate default demo data if map is empty
        if (reservationsMap == null || reservationsMap.isEmpty()) {
            initHardcodedData();
        }
    }

    /**
     * Adds and synchronizes the constant 15 hotel rooms (Floor 1-3)
     * matching the Housekeeping and Walk-In subsystem standards.
     */
    private void addDefaultDemoRooms() {
        Room[] constantRooms = new Room[] {
            // Floor 1: 1 Suite Room, 2 Deluxe Rooms, 2 Standard Rooms
            new Room("R101", "Suite Room", 1, "Dirty", "UNASSIGNED"),
            new Room("R102", "Deluxe Room", 1, "Cleaning In Progress", "ST101"),
            new Room("R103", "Deluxe Room", 1, "Inspected", "ST101"),
            new Room("R104", "Standard Room", 1, "Ready for Check-In", "ST102"),
            new Room("R105", "Standard Room", 1, "Ready for Check-In", "ST102"),

            // Floor 2: 1 Suite Room, 2 Deluxe Rooms, 2 Standard Rooms
            new Room("R201", "Suite Room", 2, "Dirty", "UNASSIGNED"),
            new Room("R202", "Deluxe Room", 2, "Cleaning In Progress", "ST103"),
            new Room("R203", "Deluxe Room", 2, "Inspected", "ST103"),
            new Room("R204", "Standard Room", 2, "Ready for Check-In", "ST103"),
            new Room("R205", "Standard Room", 2, "Ready for Check-In", "ST103"),

            // Floor 3: 1 Suite Room, 2 Deluxe Rooms, 2 Standard Rooms
            new Room("R301", "Suite Room", 3, "Dirty", "ST104"),
            new Room("R302", "Deluxe Room", 3, "Cleaning In Progress", "ST104"),
            new Room("R303", "Deluxe Room", 3, "Inspected", "ST104"),
            new Room("R304", "Standard Room", 3, "Ready for Check-In", "ST104"),
            new Room("R305", "Standard Room", 3, "Ready for Check-In", "ST104")
        };
        for (Room r : constantRooms) {
            Room existing = findRoomById(r.getRoomId());
            if (existing == null) {
                roomList.add(r);
            } else {
                existing.setRoomType(r.getRoomType());
                existing.setFloorNumber(r.getFloorNumber());
            }
        }
        housekeepingDAO.saveRoomsToFile(roomList);
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
        Room r101 = findRoomById("R101");
        Room r201 = findRoomById("R201");
        Room r301 = findRoomById("R301");
        Room r104 = findRoomById("R104");

        if (r101 == null) r101 = new Room("R101", "Suite Room", 1, "Occupied", "UNASSIGNED");
        if (r201 == null) r201 = new Room("R201", "Suite Room", 2, "Occupied", "UNASSIGNED");
        if (r301 == null) r301 = new Room("R301", "Suite Room", 3, "Occupied", "ST104");
        if (r104 == null) r104 = new Room("R104", "Standard Room", 1, "Ready for Check-In", "ST102");

        // --- Guest Entity Objects ---
        Guest g1 = new Guest("990101-14-5566", "Ali Bin Abu",    "012-3456789");
        Guest g2 = new Guest("881212-10-1234", "Chong Wei",      "016-9876543");
        Guest g3 = new Guest("010203-01-9988", "Siti Nurhaliza", "019-1112222");
        Guest g4 = new Guest("750505-08-4433", "John Doe",       "011-5556666");

        // --- Reservation Entities: put into HashMap with 8-digit confirmation number as key ---
        Reservation res1 = new Reservation("12345678", g1, r101, "2026-08-01", "2026-08-05", 600.00, "Checked-In");
        res1.addIncidentalCharge(85.00);

        Reservation res2 = new Reservation("87654321", g2, r201, "2026-08-10", "2026-08-12", 600.00, "Confirmed");

        Reservation res3 = new Reservation("11223344", g3, r301, "2026-08-02", "2026-08-07", 600.00, "Checked-In");
        res3.addIncidentalCharge(650.00);

        Reservation res4 = new Reservation("99887766", g4, r104, "2026-08-15", "2026-08-20", 200.00, "Confirmed");
        res4.addIncidentalCharge(900.00);

        reservationsMap.put(res1.getConfirmationNumber(), res1);
        reservationsMap.put(res2.getConfirmationNumber(), res2);
        reservationsMap.put(res3.getConfirmationNumber(), res3);
        reservationsMap.put(res4.getConfirmationNumber(), res4);

        // Persist initial data to file via DAO
        dao.saveReservationsToFile(reservationsMap);
    }

    // =========================================================================
    // MAIN RUN LOOP
    // =========================================================================

    public void run() {
        runFrontDeskSystem();
    }

    public void runFrontDeskSystem() {
        // Reload latest room and reservation data from shared files each time subsystem is entered
        // so changes made by Housekeeping or Walk-In are reflected live
        ListInterface<Room> latestRooms = housekeepingDAO.retrieveRoomsFromFile();
        if (latestRooms != null && !latestRooms.isEmpty()) {
            roomList = latestRooms;
        }
        MapInterface<String, Reservation> latestRes = dao.retrieveReservationsFromFile();
        if (latestRes != null && !latestRes.isEmpty()) {
            reservationsMap = latestRes;
        }

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
                case 5: updateGuestCharges();        break;
                case 6: checkOutGuest();             break;
                // --- Reports ---
                case 7: reportDailyOccupancy();              break;
                case 8: reportPendingSettlementHighValue();  break;
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
     * Displays concise Guest Identification & Stay Profile.
     */
    private void guestLookupByConfirmation() {
        ui.displayHeader("GUEST IDENTIFICATION & STAY LOOKUP");
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
            System.out.println("\n==========================================================================");
            System.out.println("             GUEST IDENTIFICATION & STAY INFORMATION");
            System.out.println("==========================================================================");
            System.out.println(res.toStayInfoString());
            System.out.println("==========================================================================");
        } else {
            ui.displayMessage("No reservation found for confirmation number: " + confNum +
                    "\nTip: Use Option [2] to search by Guest Name or IC Number.");
        }
        ui.pressEnterToContinue();
    }

    /**
     * Feature 2: Guest search by Name or IC number.
     * Demonstrates Linear Search through HashMap values — used as the PRIMARY
     * RECOVERY TOOL when the 8-digit confirmation number is forgotten.
     */
    private void searchGuestByNameOrIC() {
        ui.displayHeader("SEARCH GUEST BY NAME / IC NUMBER");
        String query = ui.inputGuestSearchQuery();
        if (query.isEmpty()) { ui.displayMessage("Search query cannot be empty."); return; }

        ui.displayHeader("SEARCH RESULTS FOR: \"" + query + "\"");
        boolean found = false;
        Object[] allRes = reservationsMap.values();

        // Linear Search through all values in the HashMap
        for (Object obj : allRes) {
            Reservation r = (Reservation) obj;
            if (r.getGuest().getName().toLowerCase().contains(query.toLowerCase()) ||
                r.getGuest().getIcNumber().contains(query)) {
                
                System.out.println("--------------------------------------------------------------------------");
                System.out.println("Guest Found         : " + r.getGuest().getName());
                System.out.println("IC Number           : " + r.getGuest().getIcNumber());
                System.out.println("Contact Number      : " + r.getGuest().getContactNumber());
                System.out.println("Room                : " + r.getRoom().getRoomId() + " (" + r.getRoom().getRoomType() + ", Floor " + r.getRoom().getFloorNumber() + ")");
                System.out.println("Confirmation Number : " + r.getConfirmationNumber());
                System.out.println("Reservation Status  : " + r.getStatus());
                System.out.println("Stay Period         : " + r.getCheckInDate() + " to " + r.getCheckOutDate() + " (" + r.getNumberOfNights() + " night(s))");
                System.out.println("Room Rate           : RM " + String.format("%.2f", r.getRoomRate()) + " / night");
                System.out.println("Incidentals Added   : RM " + String.format("%.2f", r.getIncidentalCharges()));
                System.out.println("Total Folio Amount  : RM " + String.format("%.2f", r.getTotalAmount()) + " (Status: " + (r.isPaid() ? "PAID" : "OUTSTANDING") + ")");
                System.out.println("--------------------------------------------------------------------------");
                found = true;
            }
        }

        if (!found) {
            ui.displayMessage("No active reservations matching \"" + query + "\" were found in Front-Desk.\n" +
                "Tip: If the guest has not been allocated a room yet, check the Walk-In Waiting Queue.");
        }
        ui.pressEnterToContinue();
    }

    /**
     * Feature 3: Real-Time Room Availability & Inventory Query.
     * Instantly queries live room statuses, cross-references active occupants from HashMap,
     * and produces a detailed availability breakdown by tier and floor.
     */
    private void checkRoomAvailability() {
        ui.displayHeader("REAL-TIME ROOM INVENTORY & AVAILABILITY QUERY");

        // Reload latest room data and reservations from shared binary files
        ListInterface<Room> latestRooms = housekeepingDAO.retrieveRoomsFromFile();
        if (latestRooms != null && !latestRooms.isEmpty()) {
            roomList = latestRooms;
        }
        MapInterface<String, Reservation> latestRes = dao.retrieveReservationsFromFile();
        if (latestRes != null && !latestRes.isEmpty()) {
            reservationsMap = latestRes;
        }

        String filterType = ui.inputRoomType();
        if (filterType.isEmpty()) {
            ui.displayMessage("Operation cancelled.");
            return;
        }

        boolean showAll = "ALL".equalsIgnoreCase(filterType);

        System.out.println("\n================================================================================================================");
        System.out.printf("  %-8s | %-16s | %-8s | %-14s | %-22s | %-30s%n",
                "Room ID", "Room Type", "Floor", "Nightly Rate", "Status", "Occupant / Notes");
        System.out.println("----------------------------------------------------------------------------------------------------------------");

        int totalCount = 0;
        int readyCount = 0;
        int occupiedCount = 0;
        int dirtyCount = 0;
        int cleaningCount = 0;
        int inspectedCount = 0;

        int[] readyPerFloor = new int[4]; // floors 1 to 3

        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
            Room r = roomList.getEntry(i);
            boolean match = showAll || r.getRoomType().equalsIgnoreCase(filterType) ||
                    r.getRoomType().toLowerCase().contains(filterType.toLowerCase());

            if (!match) continue;

            totalCount++;
            int floor = r.getFloorNumber();
            String status = r.getCurrentStatus();
            double rate = getRateForRoomType(r.getRoomType());

            String occupantNote = "-";

            if ("Ready for Check-In".equalsIgnoreCase(status)) {
                occupantNote = "Ready for Immediate Check-In";
                readyCount++;
                if (floor >= 1 && floor <= 3) readyPerFloor[floor]++;
            } else if ("Occupied".equalsIgnoreCase(status)) {
                occupiedCount++;
                Reservation activeRes = findReservationByRoomId(r.getRoomId());
                if (activeRes != null) {
                    occupantNote = activeRes.getGuest().getName() + " (" + activeRes.getConfirmationNumber() + ")";
                } else {
                    occupantNote = "In-House Guest";
                }
            } else if ("Cleaning In Progress".equalsIgnoreCase(status)) {
                cleaningCount++;
                occupantNote = "Cleaning by " + r.getAssignedStaffId();
            } else if ("Inspected".equalsIgnoreCase(status)) {
                inspectedCount++;
                occupantNote = "Awaiting Final Release";
            } else if ("Dirty".equalsIgnoreCase(status)) {
                dirtyCount++;
                occupantNote = "Pending Housekeeping Clean";
            } else {
                occupantNote = "Status: " + status;
            }

            System.out.printf("  %-8s | %-16s | %-8s | %-14s | %-22s | %-30s%n",
                    r.getRoomId(),
                    r.getRoomType(),
                    "Floor " + floor,
                    String.format("RM %.2f", rate),
                    status,
                    occupantNote);
        }

        System.out.println("================================================================================================================");

        // Summary Breakdown Card
        String categoryName = showAll ? "All Room Types (Full Resort Overview)" : filterType;
        int pipelineTotal = dirtyCount + cleaningCount + inspectedCount;

        System.out.println("\n  ROOM AVAILABILITY & INVENTORY SUMMARY:");
        System.out.println("  -----------------------------------------------------------------");
        System.out.printf("  Target Category         : %s%n", categoryName);
        System.out.printf("  Total Rooms in Scope    : %d room(s)%n", totalCount);
        System.out.printf("  - Ready for Check-In    : %d room(s)%n", readyCount);
        System.out.printf("  - Occupied by Guests    : %d room(s)%n", occupiedCount);
        System.out.printf("  - Housekeeping Pipeline : %d room(s)  [ %d Dirty | %d Cleaning In Progress | %d Inspected ]%n",
                pipelineTotal, dirtyCount, cleaningCount, inspectedCount);
        System.out.println("  -----------------------------------------------------------------");

        System.out.println("  Floor-by-Floor Ready Availability:");
        System.out.printf("  - Floor 1 : %d ready room(s)%n", readyPerFloor[1]);
        System.out.printf("  - Floor 2 : %d ready room(s)%n", readyPerFloor[2]);
        System.out.printf("  - Floor 3 : %d ready room(s)%n", readyPerFloor[3]);
        System.out.println("  -----------------------------------------------------------------");

        ui.pressEnterToContinue();
    }

    private double getRateForRoomType(String roomType) {
        if (roomType == null) return 200.00;
        String t = roomType.toLowerCase();
        if (t.contains("suite")) return 600.00;
        if (t.contains("deluxe")) return 350.00;
        return 200.00;
    }

    private Reservation findReservationByRoomId(String roomId) {
        if (roomId == null) return null;
        for (Object obj : reservationsMap.values()) {
            Reservation r = (Reservation) obj;
            if (r != null && r.getRoom() != null && roomId.equalsIgnoreCase(r.getRoom().getRoomId())) {
                if ("Checked-In".equalsIgnoreCase(r.getStatus()) || "Confirmed".equalsIgnoreCase(r.getStatus())) {
                    return r;
                }
            }
        }
        return null;
    }

    /**
     * Feature 4: Billing & Detailed Folio Inquiry.
     * Retrieves itemized folio with room subtotal, nights, incidentals, total, and payment status.
     * Supports lookup by Confirmation Number, Guest Name, or IC Number.
     */
    private void billingFolioInquiry() {
        ui.displayHeader("BILLING & DETAILED FOLIO INQUIRY");
        Reservation res = findReservationPrompt("BILLING & DETAILED FOLIO INQUIRY", false);
        if (res == null) {
            return;
        }

        ui.displayReservationDetails(res.toFolioString());
        ui.pressEnterToContinue();
    }

    /**
     * Helper method to find a reservation by Confirmation Number, Guest Name, or IC Number.
     * Used across Billing Inquiry, Update Charges, and Check-Out.
     */
    private Reservation findReservationPrompt(String actionTitle, boolean onlyCheckedIn) {
        System.out.println("Find Reservation By:");
        System.out.println("  [1] Confirmation Number (8-digit)");
        System.out.println("  [2] Guest Name or IC Number");
        System.out.println("  [0] Cancel");
        int lookupChoice = ui.inputInt("Select [0-2]: ");

        if (lookupChoice == 1) {
            String confNum = ui.inputConfirmationNumber();
            if (confNum.isEmpty()) { ui.displayMessage("Operation cancelled."); return null; }
            Reservation res = reservationsMap.get(confNum);
            if (res == null) {
                ui.displayMessage("No reservation found for confirmation number: " + confNum +
                        "\nTip: Use Option [2] to search by Guest Name or IC Number.");
                ui.pressEnterToContinue();
                return null;
            }
            if (onlyCheckedIn && !"Checked-In".equalsIgnoreCase(res.getStatus())) {
                ui.displayMessage("Guest is not currently checked in. Status: " + res.getStatus());
                ui.pressEnterToContinue();
                return null;
            }
            return res;

        } else if (lookupChoice == 2) {
            String query = ui.inputGuestSearchQuery();
            if (query.isEmpty() || "0".equals(query)) { ui.displayMessage("Operation cancelled."); return null; }

            Object[] allRes = reservationsMap.values();
            adt.ListInterface<Reservation> matches = new adt.ArrayList<>();
            for (Object obj : allRes) {
                Reservation r = (Reservation) obj;
                if (onlyCheckedIn && !"Checked-In".equalsIgnoreCase(r.getStatus())) continue;
                if (r.getGuest().getName().toLowerCase().contains(query.toLowerCase()) ||
                    r.getGuest().getIcNumber().contains(query)) {
                    matches.add(r);
                }
            }

            if (matches.isEmpty()) {
                ui.displayMessage("No " + (onlyCheckedIn ? "checked-in " : "") + "guest found matching \"" + query + "\".");
                ui.pressEnterToContinue();
                return null;
            }

            if (matches.getNumberOfEntries() == 1) {
                return matches.getEntry(1);
            } else {
                System.out.println("\nMultiple matching guests found:");
                System.out.printf("  %-4s | %-10s | %-20s | %-14s | %-6s%n",
                        "No.", "Conf. No.", "Guest Name", "IC Number", "Room");
                System.out.println("  ----------------------------------------------------------");
                for (int i = 1; i <= matches.getNumberOfEntries(); i++) {
                    Reservation r = matches.getEntry(i);
                    System.out.printf("  %-4d | %-10s | %-20s | %-14s | %-6s%n",
                            i, r.getConfirmationNumber(),
                            r.getGuest().getName(), r.getGuest().getIcNumber(),
                            r.getRoom().getRoomId());
                }
                int pick = ui.inputInt("Select guest number [1-" + matches.getNumberOfEntries() + "] or 0 to cancel: ");
                if (pick < 1 || pick > matches.getNumberOfEntries()) {
                    ui.displayMessage("Operation cancelled.");
                    return null;
                }
                return matches.getEntry(pick);
            }
        } else {
            ui.displayMessage("Operation cancelled.");
            return null;
        }
    }

    // =========================================================================
    // SECTION 2: FRONT-DESK RECORD MANAGEMENT (CRUD)
    // =========================================================================

    /**
     * Feature 6: Update Guest Charges / Extend Stay.
     * Demonstrates retrieving and mutating an object stored in the HashMap.
     * Supports lookup by Confirmation Number, Guest Name, or IC Number.
     */
    private void updateGuestCharges() {
        ui.displayHeader("UPDATE GUEST CHARGES / EXTEND STAY");
        Reservation res = findReservationPrompt("UPDATE GUEST CHARGES / EXTEND STAY", true);
        if (res == null) {
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
        dao.saveReservationsToFile(reservationsMap);
        ui.pressEnterToContinue();
    }

    /**
     * Feature 7: Check-Out Guest — REMOVE from HashMap.
     * Demonstrates the remove(key) operation on the custom Non-Linear ADT.
     * Supports lookup by Confirmation Number, Guest Name, or IC Number.
     */
    private void checkOutGuest() {
        ui.displayHeader("CHECK-OUT GUEST (Remove from Active Map)");
        Reservation res = findReservationPrompt("CHECK-OUT GUEST", true);
        if (res == null) {
            return;
        }


        // Show current folio
        System.out.println("\n--- CURRENT FOLIO ---");
        System.out.println(res.toFolioString());
        ui.displaySectionDivider();

        // -----------------------------------------------------------------------
        // Late Check-Out Detection
        // Compare today's date vs planned check-out date
        // -----------------------------------------------------------------------
        java.time.LocalDate today       = java.time.LocalDate.now();
        java.time.LocalDate plannedOut  = null;
        boolean isLate = false;
        long lateNights = 0;
        try {
            plannedOut = java.time.LocalDate.parse(res.getCheckOutDate());
            if (today.isAfter(plannedOut)) {
                lateNights = java.time.temporal.ChronoUnit.DAYS.between(plannedOut, today);
                isLate = true;
            }
        } catch (Exception ignored) {}

        if (isLate) {
            double lateCharge = lateNights * res.getRoomRate();
            System.out.println("  ==================================================");
            System.out.println("  [!] LATE CHECK-OUT DETECTED");
            System.out.printf ("  Planned Check-Out : %s%n", res.getCheckOutDate());
            System.out.printf ("  Today's Date      : %s%n", today.toString());
            System.out.printf ("  Extra Nights      : %d night(s)%n", lateNights);
            System.out.printf ("  Late Charge Added : RM %.2f%n", lateCharge);
            System.out.println("  ==================================================");

            // Auto-add late night charges as incidental
            res.addIncidentalCharge(lateCharge);
            // Update check-out date to today
            res.setCheckOutDate(today.toString());
            System.out.println("\n  Late check-out charge of RM" + String.format("%.2f", lateCharge) +
                    " (" + lateNights + " extra night(s) x RM" + String.format("%.2f", res.getRoomRate()) + ") has been added.");

            System.out.println("\n--- UPDATED FINAL FOLIO ---");
            System.out.println(res.toFolioString());
            ui.displaySectionDivider();
        }

        if (!ui.confirmAction("Check out guest " + res.getGuest().getName() +
                " and collect RM" + String.format("%.2f", res.getTotalAmount()) + " payment")) {
            ui.displayMessage("Check-out cancelled.");
            ui.pressEnterToContinue();
            return;
        }

        // Mark as paid and checked-out
        res.setPaid(true);
        res.setStatus("Checked-Out");

        // Archive into persistent checkout history
        checkoutHistory.add(res);
        dao.saveHistoryToFile(checkoutHistory);

        // Free up the room and trigger Housekeeping status change to Dirty
        Room room = res.getRoom();
        if (room != null) {
            Room matchingRoom = findRoomById(room.getRoomId());
            if (matchingRoom != null) {
                matchingRoom.setCurrentStatus("Dirty");
                matchingRoom.setAssignedStaffId("UNASSIGNED");
            } else {
                room.setCurrentStatus("Dirty");
                room.setAssignedStaffId("UNASSIGNED");
            }
            housekeepingDAO.saveRoomsToFile(roomList);
        }

        // REMOVE from active HashMap
        reservationsMap.remove(res.getConfirmationNumber());
        dao.saveReservationsToFile(reservationsMap);

        ui.displayMessage("Check-out successful for " + res.getGuest().getName() +
                ".\nRoom " + (room != null ? room.getRoomId() : "N/A") + " status updated to 'Dirty' (pending housekeeping).\nPayment collected: RM" +
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
        System.out.printf("%-10s | %-20s | %-16s | %-10s | %-12s | %-10s%n",
                "Conf. No.", "Guest Name", "Room Type", "Room ID", "Check-In", "Total (RM)");
        ui.displayFooter();

        int standard = 0, deluxe = 0, suite = 0;
        for (Reservation r : checkedIn) {
            System.out.printf("%-10s | %-20s | %-16s | %-10s | %-12s | RM%-8.2f%n",
                    r.getConfirmationNumber(),
                    r.getGuest().getName(),
                    r.getRoom().getRoomType(),
                    r.getRoom().getRoomId(),
                    r.getCheckInDate(),
                    r.getTotalAmount());
            String type = r.getRoom().getRoomType().toLowerCase();
            if (type.contains("standard")) standard++;
            else if (type.contains("deluxe")) deluxe++;
            else if (type.contains("suite")) suite++;
        }
        ui.displayFooter();

        // Calculate total rooms per tier for occupancy rate
        int totalStd = 0, totalDlx = 0, totalSte = 0;
        int totalRoomsCount = roomList.getNumberOfEntries();
        for (int i = 1; i <= totalRoomsCount; i++) {
            Room rm = roomList.getEntry(i);
            String type = rm.getRoomType().toLowerCase();
            if (type.contains("standard")) totalStd++;
            else if (type.contains("deluxe")) totalDlx++;
            else if (type.contains("suite")) totalSte++;
        }

        System.out.println("\n  OCCUPANCY SUMMARY BY ROOM TIER:");
        System.out.printf("  %-16s : %d / %d occupied (%.0f%%)%n",
                "Standard Room", standard, totalStd, totalStd > 0 ? standard * 100.0 / totalStd : 0);
        System.out.printf("  %-16s : %d / %d occupied (%.0f%%)%n",
                "Deluxe Room", deluxe, totalDlx, totalDlx > 0 ? deluxe * 100.0 / totalDlx : 0);
        System.out.printf("  %-16s : %d / %d occupied (%.0f%%)%n",
                "Suite Room", suite, totalSte, totalSte > 0 ? suite * 100.0 / totalSte : 0);

        int totalOccupied = standard + deluxe + suite;
        System.out.printf("  %-16s : %d / %d (%.0f%%)%n",
                "Overall", totalOccupied, totalRoomsCount,
                totalRoomsCount > 0 ? totalOccupied * 100.0 / totalRoomsCount : 0);

        ui.printReportFooter(checkedIn.length);
        ui.pressEnterToContinue();
    }

    /**
     * Report 2: Pending Settlement & High-Value Guest Analysis.
     * Supports filtering by Date Range, Reservation Status, or Amount Threshold.
     * Sorts matching records by Total Amount descending (QuickSort) to highlight highest bills.
     */
    private void reportPendingSettlementHighValue() {
        ui.printReportHeader("PENDING SETTLEMENT & HIGH-VALUE GUEST ANALYSIS");

        System.out.println("Select Report Filter Option:");
        System.out.println("  [1] All Active & Outstanding Accounts (Default)");
        System.out.println("  [2] Filter by Check-In Date Range");
        System.out.println("  [3] Filter by Reservation Status (Checked-In, Confirmed, Checked-Out, All)");
        System.out.println("  [4] Filter by High-Value Threshold (> RM 1,000.00)");
        System.out.println("  [0] Cancel");
        int filterChoice = ui.inputInt("Select [0-4]: ");

        if (filterChoice == 0) {
            ui.displayMessage("Report cancelled.");
            return;
        }

        // Gather all active records and archived history
        adt.ListInterface<Reservation> allList = new adt.ArrayList<>();
        for (Object obj : reservationsMap.values()) {
            if (obj != null) allList.add((Reservation) obj);
        }
        for (int i = 1; i <= checkoutHistory.getNumberOfEntries(); i++) {
            Reservation r = checkoutHistory.getEntry(i);
            if (r != null) allList.add(r);
        }

        adt.ListInterface<Reservation> filteredList = new adt.ArrayList<>();
        String filterDescription = "All Active & Outstanding Accounts";

        if (filterChoice == 1) {
            // Default: All unpaid / active
            filterDescription = "All Outstanding Accounts";
            for (int i = 1; i <= allList.getNumberOfEntries(); i++) {
                Reservation r = allList.getEntry(i);
                if (!r.isPaid()) {
                    filteredList.add(r);
                }
            }

        } else if (filterChoice == 2) {
            // Date Range Filter
            System.out.println("\n--- FILTER BY CHECK-IN DATE RANGE ---");
            String startDate = ui.inputDate("Enter Start Date");
            if (startDate == null) { ui.displayMessage("Report cancelled."); return; }
            String endDate = ui.inputDate("Enter End Date");
            if (endDate == null) { ui.displayMessage("Report cancelled."); return; }

            java.time.LocalDate start = java.time.LocalDate.parse(startDate);
            java.time.LocalDate end   = java.time.LocalDate.parse(endDate);
            filterDescription = "Check-In Date Range (" + startDate + " to " + endDate + ")";

            for (int i = 1; i <= allList.getNumberOfEntries(); i++) {
                Reservation r = allList.getEntry(i);
                try {
                    java.time.LocalDate inDate = java.time.LocalDate.parse(r.getCheckInDate());
                    if (!inDate.isBefore(start) && !inDate.isAfter(end)) {
                        filteredList.add(r);
                    }
                } catch (Exception ignored) {}
            }

        } else if (filterChoice == 3) {
            // Status Filter
            System.out.println("\nSelect Reservation Status:");
            System.out.println("  [1] Checked-In (Active In-House)");
            System.out.println("  [2] Confirmed (Upcoming Bookings)");
            System.out.println("  [3] Checked-Out (Past Settled History)");
            System.out.println("  [4] All Statuses");
            int stChoice = ui.inputInt("Select [1-4]: ");

            String targetStatus = null;
            if (stChoice == 1) { targetStatus = "Checked-In"; filterDescription = "Status: Checked-In"; }
            else if (stChoice == 2) { targetStatus = "Confirmed"; filterDescription = "Status: Confirmed"; }
            else if (stChoice == 3) { targetStatus = "Checked-Out"; filterDescription = "Status: Checked-Out"; }
            else { filterDescription = "All Reservation Statuses"; }

            for (int i = 1; i <= allList.getNumberOfEntries(); i++) {
                Reservation r = allList.getEntry(i);
                if (targetStatus == null || targetStatus.equalsIgnoreCase(r.getStatus())) {
                    filteredList.add(r);
                }
            }

        } else if (filterChoice == 4) {
            // High-Value Threshold Filter
            double minThreshold = ui.inputDouble("Enter Minimum Outstanding Threshold (RM) [Press Enter for 1000.00]: ");
            if (minThreshold <= 0) minThreshold = 1000.00;
            filterDescription = "High-Value Accounts (Total >= RM " + String.format("%.2f", minThreshold) + ")";

            for (int i = 1; i <= allList.getNumberOfEntries(); i++) {
                Reservation r = allList.getEntry(i);
                if (r.getTotalAmount() >= minThreshold) {
                    filteredList.add(r);
                }
            }
        }

        if (filteredList.isEmpty()) {
            ui.displayMessage("No records found matching filter criteria: " + filterDescription);
            ui.pressEnterToContinue();
            return;
        }

        // Convert to array and sort by Total Amount descending using QuickSort
        Reservation[] results = new Reservation[filteredList.getNumberOfEntries()];
        for (int i = 1; i <= filteredList.getNumberOfEntries(); i++) {
            results[i - 1] = filteredList.getEntry(i);
        }
        quickSortByAmountDesc(results, 0, results.length - 1);

        System.out.println("\n  Filter Applied: " + filterDescription);
        System.out.println("  =========================================================================================================================");
        System.out.printf("  %-10s | %-18s | %-6s | %-11s | %-11s | %-6s | %-13s | %-12s | %-12s%n",
                "Conf. No.", "Guest Name", "Room", "Check-In", "Check-Out", "Nights", "Total (RM)", "Payment", "Status");
        System.out.println("  -------------------------------------------------------------------------------------------------------------------------");

        double totalGross = 0;
        double totalOutstanding = 0;
        double totalCollected = 0;

        for (Reservation r : results) {
            System.out.printf("  %-10s | %-18s | %-6s | %-11s | %-11s | %-6d | RM%-11.2f | %-12s | %-12s%n",
                    r.getConfirmationNumber(),
                    r.getGuest().getName(),
                    r.getRoom().getRoomId(),
                    r.getCheckInDate(),
                    r.getCheckOutDate(),
                    r.getNumberOfNights(),
                    r.getTotalAmount(),
                    r.isPaid() ? "PAID" : "OUTSTANDING",
                    r.getStatus());

            totalGross += r.getTotalAmount();
            if (r.isPaid()) {
                totalCollected += r.getTotalAmount();
            } else {
                totalOutstanding += r.getTotalAmount();
            }
        }
        System.out.println("  =========================================================================================================================");

        System.out.printf("%n  FINANCIAL SUMMARY FOR FILTERED RECORDS:%n");
        System.out.printf("  Total Matching Records    : %d%n", results.length);
        System.out.printf("  Total Gross Folio Value   : RM %.2f%n", totalGross);
        System.out.printf("  Total Outstanding Balance : RM %.2f%n", totalOutstanding);
        System.out.printf("  Total Settled / Collected : RM %.2f%n", totalCollected);

        // Highlight high-value guests (> RM1000)
        System.out.println("\n  HIGH-VALUE ALERTS (Amount > RM 1,000.00):");
        boolean anyHighValue = false;
        for (Reservation r : results) {
            if (r.getTotalAmount() >= 1000.0) {
                System.out.printf("  [!] %-18s (%s) - Room %-4s - Total: RM %-9.2f | Payment: %-11s | Status: %s%n",
                        r.getGuest().getName(),
                        r.getGuest().getContactNumber(),
                        r.getRoom().getRoomId(),
                        r.getTotalAmount(),
                        r.isPaid() ? "PAID" : "OUTSTANDING",
                        r.getStatus());
                anyHighValue = true;
            }
        }
        if (!anyHighValue) {
            System.out.println("  No high-value records (> RM 1,000.00) in this report.");
        }

        ui.printReportFooter(results.length);
        ui.pressEnterToContinue();
    }

    // =========================================================================
    // HELPER / UTILITY METHODS
    // =========================================================================

    private Room findRoomById(String roomId) {
        if (roomId == null) return null;
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
            Room r = roomList.getEntry(i);
            if (r.getRoomId().equalsIgnoreCase(roomId)) {
                return r;
            }
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
