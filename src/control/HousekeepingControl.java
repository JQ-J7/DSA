package control;

import adt.ArrayList;
import adt.LinkedStack;
import adt.ListInterface;
import adt.StackInterface;
import boundary.HousekeepingUI;
import dao.HousekeepingDAO;
import entity.HousekeepingTask;
import entity.Room;
import entity.TaskStatusHistory;
import java.time.Duration;
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

        // Ensure constant 15 rooms (R101-R305) exist & are properly sanitized
        sanitizeConstantRooms();

        // Deduplicate loaded data and sanitize task assignments
        deduplicateData();

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
                // Sanitize task staffId if it holds obsolete staff IDs or legacy demo assignments
                if (isObsoleteStaffId(t.getStaffId())) {
                    Room r = findRoomById(t.getRoomId());
                    if (r != null) {
                        t.setStaffId(r.getAssignedStaffId());
                    } else {
                        t.setStaffId("UNASSIGNED");
                    }
                    tasksChanged = true;
                } else if (isLegacyTaskDemoMapping(t)) {
                    Room r = findRoomById(t.getRoomId());
                    if (r != null) {
                        t.setStaffId(r.getAssignedStaffId());
                    }
                    tasksChanged = true;
                }
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

    public boolean isValidStaffId(String staffId) {
        if (staffId == null || staffId.trim().isEmpty()) {
            return false;
        }
        String id = staffId.trim().toUpperCase();
        return id.matches("ST10[1-5]") || "SUP-01".equals(id);
    }

    /**
     * Enforces strict staff floor assignment rules:
     * - ST101, ST102: Floor 1 only
     * - ST103: Floor 2 only
     * - ST104: Floor 3 only
     * - ST105 (Junior Float) & SUP-01 (Supervisor): All Floors (1, 2, 3)
     * 
     * @param staffId Target staff ID
     * @param floorNumber Hotel floor number
     * @return True if staff is permitted to service this floor, false otherwise
     */
    public boolean isStaffAllowedOnFloor(String staffId, int floorNumber) {
        if (staffId == null || staffId.trim().isEmpty()) {
            return false;
        }
        String id = staffId.trim().toUpperCase();
        switch (id) {
            case "ST101":
            case "ST102":
                return floorNumber == 1;
            case "ST103":
                return floorNumber == 2;
            case "ST104":
                return floorNumber == 3;
            case "ST105":
            case "SUP-01":
                return floorNumber >= 1 && floorNumber <= 3;
            default:
                return false;
        }
    }

    /**
     * Inspects a task's TaskStatusHistory stack to calculate the duration (in minutes)
     * taken between 'Cleaning In Progress' and 'Ready for Check-In'.
     * Utilizes an auxiliary Stack ADT to traverse logs and completely restore original stack state.
     * 
     * @param task HousekeepingTask to evaluate
     * @return Duration in minutes, or -1 if the task has not completed the full turnaround workflow
     */
    private long calculateTaskTurnaroundMinutes(HousekeepingTask task) {
        if (task == null || task.getHistoryStack() == null || task.getHistoryStack().isEmpty()) {
            return -1;
        }

        StackInterface<TaskStatusHistory> originalStack = task.getHistoryStack();
        StackInterface<TaskStatusHistory> tempStack = new LinkedStack<>();

        String cleaningStartTimestamp = null;
        String readyTimestamp = null;

        // Traverse stack by popping entries into temporary auxiliary stack
        while (!originalStack.isEmpty()) {
            TaskStatusHistory log = originalStack.pop();
            tempStack.push(log);

            String status = log.getNewStatus();
            if (status != null) {
                if (status.equalsIgnoreCase("Cleaning In Progress") && cleaningStartTimestamp == null) {
                    cleaningStartTimestamp = log.getTimestamp();
                } else if (status.equalsIgnoreCase("Ready for Check-In") && readyTimestamp == null) {
                    readyTimestamp = log.getTimestamp();
                }
            }
        }

        // Completely restore original stack back to its exact prior state and LIFO ordering
        while (!tempStack.isEmpty()) {
            originalStack.push(tempStack.pop());
        }

        // Measure turnaround duration if both milestone timestamps were logged
        if (cleaningStartTimestamp != null && readyTimestamp != null) {
            try {
                LocalDateTime start = LocalDateTime.parse(cleaningStartTimestamp, dtf);
                LocalDateTime ready = LocalDateTime.parse(readyTimestamp, dtf);
                long minutes = Duration.between(start, ready).toMinutes();
                return minutes >= 0 ? minutes : 0;
            } catch (Exception ex) {
                return -1;
            }
        }

        return -1;
    }

    private boolean isObsoleteStaffId(String staffId) {
        if (staffId == null || staffId.trim().isEmpty()) {
            return false;
        }
        String id = staffId.trim().toUpperCase();
        if ("UNASSIGNED".equals(id)) {
            return false;
        }
        return !isValidStaffId(id);
    }

    private boolean isLegacyDemoMapping(Room existing) {
        String id = existing.getRoomId() == null ? "" : existing.getRoomId().toUpperCase();
        String staff = existing.getAssignedStaffId() == null ? "" : existing.getAssignedStaffId().toUpperCase();
        if ("R101".equals(id) && "ST101".equals(staff)) return true;
        if ("R102".equals(id) && "ST102".equals(staff)) return true;
        if ("R103".equals(id) && "ST103".equals(staff)) return true;
        if ("R104".equals(id) && "ST104".equals(staff)) return true;
        if ("R105".equals(id) && "ST105".equals(staff)) return true;
        return false;
    }

    private boolean isLegacyTaskDemoMapping(HousekeepingTask t) {
        String rid = t.getRoomId() == null ? "" : t.getRoomId().toUpperCase();
        String sid = t.getStaffId() == null ? "" : t.getStaffId().toUpperCase();
        if ("R101".equals(rid) && "ST101".equals(sid)) return true;
        if ("R102".equals(rid) && "ST102".equals(sid)) return true;
        if ("R103".equals(rid) && "ST103".equals(sid)) return true;
        if ("R104".equals(rid) && "ST104".equals(sid)) return true;
        if ("R105".equals(rid) && "ST105".equals(sid)) return true;
        return false;
    }

    private void initializeDemoData() {
        roomList.clear();
        taskList.clear();
        addDefaultDemoRooms();
        addDefaultDemoTasks();
    }

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
                // Force update staff assignment if it contains obsolete/unregistered staff or legacy demo defaults
                if (existing.getAssignedStaffId() == null 
                        || existing.getAssignedStaffId().trim().isEmpty() 
                        || isObsoleteStaffId(existing.getAssignedStaffId())
                        || isLegacyDemoMapping(existing)) {
                    existing.setAssignedStaffId(r.getAssignedStaffId());
                }
            }
        }
        dao.saveRoomsToFile(roomList);
    }

    private void addDefaultDemoTasks() {
        LocalDateTime baseTime = LocalDateTime.now();
        int taskCounter = 1001;

        Room[] defaultRooms = new Room[] {
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

        for (Room r : defaultRooms) {
            String taskId = "TSK-" + taskCounter++;
            if (findActiveTaskByRoomId(r.getRoomId()) == null) {
                String createTime = baseTime.minusMinutes(90).format(dtf);
                String cleanTime = baseTime.minusMinutes(45).format(dtf);
                String inspectTime = baseTime.minusMinutes(15).format(dtf);
                String readyTime = baseTime.format(dtf);

                HousekeepingTask task = new HousekeepingTask(taskId, r.getRoomId(), r.getAssignedStaffId(), "Dirty", createTime);
                if ("Cleaning In Progress".equals(r.getCurrentStatus())) {
                    task.updateStatus("Cleaning In Progress", r.getAssignedStaffId(), cleanTime, "Staff started cleaning");
                } else if ("Inspected".equals(r.getCurrentStatus())) {
                    task.updateStatus("Cleaning In Progress", r.getAssignedStaffId(), cleanTime, "Staff started cleaning");
                    task.updateStatus("Inspected", "SUP-01", inspectTime, "Supervisor inspection completed");
                } else if ("Ready for Check-In".equals(r.getCurrentStatus())) {
                    task.updateStatus("Cleaning In Progress", r.getAssignedStaffId(), cleanTime, "Staff started cleaning");
                    task.updateStatus("Inspected", "SUP-01", inspectTime, "Supervisor inspection passed");
                    task.updateStatus("Ready for Check-In", "SUP-01", readyTime, "Released for guest check-in");
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
        System.out.println(String.format("%-6s | %-10s | %-22s | %-26s | %-15s", 
                "No.", "Staff ID", "Staff Name", "Role / Designation", "Assigned Floor"));
        System.out.println("--------------------------------------------------------------------------------------------------");
        System.out.println(String.format("%-6d | %-10s | %-22s | %-26s | %-15s", 1, "ST101", "Ahmad Razali", "Senior Housekeeper", "Floor 1"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-26s | %-15s", 2, "ST102", "Siti Nurhaliza", "Housekeeping Attendant", "Floor 1"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-26s | %-15s", 3, "ST103", "Tan Ah Kow", "Senior Housekeeper", "Floor 2"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-26s | %-15s", 4, "ST104", "Murali Vijay", "Housekeeping Attendant", "Floor 3"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-26s | %-15s", 5, "ST105", "Lee Chong Wei", "Junior Attendant - Float", "All Floors"));
        System.out.println(String.format("%-6d | %-10s | %-22s | %-26s | %-15s", 6, "SUP-01", "Rosmah Mansor", "Housekeeping Supervisor", "All Floors"));
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

        // Check if room already has a staff member assigned or actively working on it
        String currentStaff = room.getAssignedStaffId();
        if (currentStaff != null && !currentStaff.trim().isEmpty() && !"UNASSIGNED".equalsIgnoreCase(currentStaff.trim())) {
            ui.displayMessage("ERROR: Room " + roomId + " already has staff member '" + currentStaff + "' assigned/working on it (Status: " + room.getCurrentStatus() + ")!\n"
                    + "       Another staff member cannot be assigned to this room while staff assignment is active.");
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

        if (!isValidStaffId(staffId)) {
            ui.displayMessage("ERROR: Staff ID '" + staffId + "' is invalid. Valid staff IDs are ST101-ST105 and SUP-01.");
            ui.pressEnterToContinue();
            return;
        }

        // Strict floor restriction validation check
        if (!isStaffAllowedOnFloor(staffId, room.getFloorNumber())) {
            ui.displayMessage("ERROR: Staff '" + staffId + "' is restricted and cannot be assigned to Floor " + room.getFloorNumber() + "!\n"
                    + "       Roster Duty Rules: ST101/ST102 (Floor 1), ST103 (Floor 2), ST104 (Floor 3), ST105/SUP-01 (All Floors).");
            ui.pressEnterToContinue();
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
            room.setAssignedStaffId(updatedBy);
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
    // MANAGEMENT REPORTS GENERATION (Custom Selection Sort & Bubble Sort ADT)
    // =========================================================================

    /**
     * Management Report 1: Room Cleaning Efficiency & Turnaround Summary Report.
     * Demonstrates custom Selection Sort by Floor & Room ID, multi-criteria status counting,
     * and calculates executive Turnaround Readiness Rates with health indicators.
     */
    public void generateRoomEfficiencyReport() {
        ui.displayHeader("MANAGEMENT REPORT 1: ROOM CLEANING EFFICIENCY & TURNAROUND SUMMARY");

        // Clone rooms into temporary List ADT to protect original memory structure during sorting
        ListInterface<Room> reportRooms = new ArrayList<>();
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
            reportRooms.add(roomList.getEntry(i));
        }

        // Custom Selection Sort Algorithm sorting entries by Floor Number & Room ID
        sortRoomsByFloorAndId(reportRooms);

        int dirtyCount = 0;
        int cleaningCount = 0;
        int inspectedCount = 0;
        int readyCount = 0;

        long totalTurnaroundMinutes = 0;
        int completedTurnaroundCount = 0;

        System.out.println(String.format("%-6s | %-8s | %-18s | %-22s | %-10s", "No.", "Room ID", "Room Type", "Current Status", "Staff ID"));
        System.out.println("--------------------------------------------------------------------------");

        for (int i = 1; i <= reportRooms.getNumberOfEntries(); i++) {
            Room r = reportRooms.getEntry(i);
            System.out.println(String.format("%-6d | %-8s | %-18s | %-22s | %-10s",
                    i, r.getRoomId(), r.getRoomType(), r.getCurrentStatus(), r.getAssignedStaffId()));

            switch (r.getCurrentStatus()) {
                case "Dirty": dirtyCount++; break;
                case "Cleaning In Progress": cleaningCount++; break;
                case "Inspected": inspectedCount++; break;
                case "Ready for Check-In": readyCount++; break;
            }
        }

        // Calculate average turnaround duration from task history stack via custom Stack ADT traversal
        for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
            HousekeepingTask t = taskList.getEntry(i);
            long duration = calculateTaskTurnaroundMinutes(t);
            if (duration >= 0) {
                totalTurnaroundMinutes += duration;
                completedTurnaroundCount++;
            }
        }

        double totalRooms = reportRooms.getNumberOfEntries();
        double turnaroundRate = totalRooms > 0 ? (readyCount * 100.0 / totalRooms) : 0.0;
        double avgTurnaroundMinutes = completedTurnaroundCount > 0 ? (totalTurnaroundMinutes * 1.0 / completedTurnaroundCount) : 0.0;

        System.out.println("--------------------------------------------------------------------------");
        System.out.println("SUMMARY STATISTICS & KPIS:");
        System.out.printf("%-30s : %d\n", "Total Rooms Managed", (int) totalRooms);
        System.out.printf("%-30s : %d (%.1f%%)\n", "Rooms Ready for Check-In", readyCount, turnaroundRate);
        System.out.printf("%-30s : %d\n", "Rooms Inspected (Pending Ready)", inspectedCount);
        System.out.printf("%-30s : %d\n", "Rooms Cleaning In Progress", cleaningCount);
        System.out.printf("%-30s : %d\n", "Rooms Dirty (Pending Task)", dirtyCount);
        System.out.printf("%-30s : %.1f minutes (%d completed tasks evaluated)\n", "Avg Turnaround Duration", avgTurnaroundMinutes, completedTurnaroundCount);
        System.out.println("--------------------------------------------------------------------------");
        
        // Executive Status Health Assessment Indicator
        if (turnaroundRate >= 70.0) {
            System.out.println("STATUS HEALTH EVALUATION      : EXCELLENT (High Turnover Efficiency)");
        } else if (turnaroundRate >= 40.0) {
            System.out.println("STATUS HEALTH EVALUATION      : MODERATE (Housekeeping In-Progress)");
        } else {
            System.out.println("STATUS HEALTH EVALUATION      : ATTENTION NEEDED (High Dirty/Pending Ratio)");
        }
        System.out.println("==========================================================================\n");
        ui.pressEnterToContinue();
    }

    /**
     * Management Report 2: Housekeeping Task Rollback & Exception Audit Log.
     * Demonstrates custom Bubble Sort ordering tasks by Rollback Count descending
     * to highlight exception bottlenecks and Stack ADT operational frequencies.
     */
    public void generateTaskRollbackAuditReport() {
        ui.displayHeader("MANAGEMENT REPORT 2: HOUSEKEEPING TASK ROLLBACK & EXCEPTION AUDIT LOG");

        ListInterface<HousekeepingTask> auditTasks = new ArrayList<>();
        for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
            auditTasks.add(taskList.getEntry(i));
        }

        // Custom Bubble Sort Algorithm sorting tasks by Rollback Count (Descending)
        sortTasksByRollbackCountDesc(auditTasks);

        int totalRollbacks = 0;
        int highExceptionTasks = 0;

        System.out.println(String.format("%-8s | %-6s | %-8s | %-22s | %-12s", "Task ID", "Room", "Staff", "Current Status", "Rollbacks"));
        System.out.println("--------------------------------------------------------------------------");

        for (int i = 1; i <= auditTasks.getNumberOfEntries(); i++) {
            HousekeepingTask t = auditTasks.getEntry(i);
            System.out.println(String.format("%-8s | %-6s | %-8s | %-22s | %-12d",
                    t.getTaskId(), t.getRoomId(), t.getStaffId(), t.getCurrentStatus(), t.getRollbackCount()));
            totalRollbacks += t.getRollbackCount();
            if (t.getRollbackCount() > 1) {
                highExceptionTasks++;
            }
        }

        double avgRollbacks = auditTasks.getNumberOfEntries() > 0 ? (totalRollbacks * 1.0 / auditTasks.getNumberOfEntries()) : 0.0;

        System.out.println("--------------------------------------------------------------------------");
        System.out.println("AUDIT SUMMARY & EXCEPTION ANALYSIS:");
        System.out.printf("%-32s : %d\n", "Total Tasks Audited", auditTasks.getNumberOfEntries());
        System.out.printf("%-32s : %d\n", "Total Exception Rollbacks", totalRollbacks);
        System.out.printf("%-32s : %.2f\n", "Average Rollbacks per Task", avgRollbacks);
        System.out.printf("%-32s : %d\n", "Frequent Exception Tasks (>1)", highExceptionTasks);
        System.out.println("--------------------------------------------------------------------------");

        // Audit Risk Assessment Flag
        if (totalRollbacks > 3 || highExceptionTasks > 1) {
            System.out.println("AUDIT RISK ALERT              : HIGH (Multiple Status Reversals Detected)");
        } else {
            System.out.println("AUDIT RISK ALERT              : NORMAL (Low Operational Reversals)");
        }
        System.out.println("==========================================================================\n");
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
