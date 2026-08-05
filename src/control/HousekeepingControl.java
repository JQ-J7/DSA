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

        // Populate default demo data if list is empty
        if (roomList.isEmpty() || taskList.isEmpty()) {
            initializeDemoData();
        }
    }

    private void initializeDemoData() {
        roomList.add(new Room("R101", "Standard Deluxe", 1, "Dirty", "ST101"));
        roomList.add(new Room("R102", "Standard Deluxe", 1, "Cleaning In Progress", "ST102"));
        roomList.add(new Room("R201", "Executive Suite", 2, "Inspected", "ST101"));
        roomList.add(new Room("R202", "Executive Suite", 2, "Ready for Check-In", "ST103"));
        roomList.add(new Room("R301", "Presidential Suite", 3, "Dirty", "ST104"));
        dao.saveRoomsToFile(roomList);

        String now = LocalDateTime.now().format(dtf);
        HousekeepingTask t1 = new HousekeepingTask("TSK-1001", "R101", "ST101", "Dirty", now);
        HousekeepingTask t2 = new HousekeepingTask("TSK-1002", "R102", "ST102", "Dirty", now);
        t2.updateStatus("Cleaning In Progress", "ST102", now, "Staff started cleaning");

        HousekeepingTask t3 = new HousekeepingTask("TSK-1003", "R201", "ST101", "Dirty", now);
        t3.updateStatus("Cleaning In Progress", "ST101", now, "Staff started cleaning");
        t3.updateStatus("Inspected", "SUP-01", now, "Supervisor inspection completed");

        HousekeepingTask t4 = new HousekeepingTask("TSK-1004", "R202", "ST103", "Dirty", now);
        t4.updateStatus("Cleaning In Progress", "ST103", now, "Staff started cleaning");
        t4.updateStatus("Inspected", "SUP-01", now, "Supervisor inspection passed");
        t4.updateStatus("Ready for Check-In", "SUP-01", now, "Released for guest check-in");

        HousekeepingTask t5 = new HousekeepingTask("TSK-1005", "R301", "ST104", "Dirty", now);

        taskList.add(t1);
        taskList.add(t2);
        taskList.add(t3);
        taskList.add(t4);
        taskList.add(t5);
        dao.saveTasksToFile(taskList);
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

    public void displayAllRooms() {
        ui.displayHeader("ALL HOTEL ROOMS & CURRENT STATUSES");
        System.out.println(String.format("%-6s | %-8s | %-18s | %-6s | %-22s | %-10s", 
                "No.", "Room ID", "Room Type", "Floor", "Current Status", "Staff ID"));
        System.out.println("--------------------------------------------------------------------------");
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
            Room r = roomList.getEntry(i);
            System.out.println(String.format("%-6d | %-8s | %-18s | Floor %-2d | %-22s | %-10s",
                    i, r.getRoomId(), r.getRoomType(), r.getFloorNumber(), r.getCurrentStatus(), r.getAssignedStaffId()));
        }
        ui.displayFooter();
        ui.pressEnterToContinue();
    }

    public void assignNewCleaningTask() {
        ui.displayHeader("ASSIGN NEW HOUSEKEEPING CLEANING TASK");
        String roomId = ui.inputRoomId();
        if (roomId.isEmpty()) {
            ui.displayMessage("Operation cancelled.");
            return;
        }

        // Check if room exists
        Room room = findRoomById(roomId);
        if (room == null) {
            System.out.println("Room " + roomId + " is not registered yet. Creating new room record...");
            String roomType = ui.inputRoomType();
            int floor = ui.inputFloorNumber();
            String staffId = ui.inputStaffId();
            if (staffId.isEmpty()) {
                ui.displayMessage("Operation cancelled.");
                return;
            }
            room = new Room(roomId, roomType, floor, "Dirty", staffId);
            roomList.add(room);
            dao.saveRoomsToFile(roomList);
        }

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

        String updatedBy = ui.inputStaffId();
        if (updatedBy.isEmpty()) {
            updatedBy = task.getStaffId();
        }
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

        // Custom Linear Search Algorithm
        for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
            HousekeepingTask task = taskList.getEntry(i);
            if (task.getRoomId().equalsIgnoreCase(query) || task.getStaffId().equalsIgnoreCase(query)) {
                System.out.println(task);
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
