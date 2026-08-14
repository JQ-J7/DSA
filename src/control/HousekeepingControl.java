package control;

import adt.ArrayList;
import adt.ListInterface;
import boundary.HousekeepingUI;
import dao.HousekeepingDAO;
import entity.HousekeepingTask;
import entity.Room;
import entity.TaskStatusHistory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import utility.MessageUI;

/**
 * Control Class orchestrating Housekeeping and Task Log management.
 * Implements business logic, rollback execution using Stack ADT,
 * explicit sorting & searching algorithms, and management report generation.
 * 
 * @author Tan Jun Qi
 */
public class HousekeepingControl {

    private ListInterface<Room> roomList = new ArrayList<>();
    private ListInterface<HousekeepingTask> taskList = new ArrayList<>();
    private HousekeepingDAO dao = new HousekeepingDAO();
    private HousekeepingUI ui = new HousekeepingUI();
    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public HousekeepingControl() {
        // Load data from binary files
        roomList = dao.retrieveRoomsFromFile();
        taskList = dao.retrieveTasksFromFile();

        // Deduplicate loaded data
        deduplicateData();

        // Ensure constant 15 rooms (R101-R305) exist
        sanitizeConstantRooms();

        // Ensure default tasks exist
        if (taskList.isEmpty()) {
            addDefaultDemoTasks();
        }
    }

    private void deduplicateData() {
        boolean roomsChanged = false;
        ListInterface<Room> cleanRoomList = new ArrayList<>();
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
            Room r = roomList.getEntry(i);
            boolean exists = false;
            for (int j = 1; j <= cleanRoomList.getNumberOfEntries(); j++) {
                if (cleanRoomList.getEntry(j).getRoomId().equalsIgnoreCase(r.getRoomId())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                cleanRoomList.add(r);
            } else {
                roomsChanged = true;
            }
        }
        if (roomsChanged) {
            roomList = cleanRoomList;
            dao.saveRoomsToFile(roomList);
        }

        boolean tasksChanged = false;
        ListInterface<HousekeepingTask> cleanTaskList = new ArrayList<>();
        for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
            HousekeepingTask t = taskList.getEntry(i);
            boolean exists = false;
            for (int j = 1; j <= cleanTaskList.getNumberOfEntries(); j++) {
                if (cleanTaskList.getEntry(j).getTaskId().equalsIgnoreCase(t.getTaskId())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                cleanTaskList.add(t);
            } else {
                tasksChanged = true;
            }
        }
        if (tasksChanged) {
            taskList = cleanTaskList;
            dao.saveTasksToFile(taskList);
        }
    }

    private void sanitizeConstantRooms() {
        addDefaultDemoRooms();
        ListInterface<Room> sanitizedList = new ArrayList<>();
        boolean changed = false;
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
            Room r = roomList.getEntry(i);
            if (isValidConstantRoomId(r.getRoomId())) {
                sanitizedList.add(r);
            } else {
                changed = true;
            }
        }
        if (changed || sanitizedList.getNumberOfEntries() < 15) {
            roomList = sanitizedList;
            addDefaultDemoRooms();
        }
        dao.saveRoomsToFile(roomList);
    }

    private boolean isValidConstantRoomId(String roomId) {
        if (roomId == null) return false;
        String id = roomId.toUpperCase();
        return id.matches("R[1-3]0[1-5]");
    }

    private void initializeDemoData() {
        roomList.clear();
        taskList.clear();
        addDefaultDemoRooms();
        addDefaultDemoTasks();
    }

    private void addDefaultDemoRooms() {
        Room[] constantRooms = new Room[] {
            // Floor 1: 1 Presidential Suite, 2 Executive Suites, 2 Standard Deluxe
            new Room("R101", "Presidential Suite", 1, "Dirty", "ST101"),
            new Room("R102", "Executive Suite", 1, "Cleaning In Progress", "ST102"),
            new Room("R103", "Executive Suite", 1, "Inspected", "ST103"),
            new Room("R104", "Standard Deluxe", 1, "Ready for Check-In", "ST104"),
            new Room("R105", "Standard Deluxe", 1, "Ready for Check-In", "ST105"),

            // Floor 2: 1 Presidential Suite, 2 Executive Suites, 2 Standard Deluxe
            new Room("R201", "Presidential Suite", 2, "Dirty", "ST201"),
            new Room("R202", "Executive Suite", 2, "Cleaning In Progress", "ST202"),
            new Room("R203", "Executive Suite", 2, "Inspected", "ST203"),
            new Room("R204", "Standard Deluxe", 2, "Ready for Check-In", "ST204"),
            new Room("R205", "Standard Deluxe", 2, "Ready for Check-In", "ST205"),

            // Floor 3: 1 Presidential Suite, 2 Executive Suites, 2 Standard Deluxe
            new Room("R301", "Presidential Suite", 3, "Dirty", "ST301"),
            new Room("R302", "Executive Suite", 3, "Cleaning In Progress", "ST302"),
            new Room("R303", "Executive Suite", 3, "Inspected", "ST303"),
            new Room("R304", "Standard Deluxe", 3, "Ready for Check-In", "ST304"),
            new Room("R305", "Standard Deluxe", 3, "Ready for Check-In", "ST305")
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
        dao.saveRoomsToFile(roomList);
    }

    private void addDefaultDemoTasks() {
        String now = LocalDateTime.now().format(dtf);
        int taskCounter = 1001;

        Room[] defaultRooms = new Room[] {
            new Room("R101", "Presidential Suite", 1, "Dirty", "ST101"),
            new Room("R102", "Executive Suite", 1, "Cleaning In Progress", "ST102"),
            new Room("R103", "Executive Suite", 1, "Inspected", "ST103"),
            new Room("R104", "Standard Deluxe", 1, "Ready for Check-In", "ST104"),
            new Room("R105", "Standard Deluxe", 1, "Ready for Check-In", "ST105"),
            new Room("R201", "Presidential Suite", 2, "Dirty", "ST201"),
            new Room("R202", "Executive Suite", 2, "Cleaning In Progress", "ST202"),
            new Room("R203", "Executive Suite", 2, "Inspected", "ST203"),
            new Room("R204", "Standard Deluxe", 2, "Ready for Check-In", "ST204"),
            new Room("R205", "Standard Deluxe", 2, "Ready for Check-In", "ST205"),
            new Room("R301", "Presidential Suite", 3, "Dirty", "ST301"),
            new Room("R302", "Executive Suite", 3, "Cleaning In Progress", "ST302"),
            new Room("R303", "Executive Suite", 3, "Inspected", "ST303"),
            new Room("R304", "Standard Deluxe", 3, "Ready for Check-In", "ST304"),
            new Room("R305", "Standard Deluxe", 3, "Ready for Check-In", "ST305")
        };

        for (Room r : defaultRooms) {
            String taskId = "TSK-" + taskCounter++;
            if (findActiveTaskByRoomId(r.getRoomId()) == null) {
                HousekeepingTask task = new HousekeepingTask(taskId, r.getRoomId(), r.getAssignedStaffId(), "Dirty", now);
                if ("Cleaning In Progress".equals(r.getCurrentStatus())) {
                    task.updateStatus("Cleaning In Progress", r.getAssignedStaffId(), now, "Staff started cleaning");
                } else if ("Inspected".equals(r.getCurrentStatus())) {
                    task.updateStatus("Cleaning In Progress", r.getAssignedStaffId(), now, "Staff started cleaning");
                    task.updateStatus("Inspected", "SUP-01", now, "Supervisor inspection completed");
                } else if ("Ready for Check-In".equals(r.getCurrentStatus())) {
                    task.updateStatus("Cleaning In Progress", r.getAssignedStaffId(), now, "Staff started cleaning");
                    task.updateStatus("Inspected", "SUP-01", now, "Supervisor inspection passed");
                    task.updateStatus("Ready for Check-In", "SUP-01", now, "Released for guest check-in");
                }
                taskList.add(task);
            }
        }
        dao.saveTasksToFile(taskList);
    }

    private HousekeepingTask findTaskById(String taskId) {
        for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
            HousekeepingTask t = taskList.getEntry(i);
            if (t.getTaskId().equalsIgnoreCase(taskId)) {
                return t;
            }
        }
        return null;
    }

    public void runHousekeepingSystem() {
        int choice;
        do {
            choice = ui.getMenuChoice();
            switch (choice) {
                case 1:
                    displayAllRooms();
                    break;
                case 2:
                    assignNewCleaningTask();
                    break;
                case 3:
                    updateCleaningStatus();
                    break;
                case 4:
                    rollbackTaskStatus();
                    break;
                case 5:
                    searchRoomOrTask();
                    break;
                case 6:
                    generateRoomEfficiencyReport();
                    break;
                case 7:
                    generateTaskRollbackAuditReport();
                    break;
                case 0:
                    MessageUI.displayExitMessage();
                    break;
                default:
                    MessageUI.displayInvalidChoiceMessage();
            }
        } while (choice != 0);
    }

    public void displayRoomTaskTable() {
        HousekeepingUI.displayRoomTable(roomList);
    }

    public ListInterface<Room> getRoomList() {
        return roomList;
    }

    public void displayAllRooms() {
        ui.displayHeader("ALL HOTEL ROOMS & TASK STATUSES (TABLE OF ROOM ID, STAFF ID & STATUS)");
        displayRoomTaskTable();
        ui.pressEnterToContinue();
    }

    public void displayStaffTable() {
        System.out.println("\n--------------------------------------------------------------------------------------------------");
        System.out.println("                                HOUSEKEEPING STAFF ROSTER TABLE                                  ");
        System.out.println("--------------------------------------------------------------------------------------------------");
        System.out.println(String.format("%-6s | %-10s | %-22s | %-24s | %-15s", 
                "No.", "Staff ID", "Staff Name", "Role / Designation", "Assigned Floor"));
        System.out.println("--------------------------------------------------------------------------------------------------");
        System.out.println(String.format("%-6d | %-10s | %-22s | %-24s | %-15s", 1, "ST101", "Ahmad Razali", "Senior Housekeeper", "Floor 1"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-24s | %-15s", 2, "ST102", "Siti Nurhaliza", "Housekeeping Attendant", "Floor 1"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-24s | %-15s", 3, "ST103", "Tan Ah Kow", "Housekeeping Attendant", "Floor 1"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-24s | %-15s", 4, "ST104", "Murali Vijay", "Junior Attendant", "Floor 1"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-24s | %-15s", 5, "ST105", "Lee Chong Wei", "Junior Attendant", "Floor 1"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-24s | %-15s", 6, "ST201", "Wong Mei Ling", "Senior Housekeeper", "Floor 2"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-24s | %-15s", 7, "ST202", "Devi Ananda", "Housekeeping Attendant", "Floor 2"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-24s | %-15s", 8, "ST203", "Kassim Selamat", "Housekeeping Attendant", "Floor 2"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-24s | %-15s", 9, "ST204", "Chan Xian Feng", "Junior Attendant", "Floor 2"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-24s | %-15s", 10, "ST205", "Kavitha Raj", "Junior Attendant", "Floor 2"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-24s | %-15s", 11, "ST301", "Subramaniam K", "Senior Housekeeper", "Floor 3"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-24s | %-15s", 12, "ST302", "Farida Begum", "Housekeeping Attendant", "Floor 3"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-24s | %-15s", 13, "ST303", "Jason Leong", "Housekeeping Attendant", "Floor 3"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-24s | %-15s", 14, "ST304", "Nurul Izzah", "Junior Attendant", "Floor 3"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-24s | %-15s", 15, "ST305", "Lim Guan Eng", "Junior Attendant", "Floor 3"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-24s | %-15s", 16, "SUP-01", "Rosmah Mansor", "Housekeeping Supervisor", "All Floors"));
        System.out.println("--------------------------------------------------------------------------------------------------");
    }

    public void assignNewCleaningTask() {
        ui.displayHeader("ASSIGN NEW HOUSEKEEPING CLEANING TASK");
        System.out.println("--- CURRENT ROOM ID, STAFF ID & STATUS TABLE ---");
        displayRoomTaskTable();
        System.out.println();

        String roomId = ui.inputRoomId();
        if (roomId.isEmpty()) {
            ui.displayMessage("Operation cancelled.");
            return;
        }

        // Check if room exists
        Room room = findRoomById(roomId);
        if (room == null) {
            ui.displayMessage("ERROR: Room " + roomId + " is unknown / not registered in the system! You cannot assign a task to an unknown room.");
            ui.pressEnterToContinue();
            return;
        }

        // Display Staff Roster Table before asking for Staff ID
        displayStaffTable();

        String staffId = ui.inputStaffId();
        if (staffId.isEmpty()) {
            ui.displayMessage("Operation cancelled.");
            return;
        }
        room.setAssignedStaffId(staffId);
        room.setCurrentStatus("Dirty");

        String taskId = "TSK-" + (1000 + taskList.getNumberOfEntries() + 1);
        String timestamp = LocalDateTime.now().format(dtf);

        HousekeepingTask task = new HousekeepingTask(taskId, roomId, staffId, "Dirty", timestamp);
        taskList.add(task);

        dao.saveRoomsToFile(roomList);
        dao.saveTasksToFile(taskList);

        ui.displayMessage("Successfully created task " + taskId + " for Room " + roomId + " assigned to Staff " + staffId);
        ui.pressEnterToContinue();
    }

    public void updateCleaningStatus() {
        ui.displayHeader("UPDATE SEQUENTIAL CLEANING STATUS");
        System.out.println("--- CURRENT ROOM ID, STAFF ID & STATUS TABLE ---");
        displayRoomTaskTable();
        System.out.println();

        String roomId = ui.inputRoomId();
        if (roomId.isEmpty()) {
            ui.displayMessage("Operation cancelled.");
            return;
        }

        HousekeepingTask task = findActiveTaskByRoomId(roomId);
        if (task == null) {
            ui.displayMessage("No active housekeeping task found for Room " + roomId);
            ui.pressEnterToContinue();
            return;
        }

        String currentStatus = task.getCurrentStatus();
        int choice = ui.selectNextStatusChoice(currentStatus);
        if (choice == 0) {
            ui.displayMessage("Status update operation cancelled.");
            return;
        }
        String newStatus;

        switch (choice) {
            case 1:
                newStatus = "Cleaning In Progress";
                break;
            case 2:
                newStatus = "Inspected";
                break;
            case 3:
                newStatus = "Ready for Check-In";
                break;
            default:
                ui.displayMessage("Invalid status choice selection.");
                return;
        }

        if (currentStatus.equalsIgnoreCase(newStatus)) {
            ui.displayMessage("Room " + roomId + " is already in status '" + currentStatus + "'. No changes made.");
            ui.pressEnterToContinue();
            return;
        }

        String updatedBy = (task.getStaffId() == null || task.getStaffId().trim().isEmpty()) ? "UNASSIGNED" : task.getStaffId();
        String reason = ui.inputReason();
        if (reason.isEmpty()) {
            reason = "Standard status progression";
        }
        String timestamp = LocalDateTime.now().format(dtf);

        // Update task state history stack
        task.updateStatus(newStatus, updatedBy, timestamp, reason);

        // Update matching room status
        Room room = findRoomById(roomId);
        if (room != null) {
            room.setCurrentStatus(newStatus);
            dao.saveRoomsToFile(roomList);
        }

        dao.saveTasksToFile(taskList);
        ui.displayMessage("SUCCESS: Room " + roomId + " status updated from '" + currentStatus + "' -> '" + newStatus + "'");
        ui.pressEnterToContinue();
    }

    public void rollbackTaskStatus() {
        ui.displayHeader("ROLLBACK / UNDO TASK STATUS (STACK ADT)");
        System.out.println("--- CURRENT ROOM ID, STAFF ID & STATUS TABLE ---");
        displayRoomTaskTable();
        System.out.println();

        String roomId = ui.inputRoomId();
        if (roomId.isEmpty()) {
            ui.displayMessage("Operation cancelled.");
            return;
        }

        HousekeepingTask task = findActiveTaskByRoomId(roomId);
        if (task == null) {
            ui.displayMessage("No active task found for Room " + roomId);
            ui.pressEnterToContinue();
            return;
        }

        String currentStatus = task.getCurrentStatus();
        if (task.getHistoryStack().isEmpty()) {
            ui.displayMessage("Cannot rollback: Task status stack history is empty!");
            ui.pressEnterToContinue();
            return;
        }

        // Check top log on stack for preview
        TaskStatusHistory topLog = task.getHistoryStack().peek();
        String previewPreviousStatus = topLog.getPreviousStatus();
        if ("N/A".equalsIgnoreCase(previewPreviousStatus)) {
            ui.displayMessage("Cannot rollback: Initial task creation status cannot be undone!");
            ui.pressEnterToContinue();
            return;
        }

        System.out.println("\n--------------------------------------------------------------------------");
        System.out.println(" ROLLBACK PREVIEW FOR ROOM " + roomId);
        System.out.println(" Current Status : " + currentStatus);
        System.out.println(" Target Status  : " + previewPreviousStatus + " (Status to Revert Back To)");
        System.out.println("--------------------------------------------------------------------------");

        if (!ui.confirmAction("Rollback Room " + roomId + " status back to '" + previewPreviousStatus + "'")) {
            ui.displayMessage("Rollback action cancelled by user.");
            return;
        }

        String updatedBy = ui.inputStaffId();
        if (updatedBy.isEmpty()) {
            updatedBy = "SUPERVISOR";
        }
        String reason = ui.inputReason();
        if (reason.isEmpty()) {
            reason = "Supervisor override / Late check-out rollback";
        }
        String timestamp = LocalDateTime.now().format(dtf);

        TaskStatusHistory poppedLog = task.rollbackStatus(updatedBy, timestamp, reason);

        if (poppedLog == null) {
            ui.displayMessage("Cannot rollback: Initial task creation status cannot be undone!");
            ui.pressEnterToContinue();
            return;
        }

        String rolledBackToStatus = task.getCurrentStatus();

        // Update room entity status
        Room room = findRoomById(roomId);
        if (room != null) {
            room.setCurrentStatus(rolledBackToStatus);
            dao.saveRoomsToFile(roomList);
        }

        dao.saveTasksToFile(taskList);
        ui.displayMessage("ROLLBACK SUCCESSFUL! Room " + roomId + " status reverted from '" 
                + currentStatus + "' back to '" + rolledBackToStatus + "'");
        ui.pressEnterToContinue();
    }

    public void searchRoomOrTask() {
        ui.displayHeader("SEARCH ROOM / HOUSEKEEPING TASK HISTORY");
        String query = ui.inputSearchQuery();

        if (query.isEmpty()) {
            ui.displayMessage("Search query cannot be empty.");
            return;
        }

        ui.displayHeader("SEARCH RESULTS FOR: " + query);
        boolean found = false;

        System.out.println(String.format("%-10s | %-10s | %-12s | %-24s | %-20s", 
                "Task ID", "Room ID", "Staff ID", "Current Status", "Last Updated"));
        System.out.println("--------------------------------------------------------------------------------------------------");

        // Custom Linear Search Algorithm
        for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
            HousekeepingTask task = taskList.getEntry(i);
            if (task.getRoomId().equalsIgnoreCase(query) || task.getStaffId().equalsIgnoreCase(query)) {
                System.out.println(String.format("%-10s | %-10s | %-12s | %-24s | %-20s",
                        task.getTaskId(), task.getRoomId(), task.getStaffId(), task.getCurrentStatus(), task.getLastUpdated()));
                found = true;
            }
        }

        if (!found) {
            ui.displayMessage("No tasks matching query '" + query + "' were found.");
        } else {
            ui.displayFooter();
        }
        ui.pressEnterToContinue();
    }

    // =========================================================================
    // MANAGEMENT REPORT GENERATION (Requires Search, Sort & Multi-Criteria Filter)
    // =========================================================================

    /**
     * Report 1: Room Cleaning Efficiency & Status Summary Report
     * Applies selection sorting by Floor & Room ID and filters by status criteria.
     */
    public void generateRoomEfficiencyReport() {
        ui.displayHeader("MANAGEMENT REPORT 1: ROOM CLEANING EFFICIENCY & TURNAROUND SUMMARY");

        // Clone rooms into temporary array list for sorting without mutating primary storage
        ListInterface<Room> reportRooms = new ArrayList<>();
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
            reportRooms.add(roomList.getEntry(i));
        }

        // Explicit Selection Sort Algorithm by Floor Number and Room ID
        sortRoomsByFloorAndId(reportRooms);

        int dirtyCount = 0;
        int cleaningCount = 0;
        int inspectedCount = 0;
        int readyCount = 0;

        System.out.println(String.format("%-6s | %-8s | %-18s | %-20s | %-10s", "No.", "Room ID", "Room Type", "Current Status", "Staff ID"));
        System.out.println("--------------------------------------------------------------------------");

        for (int i = 1; i <= reportRooms.getNumberOfEntries(); i++) {
            Room r = reportRooms.getEntry(i);
            System.out.println(String.format("%-6d | %-8s | %-18s | %-20s | %-10s",
                    i, r.getRoomId(), r.getRoomType(), r.getCurrentStatus(), r.getAssignedStaffId()));

            switch (r.getCurrentStatus()) {
                case "Dirty": dirtyCount++; break;
                case "Cleaning In Progress": cleaningCount++; break;
                case "Inspected": inspectedCount++; break;
                case "Ready for Check-In": readyCount++; break;
            }
        }

        System.out.println("--------------------------------------------------------------------------");
        System.out.println("SUMMARY STATISTICS:");
        System.out.println("Total Rooms Processed       : " + reportRooms.getNumberOfEntries());
        System.out.println("Rooms Dirty (Pending Task)  : " + dirtyCount);
        System.out.println("Rooms Cleaning In Progress  : " + cleaningCount);
        System.out.println("Rooms Inspected             : " + inspectedCount);
        System.out.println("Rooms Ready for Check-In    : " + readyCount);
        System.out.println("Turnaround Readiness Rate   : " + String.format("%.2f%%", 
                (reportRooms.getNumberOfEntries() > 0 ? (readyCount * 100.0 / reportRooms.getNumberOfEntries()) : 0.0)));
        ui.displayFooter();
        ui.pressEnterToContinue();
    }

    /**
     * Report 2: Housekeeping Task Rollback & Exception Audit Log
     * Sorts tasks by Rollback Count descending to identify exception bottlenecks.
     */
    public void generateTaskRollbackAuditReport() {
        ui.displayHeader("MANAGEMENT REPORT 2: HOUSEKEEPING TASK ROLLBACK & EXCEPTION AUDIT LOG");

        ListInterface<HousekeepingTask> auditTasks = new ArrayList<>();
        for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
            auditTasks.add(taskList.getEntry(i));
        }

        // Explicit Bubble Sort Algorithm by Rollback Count (Descending)
        sortTasksByRollbackCountDesc(auditTasks);

        int totalRollbacks = 0;
        System.out.println(String.format("%-8s | %-6s | %-8s | %-22s | %-12s", "Task ID", "Room", "Staff", "Current Status", "Rollbacks"));
        System.out.println("--------------------------------------------------------------------------");

        for (int i = 1; i <= auditTasks.getNumberOfEntries(); i++) {
            HousekeepingTask t = auditTasks.getEntry(i);
            System.out.println(String.format("%-8s | %-6s | %-8s | %-22s | %-12d",
                    t.getTaskId(), t.getRoomId(), t.getStaffId(), t.getCurrentStatus(), t.getRollbackCount()));
            totalRollbacks += t.getRollbackCount();
        }

        System.out.println("--------------------------------------------------------------------------");
        System.out.println("AUDIT SUMMARY & EXCEPTION ANALYSIS:");
        System.out.println("Total Tasks Audited         : " + auditTasks.getNumberOfEntries());
        System.out.println("Total Rollback Operations   : " + totalRollbacks);
        System.out.println("Average Rollbacks per Task  : " + String.format("%.2f", 
                (auditTasks.getNumberOfEntries() > 0 ? (totalRollbacks * 1.0 / auditTasks.getNumberOfEntries()) : 0.0)));
        ui.displayFooter();
        ui.pressEnterToContinue();
    }

    // Custom Sorting Helper Algorithms (Adhering to No Java Collections restriction)
    private void sortRoomsByFloorAndId(ListInterface<Room> list) {
        int n = list.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j <= n; j++) {
                Room r1 = list.getEntry(j);
                Room r2 = list.getEntry(minIdx);
                if (r1.getFloorNumber() < r2.getFloorNumber() || 
                   (r1.getFloorNumber() == r2.getFloorNumber() && r1.getRoomId().compareTo(r2.getRoomId()) < 0)) {
                    minIdx = j;
                }
            }
            if (minIdx != i) {
                Room temp = list.getEntry(i);
                list.replace(i, list.getEntry(minIdx));
                list.replace(minIdx, temp);
            }
        }
    }

    private void sortTasksByRollbackCountDesc(ListInterface<HousekeepingTask> list) {
        int n = list.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            for (int j = 1; j <= n - i; j++) {
                HousekeepingTask t1 = list.getEntry(j);
                HousekeepingTask t2 = list.getEntry(j + 1);
                if (t1.getRollbackCount() < t2.getRollbackCount()) {
                    list.replace(j, t2);
                    list.replace(j + 1, t1);
                }
            }
        }
    }

    private Room findRoomById(String roomId) {
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
            Room r = roomList.getEntry(i);
            if (r.getRoomId().equalsIgnoreCase(roomId)) {
                return r;
            }
        }
        return null;
    }

    private HousekeepingTask findActiveTaskByRoomId(String roomId) {
        for (int i = taskList.getNumberOfEntries(); i >= 1; i--) {
            HousekeepingTask t = taskList.getEntry(i);
            if (t.getRoomId().equalsIgnoreCase(roomId)) {
                return t;
            }
        }
        return null;
    }
}
