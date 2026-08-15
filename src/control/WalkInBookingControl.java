package control;

import adt.ArrayList;
import adt.LinkedQueue;
import adt.ListInterface;
import adt.QueueInterface;
import boundary.WalkInBookingUI;
import dao.HousekeepingDAO;
import dao.WalkInBookingDAO;
import entity.Guest;
import entity.Room;
import entity.WalkInBooking;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller class for Walk-In Registrations & Standard Booking Procedure.
 * Manages chronological arrivals using custom Linear Queue ADT (`LinkedQueue`).
 * Adheres strictly to ECB architecture constraints.
 * 
 * @author Chan Shao Lun
 */
public class WalkInBookingControl {

    private QueueInterface<WalkInBooking> waitingQueue = new LinkedQueue<>();
    private ListInterface<WalkInBooking> allBookings = new ArrayList<>();
    private ListInterface<Room> roomList = new ArrayList<>();

    private WalkInBookingDAO bookingDAO = new WalkInBookingDAO();
    private HousekeepingDAO housekeepingDAO = new HousekeepingDAO();
    private WalkInBookingUI ui = new WalkInBookingUI();

    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private int nextBookingIdSuffix = 1001;

    public WalkInBookingControl() {
        // Load persistent data
        allBookings = bookingDAO.retrieveBookingsFromFile();
        roomList = housekeepingDAO.retrieveRoomsFromFile();

        if (roomList.isEmpty()) {
            initDefaultRooms();
        }

        rebuildWaitingQueue();
    }

    private void initDefaultRooms() {
        // Floor 1: 1 Suite Room, 2 Deluxe Rooms, 2 Standard Rooms
        roomList.add(new Room("R101", "Suite Room",    1, "Dirty",             "UNASSIGNED"));
        roomList.add(new Room("R102", "Deluxe Room",   1, "Cleaning In Progress", "ST101"));
        roomList.add(new Room("R103", "Deluxe Room",   1, "Inspected",         "ST101"));
        roomList.add(new Room("R104", "Standard Room", 1, "Ready for Check-In","ST102"));
        roomList.add(new Room("R105", "Standard Room", 1, "Ready for Check-In","ST102"));
        // Floor 2: 1 Suite Room, 2 Deluxe Rooms, 2 Standard Rooms
        roomList.add(new Room("R201", "Suite Room",    2, "Dirty",             "UNASSIGNED"));
        roomList.add(new Room("R202", "Deluxe Room",   2, "Cleaning In Progress", "ST103"));
        roomList.add(new Room("R203", "Deluxe Room",   2, "Inspected",         "ST103"));
        roomList.add(new Room("R204", "Standard Room", 2, "Ready for Check-In","ST103"));
        roomList.add(new Room("R205", "Standard Room", 2, "Ready for Check-In","ST103"));
        // Floor 3: 1 Suite Room, 2 Deluxe Rooms, 2 Standard Rooms
        roomList.add(new Room("R301", "Suite Room",    3, "Dirty",             "ST104"));
        roomList.add(new Room("R302", "Deluxe Room",   3, "Cleaning In Progress", "ST104"));
        roomList.add(new Room("R303", "Deluxe Room",   3, "Inspected",         "ST104"));
        roomList.add(new Room("R304", "Standard Room", 3, "Ready for Check-In","ST104"));
        roomList.add(new Room("R305", "Standard Room", 3, "Ready for Check-In","ST104"));
        housekeepingDAO.saveRoomsToFile(roomList);
    }

    private void rebuildWaitingQueue() {
        waitingQueue.clear();
        for (int i = 1; i <= allBookings.getNumberOfEntries(); i++) {
            WalkInBooking b = allBookings.getEntry(i);
            if ("WAITING".equalsIgnoreCase(b.getStatus())) {
                waitingQueue.enqueue(b);
            }
        }
        updateNextBookingIdSuffix();
    }

    private void updateNextBookingIdSuffix() {
        int maxSuffix = 1000;
        for (int i = 1; i <= allBookings.getNumberOfEntries(); i++) {
            WalkInBooking b = allBookings.getEntry(i);
            if (b != null && b.getBookingId() != null && b.getBookingId().toUpperCase().startsWith("WB")) {
                try {
                    int num = Integer.parseInt(b.getBookingId().substring(2));
                    if (num > maxSuffix) {
                        maxSuffix = num;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        nextBookingIdSuffix = maxSuffix + 1;
    }

    public void runWalkInBookingSystem() {
        // Reload latest room data from shared rooms.dat each time subsystem is entered
        // so changes made by Housekeeping or Front-Desk are reflected
        ListInterface<Room> latestRooms = housekeepingDAO.retrieveRoomsFromFile();
        if (latestRooms != null && !latestRooms.isEmpty()) {
            roomList = latestRooms;
        }
        int choice;
        do {
            choice = ui.getMenuChoice();
            switch (choice) {
                case 1:
                    registerNewBooking();
                    break;
                case 2:
                    viewWaitingQueue();
                    break;
                case 3:
                    allocateRoomToNextGuest();
                    break;
                case 4:
                    cancelOrModifyBooking();
                    break;
                case 5:
                    searchBookings();
                    break;
                case 6:
                    generateQueueEfficiencyReport();
                    break;
                case 7:
                    generateChannelPerformanceReport();
                    break;
                case 0:
                    ui.displayMessage("Returning to Main System Menu.");
                    break;
                default:
                    ui.displayMessage("Invalid selection. Please try again.");
            }
        } while (choice != 0);
    }

    // =========================================================================
    // CORE USE CASES (LINEAR QUEUE ADT OPERATIONAL LOGIC)
    // =========================================================================

    private void registerNewBooking() {
        ui.displayHeader("REGISTER NEW WALK-IN / STANDARD BOOKING");

        int channelChoice = ui.inputBookingTypeChoice();
        if (channelChoice == 0) {
            ui.displayMessage("Registration cancelled.");
            return;
        }
        String bookingType = (channelChoice == 2) ? "Standard Advance" : "Walk-In";

        String ic = ui.inputRequiredText("Enter Guest IC Number (e.g., 990101-14-1234) [0 to Back]: ",
                "IC Number cannot be empty. Please try again.");
        if ("0".equals(ic)) {
            ui.displayMessage("Registration cancelled.");
            return;
        }

        String name = ui.inputRequiredText("Enter Guest Full Name [0 to Back]: ",
                "Guest name cannot be empty. Please try again.");
        if ("0".equals(name)) {
            ui.displayMessage("Registration cancelled.");
            return;
        }

        String contact = ui.inputRequiredText("Enter Contact Number (e.g., 012-3456789) [0 to Back]: ",
                "Contact number cannot be empty. Please try again.");
        if ("0".equals(contact)) {
            ui.displayMessage("Registration cancelled.");
            return;
        }
        Guest guest = new Guest(ic, name, contact);

        String roomType = ui.inputRoomType();
        if ("CANCEL".equalsIgnoreCase(roomType)) {
            ui.displayMessage("Registration cancelled.");
            return;
        }
        double rate = getRateForRoomType(roomType);

        int nights;
        while (true) {
            nights = ui.inputInt("Enter Number of Nights to Stay [0 to Back]: ");
            if (nights == 0) {
                ui.displayMessage("Registration cancelled.");
                return;
            }
            if (nights > 0) {
                break;
            }
            ui.displayMessage("Invalid stay duration! Number of nights must be greater than 0.");
        }

        String bookingId = "WB" + (nextBookingIdSuffix++);
        String timestamp = LocalDateTime.now().format(dtf);

        WalkInBooking newBooking = new WalkInBooking(
                bookingId, guest, roomType, nights, rate, timestamp, bookingType, "WAITING");

        // Linear ADT Enqueue (FIFO placement)
        waitingQueue.enqueue(newBooking);
        allBookings.add(newBooking);
        bookingDAO.saveBookingsToFile(allBookings);

        ui.displayMessage(String.format(
                "Successfully Registered Arrival!\n" +
                        "Booking ID   : %s\n" +
                        "Guest Name   : %s\n" +
                        "Channel      : %s\n" +
                        "Room Type    : %s\n" +
                        "Queue Position: %d (Enqueued in FIFO order)",
                bookingId, name, bookingType, roomType, waitingQueue.size()));
        ui.pressEnterToContinue();
    }

    private void viewWaitingQueue() {
        ui.displayHeader("PENDING CHRONOLOGICAL WAITING QUEUE (LINEAR QUEUE ADT)");

        if (waitingQueue.isEmpty()) {
            ui.displayMessage("The waiting queue is currently empty. No pending guests.");
            return;
        }

        // Convert Queue to List without modifying queue structure
        LinkedQueue<WalkInBooking> lq = (LinkedQueue<WalkInBooking>) waitingQueue;
        ListInterface<WalkInBooking> queueList = lq.toList();

        System.out.printf("%-6s | %-10s | %-18s | %-15s | %-10s | %-10s | %s%n",
                "Pos", "Booking ID", "Guest Name", "Channel", "Room Type", "Nights", "Registration Time");
        System.out.println(
                "-----------------------------------------------------------------------------------------------------");

        for (int i = 1; i <= queueList.getNumberOfEntries(); i++) {
            WalkInBooking b = queueList.getEntry(i);
            System.out.printf("#%-5d | %-10s | %-18s | %-15s | %-10s | %-10d | %s%n",
                    i, b.getBookingId(), b.getGuest().getName(), b.getBookingType(),
                    b.getRequestedRoomType(), b.getNumberOfNights(), b.getRegistrationTime());
        }
        System.out.println(
                "-----------------------------------------------------------------------------------------------------");
        System.out.printf("Total Waiting Guests in Queue: %d%n", queueList.getNumberOfEntries());

        ui.pressEnterToContinue();
    }

    private void allocateRoomToNextGuest() {
        ui.displayHeader("ALLOCATE ROOM TO NEXT WAITING GUEST (DEQUEUE & ASSIGN)");

        if (waitingQueue.isEmpty()) {
            ui.displayMessage("Waiting queue is empty. No pending guests to allocate.");
            return;
        }

        WalkInBooking nextBooking = waitingQueue.getFront(); // Peek front guest
        System.out.println("\n--- CURRENT SYSTEM ROOM TABLE ---");
        boundary.HousekeepingUI.displayRoomTable(roomList);

        System.out.println("\nNext Guest at Front of Queue:");
        System.out.println(nextBooking.toDetailString());

        // Search for an available room matching requested room type
        Room matchedRoom = null;
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
            Room r = roomList.getEntry(i);
            if (isRoomTypeMatch(r.getRoomType(), nextBooking.getRequestedRoomType()) &&
                    "Ready for Check-In".equalsIgnoreCase(r.getCurrentStatus())) {
                matchedRoom = r;
                break;
            }
        }

        if (matchedRoom == null) {
            ui.displayMessage("No room of type '" + nextBooking.getRequestedRoomType()
                    + "' is currently 'Ready for Check-In'.\n" +
                    "Guest remains at the front of the queue until housekeeping releases a matching room.");
            ui.pressEnterToContinue();
            return;
        }

        if (ui.confirmAction(
                "Assign Room " + matchedRoom.getRoomId() + " to " + nextBooking.getGuest().getName() + "?")) {
            // Dequeue from Queue Linear ADT
            waitingQueue.dequeue();

            // Update entity states
            matchedRoom.setCurrentStatus("Occupied");
            nextBooking.setAssignedRoom(matchedRoom);
            nextBooking.setStatus("ALLOCATED");

            // Register active stay into Front-Desk Non-Linear ADT Map (reservations.dat)
            dao.FrontDeskDAO frontDeskDAO = new dao.FrontDeskDAO();
            adt.MapInterface<String, entity.Reservation> resMap = frontDeskDAO.retrieveReservationsFromFile();
            if (resMap == null) {
                resMap = new adt.HashMap<>();
            }
            String confNum = String.format("2026%04d", (allBookings.getNumberOfEntries() * 19 + 1000) % 9000 + 1000);
            java.time.LocalDate inDate = java.time.LocalDate.now();
            java.time.LocalDate outDate = inDate.plusDays(nextBooking.getNumberOfNights());
            entity.Reservation newRes = new entity.Reservation(confNum, nextBooking.getGuest(), matchedRoom,
                    inDate.toString(), outDate.toString(), nextBooking.getEstimatedRatePerNight(), "Checked-In");
            resMap.put(confNum, newRes);
            frontDeskDAO.saveReservationsToFile(resMap);

            // Persist changes
            bookingDAO.saveBookingsToFile(allBookings);
            housekeepingDAO.saveRoomsToFile(roomList);

            ui.displayMessage(String.format(
                    "ROOM ALLOCATION & REGISTRATION SUCCESSFUL!\n" +
                    "  Guest Name          : %s\n" +
                    "  Booking ID          : %s\n" +
                    "  Confirmation Number : %s\n" +
                    "  Room Assigned       : %s (%s, Floor %d)\n" +
                    "  Stay Duration       : %d night(s)\n" +
                    "  Remaining in Queue  : %d",
                    nextBooking.getGuest().getName(), nextBooking.getBookingId(),
                    confNum,
                    matchedRoom.getRoomId(), matchedRoom.getRoomType(), matchedRoom.getFloorNumber(),
                    nextBooking.getNumberOfNights(),
                    waitingQueue.size()));
        } else {
            ui.displayMessage("Allocation cancelled. Guest remains in queue.");
        }

        ui.pressEnterToContinue();
    }

    private void cancelOrModifyBooking() {
        ui.displayHeader("CANCEL OR MODIFY PENDING REGISTRATION");

        String queryId = ui.inputText("Enter Booking ID to Cancel/Modify (e.g., WB1001) [0 to Back]: ");
        if (queryId.isEmpty() || "0".equals(queryId))
            return;

        WalkInBooking booking = findBookingById(queryId);
        if (booking == null) {
            ui.displayMessage("Booking ID '" + queryId + "' not found.");
            return;
        }

        System.out.println("\nBooking Details Found:");
        System.out.println(booking.toDetailString());

        if (!"WAITING".equalsIgnoreCase(booking.getStatus())) {
            ui.displayMessage("Only pending ('WAITING') registrations can be modified/cancelled. Current status: "
                    + booking.getStatus());
            return;
        }

        System.out.println("\nSelect Action:");
        System.out.println(" [1] Cancel Registration");
        System.out.println(" [2] Modify Requested Room Type");
        System.out.println(" [0] Go Back");
        int choice = ui.inputIntInRange("Select [0-2]: ", 0, 2);
        if (choice == 0)
            return;

        if (choice == 1) {
            if (ui.confirmAction("Cancel registration for " + booking.getBookingId() + "?")) {
                booking.setStatus("CANCELLED");
                rebuildWaitingQueue();
                bookingDAO.saveBookingsToFile(allBookings);
                ui.displayMessage("Registration " + booking.getBookingId() + " successfully cancelled.");
            }
        } else if (choice == 2) {
            String newRoomType = ui.inputRoomType();
            if (!"CANCEL".equalsIgnoreCase(newRoomType)) {
                booking.setRequestedRoomType(newRoomType);
                booking.setEstimatedRatePerNight(getRateForRoomType(newRoomType));
                bookingDAO.saveBookingsToFile(allBookings);
                ui.displayMessage("Room type updated to '" + newRoomType + "' for " + booking.getBookingId() + ".");
            }
        }
    }

    // =========================================================================
    // EXPLICIT SEARCHING & SORTING ALGORITHMS
    // =========================================================================

    private void searchBookings() {
        ui.displayHeader("SEARCH REGISTRATIONS (EXPLICIT SEARCHING ALGORITHMS)");

        System.out.println("Search Criteria Options:");
        System.out.println(" [1] Exact Search by Booking ID (Linear Search)");
        System.out.println(" [2] Search by Guest IC Number (Linear Search)");
        System.out.println(" [3] Search by Guest Name Keyword (Sub-string Search)");
        System.out.println(" [4] Binary Search by Booking ID (Requires Sorted Array)");
        System.out.println(" [0] Back to Walk-In Booking Menu");
        System.out.print("Select Search Option [0-4]: ");
        int choice = ui.inputIntInRange("Select Search Option [0-4]: ", 0, 4);

        if (choice == 0)
            return;

        if (choice == 1) {
            String id = ui.inputRequiredText("Enter Booking ID [0 to Back]: ", "Booking ID cannot be empty.");
            if ("0".equals(id))
                return;
            WalkInBooking found = findBookingById(id);
            displaySearchResult(found);
        } else if (choice == 2) {
            String ic = ui.inputRequiredText("Enter Guest IC Number [0 to Back]: ", "IC Number cannot be empty.");
            if ("0".equals(ic))
                return;
            WalkInBooking found = findBookingByIC(ic);
            displaySearchResult(found);
        } else if (choice == 3) {
            String nameKey = ui.inputRequiredText("Enter Guest Name Keyword [0 to Back]: ",
                    "Search keyword cannot be empty.");
            if ("0".equals(nameKey))
                return;
            ListInterface<WalkInBooking> results = searchBookingsByName(nameKey);
            displaySearchResultsList(results);
        } else if (choice == 4) {
            String id = ui.inputRequiredText("Enter Booking ID for Binary Search [0 to Back]: ",
                    "Booking ID cannot be empty.");
            if ("0".equals(id))
                return;
            WalkInBooking found = binarySearchById(id);
            displaySearchResult(found);
        } else {
            ui.displayMessage("Invalid search option.");
            return;
        }

        ui.pressEnterToContinue();
    }

    /**
     * Sequential Linear Search by Booking ID.
     */
    private WalkInBooking findBookingById(String bookingId) {
        for (int i = 1; i <= allBookings.getNumberOfEntries(); i++) {
            WalkInBooking b = allBookings.getEntry(i);
            if (b.getBookingId().equalsIgnoreCase(bookingId.trim())) {
                return b;
            }
        }
        return null;
    }

    /**
     * Sequential Linear Search by Guest IC.
     */
    private WalkInBooking findBookingByIC(String ic) {
        for (int i = 1; i <= allBookings.getNumberOfEntries(); i++) {
            WalkInBooking b = allBookings.getEntry(i);
            if (b.getGuest().getIcNumber().equalsIgnoreCase(ic.trim())) {
                return b;
            }
        }
        return null;
    }

    /**
     * Sub-string Search by Guest Name.
     */
    private ListInterface<WalkInBooking> searchBookingsByName(String keyword) {
        ListInterface<WalkInBooking> results = new ArrayList<>();
        String lowerKey = keyword.trim().toLowerCase();
        for (int i = 1; i <= allBookings.getNumberOfEntries(); i++) {
            WalkInBooking b = allBookings.getEntry(i);
            if (b.getGuest().getName().toLowerCase().contains(lowerKey)) {
                results.add(b);
            }
        }
        return results;
    }

    /**
     * Binary Search Algorithm by Booking ID O(log n).
     */
    private WalkInBooking binarySearchById(String bookingId) {
        // Create copy and sort by Booking ID using Insertion Sort
        ListInterface<WalkInBooking> sortedList = copyList(allBookings);
        insertionSortById(sortedList);

        int low = 1;
        int high = sortedList.getNumberOfEntries();
        String target = bookingId.trim().toUpperCase();

        while (low <= high) {
            int mid = low + (high - low) / 2;
            WalkInBooking midBooking = sortedList.getEntry(mid);
            int comp = midBooking.getBookingId().compareToIgnoreCase(target);

            if (comp == 0) {
                return midBooking;
            } else if (comp < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return null;
    }

    private void displaySearchResult(WalkInBooking b) {
        if (b != null) {
            ui.displayMessage("Search Result Found:\n" + b.toDetailString());
        } else {
            ui.displayMessage("No matching registration found.");
        }
    }

    private void displaySearchResultsList(ListInterface<WalkInBooking> list) {
        if (list.isEmpty()) {
            ui.displayMessage("No matching registrations found.");
            return;
        }
        System.out.printf("%-10s | %-18s | %-15s | %-12s | %-10s | %s%n",
                "Booking ID", "Guest Name", "Channel", "Room Type", "Status", "Room Assigned");
        System.out.println("-----------------------------------------------------------------------------------------");
        for (int i = 1; i <= list.getNumberOfEntries(); i++) {
            WalkInBooking b = list.getEntry(i);
            System.out.printf("%-10s | %-18s | %-15s | %-12s | %-10s | %s%n",
                    b.getBookingId(), b.getGuest().getName(), b.getBookingType(),
                    b.getRequestedRoomType(), b.getStatus(),
                    (b.getAssignedRoom() != null ? b.getAssignedRoom().getRoomId() : "N/A"));
        }
        System.out.println("-----------------------------------------------------------------------------------------");
    }

    // =========================================================================
    // MANAGEMENT ANALYTICAL REPORTS
    // =========================================================================

    /**
     * Management Report 1: Peak Season Queue Allocation & Efficiency Summary
     * Combines search/sort, filters by status/room type, displays allocation
     * efficiency metrics.
     */
    private void generateQueueEfficiencyReport() {
        ui.printReportHeader("PEAK SEASON QUEUE ALLOCATION & ROOM DEMAND EFFICIENCY SUMMARY");

        System.out.println("Filter Options:");
        System.out.println(" [1] All Registrations");
        System.out.println(" [2] Pending WAITING Guests Only");
        System.out.println(" [3] ALLOCATED Guests Only");
        System.out.println(" [0] Back to Walk-In Booking Menu");
        System.out.print("Select Filter Criteria [0-3]: ");
        int filterChoice = ui.inputIntInRange("Select Filter Criteria [0-3]: ", 0, 3);
        if (filterChoice == 0)
            return;

        ListInterface<WalkInBooking> filteredList = new ArrayList<>();
        for (int i = 1; i <= allBookings.getNumberOfEntries(); i++) {
            WalkInBooking b = allBookings.getEntry(i);
            if (filterChoice == 2 && !"WAITING".equalsIgnoreCase(b.getStatus()))
                continue;
            if (filterChoice == 3 && !"ALLOCATED".equalsIgnoreCase(b.getStatus()))
                continue;
            filteredList.add(b);
        }

        if (filteredList.isEmpty()) {
            ui.displayMessage("No records match the selected filter criteria.");
            return;
        }

        // Explicit Insertion Sort by Registration Time (Chronological order)
        insertionSortByTime(filteredList);

        int totalWaiting = 0;
        int totalAllocated = 0;
        int totalCancelled = 0;
        double totalEstRevenue = 0.0;

        System.out.printf("%-10s | %-18s | %-15s | %-12s | %-10s | RM %-8s | %s%n",
                "Booking ID", "Guest Name", "Channel", "Room Type", "Status", "Est Cost", "Registration Time");
        System.out.println(
                "---------------------------------------------------------------------------------------------------------");

        for (int i = 1; i <= filteredList.getNumberOfEntries(); i++) {
            WalkInBooking b = filteredList.getEntry(i);
            if ("WAITING".equalsIgnoreCase(b.getStatus()))
                totalWaiting++;
            else if ("ALLOCATED".equalsIgnoreCase(b.getStatus()))
                totalAllocated++;
            else if ("CANCELLED".equalsIgnoreCase(b.getStatus()))
                totalCancelled++;

            totalEstRevenue += b.getTotalEstimatedCost();

            System.out.printf("%-10s | %-18s | %-15s | %-12s | %-10s | RM %-8.2f | %s%n",
                    b.getBookingId(), b.getGuest().getName(), b.getBookingType(),
                    b.getRequestedRoomType(), b.getStatus(), b.getTotalEstimatedCost(), b.getRegistrationTime());
        }
        System.out.println(
                "---------------------------------------------------------------------------------------------------------");

        double allocationRate = (filteredList.getNumberOfEntries() > 0)
                ? ((double) totalAllocated / filteredList.getNumberOfEntries()) * 100.0
                : 0.0;

        System.out.println("\n--- QUEUE OPERATIONAL METRICS ---");
        System.out.printf("  Pending Queue (Waiting) : %d guest(s)%n", totalWaiting);
        System.out.printf("  Successfully Allocated   : %d guest(s)%n", totalAllocated);
        System.out.printf("  Cancelled Registrations  : %d guest(s)%n", totalCancelled);
        System.out.printf("  Queue Allocation Rate    : %.2f%%%n", allocationRate);
        System.out.printf("  Total Projected Revenue  : RM %.2f%n", totalEstRevenue);

        ui.printReportFooter(filteredList.getNumberOfEntries());
        ui.pressEnterToContinue();
    }

    /**
     * Management Report 2: Booking Channel Performance & Financial Forecast
     * Combines multi-criteria filtering, Bubble sort by revenue/nights, financial
     * projections.
     */
    private void generateChannelPerformanceReport() {
        ui.printReportHeader("BOOKING CHANNEL PERFORMANCE & REVENUE CONTRIBUTION FORECAST");

        System.out.println("Filter by Booking Channel:");
        System.out.println(" [1] All Channels (Walk-In & Standard Advance)");
        System.out.println(" [2] Walk-In Guests Only");
        System.out.println(" [3] Standard Advance Bookings Only");
        System.out.println(" [0] Back to Walk-In Booking Menu");
        System.out.print("Select Filter [0-3]: ");
        int channelFilter = ui.inputIntInRange("Select Filter [0-3]: ", 0, 3);
        if (channelFilter == 0)
            return;

        ListInterface<WalkInBooking> filteredList = new ArrayList<>();
        for (int i = 1; i <= allBookings.getNumberOfEntries(); i++) {
            WalkInBooking b = allBookings.getEntry(i);
            if (channelFilter == 2 && !"Walk-In".equalsIgnoreCase(b.getBookingType()))
                continue;
            if (channelFilter == 3 && !"Standard Advance".equalsIgnoreCase(b.getBookingType()))
                continue;
            filteredList.add(b);
        }

        if (filteredList.isEmpty()) {
            ui.displayMessage("No records found for the selected channel filter.");
            return;
        }

        // Explicit Bubble Sort by Total Estimated Revenue Descending
        bubbleSortByRevenueDescending(filteredList);

        int countWalkIn = 0;
        int countAdvance = 0;
        double revWalkIn = 0.0;
        double revAdvance = 0.0;
        int totalNights = 0;

        System.out.printf("%-10s | %-18s | %-15s | %-12s | %-6s | RM %-10s | %s%n",
                "Booking ID", "Guest Name", "Channel", "Room Type", "Nights", "Total Cost", "Status");
        System.out.println(
                "---------------------------------------------------------------------------------------------------------");

        for (int i = 1; i <= filteredList.getNumberOfEntries(); i++) {
            WalkInBooking b = filteredList.getEntry(i);
            if ("Walk-In".equalsIgnoreCase(b.getBookingType())) {
                countWalkIn++;
                revWalkIn += b.getTotalEstimatedCost();
            } else {
                countAdvance++;
                revAdvance += b.getTotalEstimatedCost();
            }
            totalNights += b.getNumberOfNights();

            System.out.printf("%-10s | %-18s | %-15s | %-12s | %-6d | RM %-10.2f | %s%n",
                    b.getBookingId(), b.getGuest().getName(), b.getBookingType(),
                    b.getRequestedRoomType(), b.getNumberOfNights(), b.getTotalEstimatedCost(), b.getStatus());
        }
        System.out.println(
                "---------------------------------------------------------------------------------------------------------");

        double avgNights = (filteredList.getNumberOfEntries() > 0)
                ? (double) totalNights / filteredList.getNumberOfEntries()
                : 0.0;

        System.out.println("\n--- FINANCIAL & CHANNEL PERFORMANCE BREAKDOWN ---");
        System.out.printf("  Walk-In Volume           : %d booking(s) | Projected Revenue: RM %.2f%n", countWalkIn,
                revWalkIn);
        System.out.printf("  Standard Advance Volume  : %d booking(s) | Projected Revenue: RM %.2f%n", countAdvance,
                revAdvance);
        System.out.printf("  Combined Total Revenue   : RM %.2f%n", (revWalkIn + revAdvance));
        System.out.printf("  Average Length of Stay   : %.1f night(s)%n", avgNights);

        ui.printReportFooter(filteredList.getNumberOfEntries());
        ui.pressEnterToContinue();
    }

    // =========================================================================
    // EXPLICIT SORTING UTILITIES
    // =========================================================================

    private void insertionSortById(ListInterface<WalkInBooking> list) {
        int n = list.getNumberOfEntries();
        for (int i = 2; i <= n; i++) {
            WalkInBooking key = list.getEntry(i);
            int j = i - 1;
            while (j >= 1 && list.getEntry(j).getBookingId().compareToIgnoreCase(key.getBookingId()) > 0) {
                list.replace(j + 1, list.getEntry(j));
                j--;
            }
            list.replace(j + 1, key);
        }
    }

    private void insertionSortByTime(ListInterface<WalkInBooking> list) {
        int n = list.getNumberOfEntries();
        for (int i = 2; i <= n; i++) {
            WalkInBooking key = list.getEntry(i);
            int j = i - 1;
            while (j >= 1 && list.getEntry(j).getRegistrationTime().compareTo(key.getRegistrationTime()) > 0) {
                list.replace(j + 1, list.getEntry(j));
                j--;
            }
            list.replace(j + 1, key);
        }
    }

    private void bubbleSortByRevenueDescending(ListInterface<WalkInBooking> list) {
        int n = list.getNumberOfEntries();
        for (int i = 1; i < n; i++) {
            for (int j = 1; j <= n - i; j++) {
                WalkInBooking b1 = list.getEntry(j);
                WalkInBooking b2 = list.getEntry(j + 1);
                if (b1.getTotalEstimatedCost() < b2.getTotalEstimatedCost()) {
                    list.replace(j, b2);
                    list.replace(j + 1, b1);
                }
            }
        }
    }

    private ListInterface<WalkInBooking> copyList(ListInterface<WalkInBooking> original) {
        ListInterface<WalkInBooking> copy = new ArrayList<>(original.getNumberOfEntries());
        for (int i = 1; i <= original.getNumberOfEntries(); i++) {
            copy.add(original.getEntry(i));
        }
        return copy;
    }

    private double getRateForRoomType(String roomType) {
        if (roomType != null && roomType.toLowerCase().contains("deluxe"))
            return 350.00;
        if (roomType != null && roomType.toLowerCase().contains("suite"))
            return 600.00;
        return 200.00; // Standard Room
    }

    private boolean isRoomTypeMatch(String type1, String type2) {
        if (type1 == null || type2 == null)
            return false;
        String t1 = type1.toLowerCase().replace("room", "").trim();
        String t2 = type2.toLowerCase().replace("room", "").trim();
        return t1.equals(t2);
    }
}
