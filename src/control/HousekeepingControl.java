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

    // Rule 1 & Rule 2 Enforcement: Auto-Archive older tasks and sync active task with room status
    String syncTimestamp = LocalDateTime.now().format(dtf);
    for (int i = 1; i <= cleanRoomList.getNumberOfEntries(); i++) {
        Room r = cleanRoomList.getEntry(i);
        HousekeepingTask activeTask = null;
        // Scan backwards: latest non-archived task is active, all others for this room become Archived
        for (int j = cleanTaskList.getNumberOfEntries(); j >= 1; j--) {
            HousekeepingTask t = cleanTaskList.getEntry(j);
            if (t.getRoomId().equalsIgnoreCase(r.getRoomId())) {
                if (activeTask == null && !"Archived".equalsIgnoreCase(t.getCurrentStatus())) {
                    activeTask = t;
                } else if (activeTask != null && !"Archived".equalsIgnoreCase(t.getCurrentStatus())) {
                    // Older task for same room: Auto-Archive (Rule 1)
                    t.setCurrentStatus("Archived");
                    tasksChanged = true;
                }
            }
        }

        if (activeTask != null) {
            // Rule 2: Tripartite Data Consistency (CLO2/CLO3)
            if (!activeTask.getCurrentStatus().equalsIgnoreCase(r.getCurrentStatus())) {
                if ("Occupied".equalsIgnoreCase(r.getCurrentStatus())) {
                    activeTask.setCurrentStatus("Occupied");
                    activeTask.setLastUpdated(syncTimestamp);
                    String prevSt = (activeTask.getHistoryStack().isEmpty()) ? "Ready for Check-In" : activeTask.getHistoryStack().peek().getNewStatus();
                    TaskStatusHistory occLog = new TaskStatusHistory(
                            "LOG-" + System.currentTimeMillis() % 10000,
                            prevSt,
                            "Occupied",
                            (r.getAssignedStaffId() != null ? r.getAssignedStaffId() : "FRONT_DESK"),
                            syncTimestamp,
                            "Guest checked in by Front Desk",
                            false
                    );
                    activeTask.getHistoryStack().push(occLog);
                } else {
                    // FIXED (Rule 2): Safely sync task status with room status and record transition log on historyStack
                    String prevSt = (activeTask.getHistoryStack().isEmpty()) ? activeTask.getCurrentStatus() : activeTask.getHistoryStack().peek().getNewStatus();
                    activeTask.setCurrentStatus(r.getCurrentStatus());
                    activeTask.setLastUpdated(syncTimestamp);

                    TaskStatusHistory syncLog = new TaskStatusHistory(
                            "LOG-" + System.currentTimeMillis() % 10000,
                            prevSt,
                            r.getCurrentStatus(),
                            (r.getAssignedStaffId() != null && !r.getAssignedStaffId().trim().isEmpty() ? r.getAssignedStaffId() : "SYSTEM"),
                            syncTimestamp,
                            "Synchronized task status with Room entity status",
                            false
                    );
                    activeTask.getHistoryStack().push(syncLog);
                }
                tasksChanged = true;
            }
            if (r.getAssignedStaffId() != null && !r.getAssignedStaffId().trim().isEmpty() && !"UNASSIGNED".equalsIgnoreCase(r.getAssignedStaffId()) && !r.getAssignedStaffId().equalsIgnoreCase(activeTask.getStaffId())) {
                activeTask.setStaffId(r.getAssignedStaffId());
                tasksChanged = true;
            }
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
        // Reload latest room data from shared rooms.dat each time subsystem is entered
        // so changes made by Walk-In or Front-Desk (e.g., Occupied, Dirty) are reflected
        ListInterface<Room> latestRooms = dao.retrieveRoomsFromFile();
        if (latestRooms != null && !latestRooms.isEmpty()) {
            roomList = latestRooms;
        }

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

        // Check if room is currently occupied by a guest
        if ("Occupied".equalsIgnoreCase(room.getCurrentStatus())) {
            ui.displayMessage("ERROR: Room " + roomId + " is currently OCCUPIED by a guest!\n"
                    + "       Task assignment is locked until Front Desk checks out the guest (which will set status to 'Dirty').");
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

        String timestamp = LocalDateTime.now().format(dtf);

        // =========================================================================
        // RULE 1: Single Active Task Enforcement (Auto-Archiving - CLO2/CLO3 Standard)
        // A room can have ONLY ONE active (non-Archived) task at any given time.
        // =========================================================================
        for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
            HousekeepingTask existingTask = taskList.getEntry(i);
            if (existingTask.getRoomId().equalsIgnoreCase(roomId) && !"Archived".equalsIgnoreCase(existingTask.getCurrentStatus())) {
                existingTask.setCurrentStatus("Archived");
                existingTask.setLastUpdated(timestamp);
            }
        }

        // =========================================================================
        // RULE 2: Tripartite State Synchronization ("Three-in-One Consistency")
        // Task.currentStatus ('Dirty') == Room.currentStatus ('Dirty') == Stack.peek() ('Dirty')
        // =========================================================================
        room.setAssignedStaffId(staffId);
        room.setCurrentStatus("Dirty");

        String taskId = "TSK-" + (1000 + taskList.getNumberOfEntries() + 1);
        HousekeepingTask task = new HousekeepingTask(taskId, roomId, staffId, "Dirty", timestamp);
        taskList.add(task);

        // Immediate Data Persistence
        dao.saveRoomsToFile(roomList);
        dao.saveTasksToFile(taskList);

        ui.displayMessage("Successfully created active task " + taskId + " for Room " + roomId + " assigned to Staff " + staffId);
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

        Room room = findRoomById(roomId);
        if (room == null) {
            ui.displayMessage("ERROR: Room " + roomId + " is unknown / not registered in the system!");
            ui.pressEnterToContinue();
            return;
        }

        if ("Occupied".equalsIgnoreCase(room.getCurrentStatus())) {
            ui.displayMessage("ERROR: Room " + roomId + " is currently OCCUPIED by a guest!\n"
                    + "       Status updates are locked until Front Desk checks out the guest (which will set status to 'Dirty').");
            ui.pressEnterToContinue();
            return;
        }

        // Validation: Ensure room has an assigned staff member before updating cleaning status
        String assignedStaff = room.getAssignedStaffId();
        if (assignedStaff == null || assignedStaff.trim().isEmpty() || "UNASSIGNED".equalsIgnoreCase(assignedStaff.trim())) {
            ui.displayMessage("ERROR: Cannot update cleaning status for Room " + roomId + "!\n"
                    + "       No housekeeping staff member is currently assigned to this room.\n"
                    + "       Please assign a staff member first using Option [2] (Assign New Cleaning Task to Staff).");
            ui.pressEnterToContinue();
            return;
        }

        HousekeepingTask task = findActiveTaskByRoomId(roomId);
        String currentStatus = room.getCurrentStatus();

        // If task is missing or out-of-sync with room's Dirty/new cycle, initialize a fresh task cycle
        if (task == null || (!task.getCurrentStatus().equalsIgnoreCase(currentStatus) && "Dirty".equalsIgnoreCase(currentStatus))) {
            String taskId = "TSK-" + (1000 + taskList.getNumberOfEntries() + 1);
            String timestamp = LocalDateTime.now().format(dtf);
            task = new HousekeepingTask(taskId, roomId, assignedStaff, currentStatus, timestamp);
            taskList.add(task);
            dao.saveTasksToFile(taskList);
        } else {
            task.setStaffId(assignedStaff);
        }

        String newStatus;
        String reason;

        if ("Dirty".equalsIgnoreCase(currentStatus)) {
            newStatus = "Cleaning In Progress";
            reason = "Started room cleaning";
        } else if ("Cleaning In Progress".equalsIgnoreCase(currentStatus)) {
            newStatus = "Inspected";
            reason = "Completed cleaning, submitted for inspection";
        } else if ("Inspected".equalsIgnoreCase(currentStatus)) {
            newStatus = "Ready for Check-In";
            reason = "Inspection approved, ready for guest check-in";
        } else if ("Ready for Check-In".equalsIgnoreCase(currentStatus)) {
            ui.displayMessage("Room " + roomId + " is already 'Ready for Check-In' (cleaning workflow is fully completed).");
            ui.pressEnterToContinue();
            return;
        } else {
            ui.displayMessage("Cannot auto-advance: Room " + roomId + " has unrecognized status '" + currentStatus + "'.");
            ui.pressEnterToContinue();
            return;
        }

        String updatedBy = (task.getStaffId() == null || task.getStaffId().trim().isEmpty()) ? "UNASSIGNED" : task.getStaffId();
        String timestamp = LocalDateTime.now().format(dtf);

        // Update task state history stack
        task.updateStatus(newStatus, updatedBy, timestamp, reason);

        // Update matching room status
        if (room != null) {
            room.setCurrentStatus(newStatus);
            dao.saveRoomsToFile(roomList);
        }

        dao.saveTasksToFile(taskList);
        ui.displayMessage("SUCCESS: Room " + roomId + " status auto-updated from '" + currentStatus + "' -> '" + newStatus + "'");
        ui.pressEnterToContinue();
    }

    public void rollbackTaskStatus() {
        ui.displayHeader("ROLLBACK / UNDO TASK STATUS (STACK ADT)");
        System.out.println("--- CURRENT ROOM ID, STAFF ID & STATUS TABLE ---");
        displayRoomTaskTable();
        System.out.println();

        String inputId = ui.inputRoomId();
        if (inputId.isEmpty()) {
            ui.displayMessage("Operation cancelled.");
            return;
        }

        // Support lookup by Room ID or Task ID
        Room room = findRoomById(inputId);
        HousekeepingTask task = null;
        if (room != null) {
            task = findActiveTaskByRoomId(room.getRoomId());
        } else {
            // Check if input was a Task ID
            for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
                HousekeepingTask t = taskList.getEntry(i);
                if (t.getTaskId().equalsIgnoreCase(inputId)) {
                    task = t;
                    room = findRoomById(t.getRoomId());
                    break;
                }
            }
        }

        if (room == null) {
            ui.displayMessage("ERROR: Room / Task ID '" + inputId + "' is unknown / not registered in the system!");
            ui.pressEnterToContinue();
            return;
        }

        if ("Occupied".equalsIgnoreCase(room.getCurrentStatus())) {
            ui.displayMessage("ERROR: Room " + room.getRoomId() + " is currently OCCUPIED by a guest!\n"
                    + "       Rollback operations are locked while the room is occupied.");
            ui.pressEnterToContinue();
            return;
        }

        if (task == null) {
            ui.displayMessage("No active housekeeping task found for Room " + room.getRoomId());
            ui.pressEnterToContinue();
            return;
        }

        StackInterface<TaskStatusHistory> historyStack = task.getHistoryStack();
        if (historyStack == null || historyStack.isEmpty()) {
            ui.displayMessage("Cannot rollback: Task status stack history is empty!");
            ui.pressEnterToContinue();
            return;
        }

        // Initial Log Protection: Cannot undo initial creation record
        TaskStatusHistory topLog = historyStack.peek();
        String previousStatus = topLog.getPreviousStatus();
        if (previousStatus == null || "N/A".equalsIgnoreCase(previousStatus) || historyStack.getNumberOfEntries() <= 1) {
            ui.displayMessage("Cannot rollback: Initial task creation status cannot be undone!");
            ui.pressEnterToContinue();
            return;
        }

        // Sub-menu for Rollback Scenario selection
        int scenarioChoice = ui.selectRollbackScenario();
        if (scenarioChoice == 0) {
            ui.displayMessage("Rollback operation cancelled.");
            return;
        }

        String currentStatus = room.getCurrentStatus();
        String roomId = room.getRoomId();

        switch (scenarioChoice) {
            case 1:
                // =========================================================================
                // Scenario 1: Late Check-Out Rollback (Revert Room to 'Occupied')
                // Business Context: Cleaning started prematurely, guest requested late check-out
                // =========================================================================
                System.out.println("\n--------------------------------------------------------------------------");
                System.out.println(" LATE CHECK-OUT ROLLBACK PREVIEW FOR ROOM " + roomId);
                System.out.println(" Current Status : " + currentStatus);
                System.out.println(" Target Status  : Occupied (Guest Late Check-Out Requested)");
                System.out.println(" Assigned Staff : Will be reset to UNASSIGNED");
                System.out.println("--------------------------------------------------------------------------");

                if (!ui.confirmAction("Rollback Room " + roomId + " status to 'Occupied' for Late Check-Out")) {
                    ui.displayMessage("Rollback action cancelled by user.");
                    return;
                }

                // LIFO Stack ADT Execution: Pop the premature cleaning transition log
                historyStack.pop();

                String timestamp1 = LocalDateTime.now().format(dtf);
                String rollbackReason = "Late Check-Out requested by guest";
                String staffRecorder = (task.getStaffId() == null || task.getStaffId().trim().isEmpty() || "UNASSIGNED".equalsIgnoreCase(task.getStaffId())) ? "SUPERVISOR" : task.getStaffId();

                // Push new rollback state record onto the Stack
                TaskStatusHistory lateCheckoutLog = new TaskStatusHistory(
                        "LOG-" + System.currentTimeMillis() % 10000,
                        currentStatus,
                        "Occupied",
                        staffRecorder,
                        timestamp1,
                        rollbackReason,
                        true
                );
                historyStack.push(lateCheckoutLog);

                // State Updates
                room.setCurrentStatus("Occupied");
                room.setAssignedStaffId("UNASSIGNED");
                task.setCurrentStatus("Occupied");
                task.setStaffId("UNASSIGNED");
                task.setLastUpdated(timestamp1);
                task.incrementRollbackCount();

                // Data Persistence
                dao.saveRoomsToFile(roomList);
                dao.saveTasksToFile(taskList);

                ui.displayMessage("LATE CHECK-OUT ROLLBACK SUCCESSFUL! Room " + roomId 
                        + " status reverted to 'Occupied' and staff unassigned.");
                ui.pressEnterToContinue();
                break;

            case 2:
                // =========================================================================
                // Scenario 2: Undo Status Misclick (Revert to Immediate Previous Log)
                // Business Context: Supervisor / staff accidentally advanced status by mistake
                // =========================================================================
                String targetPreviousStatus = topLog.getPreviousStatus();

                System.out.println("\n--------------------------------------------------------------------------");
                System.out.println(" MISCLICK CORRECTION PREVIEW FOR ROOM " + roomId);
                System.out.println(" Current Status : " + currentStatus);
                System.out.println(" Target Status  : " + targetPreviousStatus + " (Immediate Previous State)");
                System.out.println("--------------------------------------------------------------------------");

                if (!ui.confirmAction("Revert Room " + roomId + " status back to '" + targetPreviousStatus + "'")) {
                    ui.displayMessage("Rollback action cancelled by user.");
                    return;
                }

                String reason = ui.inputCorrectionReason();
                if (reason.isEmpty()) {
                    reason = "Supervisor status input correction";
                }

                // LIFO Stack ADT Execution: Pop the misclicked status transition log
                historyStack.pop();

                // The previous log state is now at the top of the Stack ADT
                TaskStatusHistory previousLog = historyStack.peek();
                String revertedStatus = (previousLog != null) ? previousLog.getNewStatus() : targetPreviousStatus;
                String timestamp2 = LocalDateTime.now().format(dtf);

                // State Updates
                task.setCurrentStatus(revertedStatus);
                task.setLastUpdated(timestamp2);
                task.incrementRollbackCount();
                room.setCurrentStatus(revertedStatus);

                // Data Persistence
                dao.saveRoomsToFile(roomList);
                dao.saveTasksToFile(taskList);

                ui.displayMessage("STATUS CORRECTION SUCCESSFUL! Room " + roomId 
                        + " status reverted from '" + currentStatus + "' back to '" + revertedStatus + "'");
                ui.pressEnterToContinue();
                break;

            default:
                ui.displayMessage("Invalid rollback scenario selection.");
                break;
        }
    }

    public void searchRoomOrTask() {
    ui.displayHeader("SEARCH ROOM / HOUSEKEEPING TASK HISTORY");
    String query = ui.inputSearchQuery();

    if (query.isEmpty()) {
        ui.displayMessage("Search query cannot be empty.");
        return;
    }

    // Reload latest room data and task data from shared DAO
    ListInterface<Room> latestRooms = dao.retrieveRoomsFromFile();
    if (latestRooms != null && !latestRooms.isEmpty()) {
        roomList = latestRooms;
    }
    ListInterface<HousekeepingTask> latestTasks = dao.retrieveTasksFromFile();
    if (latestTasks != null && !latestTasks.isEmpty()) {
        taskList = latestTasks;
    }

    // Rule 1 & Rule 2 Enforcement: Auto-Archive older tasks and sync active task with room status
    String syncTimestamp = LocalDateTime.now().format(dtf);
    boolean syncChanged = false;
    for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
        Room r = roomList.getEntry(i);
        HousekeepingTask activeTask = null;
        // Scan backwards: latest non-archived task is active, all others for this room become Archived
        for (int j = taskList.getNumberOfEntries(); j >= 1; j--) {
            HousekeepingTask t = taskList.getEntry(j);
            if (t.getRoomId().equalsIgnoreCase(r.getRoomId())) {
                if (activeTask == null && !"Archived".equalsIgnoreCase(t.getCurrentStatus())) {
                    activeTask = t;
                } else if (activeTask != null && !"Archived".equalsIgnoreCase(t.getCurrentStatus())) {
                    // Older task for same room: Auto-Archive (Rule 1)
                    t.setCurrentStatus("Archived");
                    syncChanged = true;
                }
            }
        }

        if (activeTask != null) {
            // Rule 2: Tripartite Data Consistency (CLO2/CLO3)
            if (!activeTask.getCurrentStatus().equalsIgnoreCase(r.getCurrentStatus())) {
                if ("Occupied".equalsIgnoreCase(r.getCurrentStatus())) {
                    activeTask.setCurrentStatus("Occupied");
                    activeTask.setLastUpdated(syncTimestamp);
                    String prevSt = (activeTask.getHistoryStack().isEmpty()) ? "Ready for Check-In" : activeTask.getHistoryStack().peek().getNewStatus();
                    TaskStatusHistory occLog = new TaskStatusHistory(
                            "LOG-" + System.currentTimeMillis() % 10000,
                            prevSt,
                            "Occupied",
                            (r.getAssignedStaffId() != null ? r.getAssignedStaffId() : "FRONT_DESK"),
                            syncTimestamp,
                            "Guest checked in by Front Desk",
                            false
                    );
                    activeTask.getHistoryStack().push(occLog);
                } else {
                    // FIXED (Rule 2): Safely sync task status with room status and record transition log on historyStack
                    String prevSt = (activeTask.getHistoryStack().isEmpty()) ? activeTask.getCurrentStatus() : activeTask.getHistoryStack().peek().getNewStatus();
                    activeTask.setCurrentStatus(r.getCurrentStatus());
                    activeTask.setLastUpdated(syncTimestamp);

                    TaskStatusHistory syncLog = new TaskStatusHistory(
                            "LOG-" + System.currentTimeMillis() % 10000,
                            prevSt,
                            r.getCurrentStatus(),
                            (r.getAssignedStaffId() != null && !r.getAssignedStaffId().trim().isEmpty() ? r.getAssignedStaffId() : "SYSTEM"),
                            syncTimestamp,
                            "Synchronized task status with Room entity status during search realignment",
                            false
                    );
                    activeTask.getHistoryStack().push(syncLog);
                }
                syncChanged = true;
            }
            if (r.getAssignedStaffId() != null && !r.getAssignedStaffId().trim().isEmpty() && !"UNASSIGNED".equalsIgnoreCase(r.getAssignedStaffId()) && !r.getAssignedStaffId().equalsIgnoreCase(activeTask.getStaffId())) {
                activeTask.setStaffId(r.getAssignedStaffId());
                syncChanged = true;
            }
        }
    }
    if (syncChanged) {
        dao.saveTasksToFile(taskList);
    }

    ui.displayHeader("SEARCH RESULTS FOR: " + query);

    // 1. Search Matching Hotel Rooms (Linear Search ADT)
    ListInterface<Room> matchedRooms = new ArrayList<>();
    for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
        Room r = roomList.getEntry(i);
        if (r.getRoomId().equalsIgnoreCase(query) || 
            (r.getAssignedStaffId() != null && r.getAssignedStaffId().equalsIgnoreCase(query)) || 
            r.getCurrentStatus().equalsIgnoreCase(query) ||
            r.getRoomType().toLowerCase().contains(query.toLowerCase()) ||
            String.valueOf(r.getFloorNumber()).equals(query)) {
            matchedRooms.add(r);
        }
    }

    // 2. Search Matching Housekeeping Tasks (Linear Search ADT)
    ListInterface<HousekeepingTask> matchedTasks = new ArrayList<>();
    for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
        HousekeepingTask task = taskList.getEntry(i);
        if (task.getTaskId().equalsIgnoreCase(query) || 
            task.getRoomId().equalsIgnoreCase(query) || 
            (task.getStaffId() != null && task.getStaffId().equalsIgnoreCase(query)) ||
            task.getCurrentStatus().equalsIgnoreCase(query)) {
            matchedTasks.add(task);
        }
    }

    if (matchedRooms.isEmpty() && matchedTasks.isEmpty()) {
        ui.displayMessage("No hotel rooms or housekeeping tasks matching query '" + query + "' were found.");
        ui.pressEnterToContinue();
        return;
    }

    // Display Matched Rooms Table
    if (!matchedRooms.isEmpty()) {
        System.out.println("--- [ 1. LIVE HOTEL ROOM STATUS ] ---");
        System.out.println(String.format("%-6s | %-10s | %-12s | %-24s | %-18s | %-8s", 
                "No.", "Room ID", "Staff ID", "Current Status", "Room Type", "Floor"));
        System.out.println("--------------------------------------------------------------------------------------------------");
        for (int i = 1; i <= matchedRooms.getNumberOfEntries(); i++) {
            Room r = matchedRooms.getEntry(i);
            String staff = (r.getAssignedStaffId() == null || r.getAssignedStaffId().trim().isEmpty()) ? "UNASSIGNED" : r.getAssignedStaffId();
            System.out.println(String.format("%-6d | %-10s | %-12s | %-24s | %-18s | Floor %-2d",
                    i, r.getRoomId(), staff, r.getCurrentStatus(), r.getRoomType(), r.getFloorNumber()));
        }
        System.out.println("--------------------------------------------------------------------------------------------------");
    }

    // Display Matched Housekeeping Tasks Table
    if (!matchedTasks.isEmpty()) {
        System.out.println("\n--- [ 2. HOUSEKEEPING TASK & WORKFLOW RECORDS ] ---");
        System.out.println(String.format("%-10s | %-10s | %-12s | %-22s | %-20s | %-10s | %-12s", 
                "Task ID", "Room ID", "Staff ID", "Current Status", "Last Updated", "Rollbacks", "Task State"));
        System.out.println("----------------------------------------------------------------------------------------------------------------------");
        for (int i = 1; i <= matchedTasks.getNumberOfEntries(); i++) {
            HousekeepingTask task = matchedTasks.getEntry(i);
            String lifecycle = "Archived".equalsIgnoreCase(task.getCurrentStatus()) ? "[ARCHIVED]" : "[ACTIVE]";
            System.out.println(String.format("%-10s | %-10s | %-12s | %-22s | %-20s | %-10d | %-12s",
                    task.getTaskId(), task.getRoomId(), task.getStaffId(), task.getCurrentStatus(), task.getLastUpdated(), task.getRollbackCount(), lifecycle));
        }
        System.out.println("----------------------------------------------------------------------------------------------------------------------");
    }

    // Display Status History Log Trace Stack if a room or active task is inspected
    HousekeepingTask targetActiveTask = null;
    if (matchedRooms.getNumberOfEntries() == 1) {
        targetActiveTask = findActiveTaskByRoomId(matchedRooms.getEntry(1).getRoomId());
    } else if (matchedTasks.getNumberOfEntries() == 1) {
        targetActiveTask = matchedTasks.getEntry(1);
    }

    if (targetActiveTask != null) {
        StackInterface<TaskStatusHistory> stack = targetActiveTask.getHistoryStack();
        if (stack != null && !stack.isEmpty()) {
            System.out.println("\n--- [ 3. STATUS HISTORY LOG TRACE (STACK ADT) FOR ACTIVE " + targetActiveTask.getTaskId() + " (" + targetActiveTask.getRoomId() + ") ] ---");
            System.out.println(stack.toString().trim());
            System.out.println("--------------------------------------------------------------------------------------------------");
        }
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

        System.out.println(String.format("%-6s | %-8s | %-18s | %-24s | %-12s", "No.", "Room ID", "Room Type", "Current Status", "Staff ID"));
        System.out.println("--------------------------------------------------------------------------");

        for (int i = 1; i <= reportRooms.getNumberOfEntries(); i++) {
            Room r = reportRooms.getEntry(i);
            String staff = (r.getAssignedStaffId() == null || r.getAssignedStaffId().trim().isEmpty()) ? "UNASSIGNED" : r.getAssignedStaffId();
            System.out.println(String.format("%-6d | %-8s | %-18s | %-24s | %-12s",
                    i, r.getRoomId(), r.getRoomType(), r.getCurrentStatus(), staff));

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

        System.out.println(String.format("%-10s | %-8s | %-12s | %-24s | %-10s", "Task ID", "Room ID", "Staff ID", "Current Status", "Rollbacks"));
        System.out.println("--------------------------------------------------------------------------");

        for (int i = 1; i <= auditTasks.getNumberOfEntries(); i++) {
            HousekeepingTask t = auditTasks.getEntry(i);
            String staff = (t.getStaffId() == null || t.getStaffId().trim().isEmpty()) ? "UNASSIGNED" : t.getStaffId();
            System.out.println(String.format("%-10s | %-8s | %-12s | %-24s | %-10d",
                    t.getTaskId(), t.getRoomId(), staff, t.getCurrentStatus(), t.getRollbackCount()));
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

    /**
     * Rule 1 Helper: Finds the single active (non-Archived) housekeeping task for a room.
     */
    private HousekeepingTask findActiveTaskByRoomId(String roomId) {
        for (int i = taskList.getNumberOfEntries(); i >= 1; i--) {
            HousekeepingTask t = taskList.getEntry(i);
            if (t.getRoomId().equalsIgnoreCase(roomId) && !"Archived".equalsIgnoreCase(t.getCurrentStatus())) {
                return t;
            }
        }
        return null;
    }
}
