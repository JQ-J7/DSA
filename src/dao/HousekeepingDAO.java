package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.HousekeepingTask;
import entity.Room;
import java.io.*;

/**
 * Data Access Object for Housekeeping Tasks and Rooms.
 * Handles reading from and writing to binary files using Object Streams.
 * 
 * @author Tan Jun Qi
 */
public class HousekeepingDAO {

    private String tasksFileName = "housekeeping_tasks.dat";
    private String roomsFileName = "rooms.dat";

    public void saveTasksToFile(ListInterface<HousekeepingTask> taskList) {
        File file = new File(tasksFileName);
        try (ObjectOutputStream ooStream = new ObjectOutputStream(new FileOutputStream(file))) {
            ooStream.writeObject(taskList);
        } catch (FileNotFoundException ex) {
            System.out.println("Tasks file not found.");
        } catch (IOException ex) {
            System.out.println("Cannot save tasks to file: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public ListInterface<HousekeepingTask> retrieveTasksFromFile() {
        File file = new File(tasksFileName);
        ListInterface<HousekeepingTask> taskList = new ArrayList<>();
        if (!file.exists()) {
            return taskList;
        }
        try (ObjectInputStream oiStream = new ObjectInputStream(new FileInputStream(file))) {
            taskList = (ArrayList<HousekeepingTask>) (oiStream.readObject());
        } catch (FileNotFoundException ex) {
            System.out.println("No task data file found.");
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Cannot read tasks from file.");
        }
        return taskList;
    }

    public void saveRoomsToFile(ListInterface<Room> roomList) {
        File file = new File(roomsFileName);
        try (ObjectOutputStream ooStream = new ObjectOutputStream(new FileOutputStream(file))) {
            ooStream.writeObject(roomList);
        } catch (FileNotFoundException ex) {
            System.out.println("Rooms file not found.");
        } catch (IOException ex) {
            System.out.println("Cannot save rooms to file: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public ListInterface<Room> retrieveRoomsFromFile() {
        File file = new File(roomsFileName);
        ListInterface<Room> roomList = new ArrayList<>();
        if (!file.exists()) {
            return roomList;
        }
        try (ObjectInputStream oiStream = new ObjectInputStream(new FileInputStream(file))) {
            roomList = (ArrayList<Room>) (oiStream.readObject());
        } catch (FileNotFoundException ex) {
            System.out.println("No room data file found.");
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Cannot read rooms from file.");
        }
        return roomList;
    }

    /**
     * Wipes persistent .dat storage files and repopulates fresh, rule-compliant seed data.
     * Enforces CLO2/CLO3 Data Consistency:
     *  - Rule 1: Single active task per room
     *  - Rule 2: Tripartite State Sync (Room.currentStatus == Task.currentStatus == Stack.peek().getNewStatus())
     *  - Rule 3: Valid LinkedStack LIFO history for each task
     * 
     * @return Formatted confirmation report of the reset and seed operation.
     */
    public String resetAndSeedData() {
        // 1. File Wipe: Delete existing binary data files
        File tasksFile = new File(tasksFileName);
        File roomsFile = new File(roomsFileName);

        if (tasksFile.exists()) {
            tasksFile.delete();
        }
        if (roomsFile.exists()) {
            roomsFile.delete();
        }

        // 2. Generate Clean 15 Constant Rooms (R101-R305) with balanced statuses
        ListInterface<Room> cleanRooms = new ArrayList<>();
        cleanRooms.add(new Room("R101", "Suite Room", 1, "Occupied", "ST101"));
        cleanRooms.add(new Room("R102", "Deluxe Room", 1, "Cleaning In Progress", "ST101"));
        cleanRooms.add(new Room("R103", "Deluxe Room", 1, "Inspected", "ST101"));
        cleanRooms.add(new Room("R104", "Standard Room", 1, "Ready for Check-In", "ST102"));
        cleanRooms.add(new Room("R105", "Standard Room", 1, "Dirty", "UNASSIGNED"));

        cleanRooms.add(new Room("R201", "Suite Room", 2, "Occupied", "ST103"));
        cleanRooms.add(new Room("R202", "Deluxe Room", 2, "Cleaning In Progress", "ST103"));
        cleanRooms.add(new Room("R203", "Deluxe Room", 2, "Dirty", "ST103"));
        cleanRooms.add(new Room("R204", "Standard Room", 2, "Inspected", "ST103"));
        cleanRooms.add(new Room("R205", "Standard Room", 2, "Ready for Check-In", "ST103"));

        cleanRooms.add(new Room("R301", "Suite Room", 3, "Dirty", "ST104"));
        cleanRooms.add(new Room("R302", "Deluxe Room", 3, "Cleaning In Progress", "ST104"));
        cleanRooms.add(new Room("R303", "Deluxe Room", 3, "Ready for Check-In", "ST104"));
        cleanRooms.add(new Room("R304", "Standard Room", 3, "Dirty", "UNASSIGNED"));
        cleanRooms.add(new Room("R305", "Standard Room", 3, "Ready for Check-In", "ST104"));

        // 3. Generate Valid Housekeeping Tasks matching Rules 1, 2, and 3
        ListInterface<HousekeepingTask> cleanTasks = new ArrayList<>();
        java.time.LocalDateTime baseTime = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        int taskCounter = 1001;
        for (int i = 1; i <= cleanRooms.getNumberOfEntries(); i++) {
            Room r = cleanRooms.getEntry(i);
            String taskId = "TSK-" + taskCounter++;
            String createTime = baseTime.minusMinutes(90).format(dtf);
            String cleanTime = baseTime.minusMinutes(45).format(dtf);
            String inspectTime = baseTime.minusMinutes(15).format(dtf);
            String readyTime = baseTime.format(dtf);

            String initialStatus = "Dirty";
            HousekeepingTask task = new HousekeepingTask(taskId, r.getRoomId(), r.getAssignedStaffId(), initialStatus, createTime);

            // Apply realistic progressive status history on custom LinkedStack ADT
            if ("Cleaning In Progress".equalsIgnoreCase(r.getCurrentStatus())) {
                task.updateStatus("Cleaning In Progress", r.getAssignedStaffId(), cleanTime, "Staff started cleaning");
            } else if ("Inspected".equalsIgnoreCase(r.getCurrentStatus())) {
                task.updateStatus("Cleaning In Progress", r.getAssignedStaffId(), cleanTime, "Staff started cleaning");
                task.updateStatus("Inspected", "SUP-01", inspectTime, "Supervisor inspection passed");
            } else if ("Ready for Check-In".equalsIgnoreCase(r.getCurrentStatus())) {
                task.updateStatus("Cleaning In Progress", r.getAssignedStaffId(), cleanTime, "Staff started cleaning");
                task.updateStatus("Inspected", "SUP-01", inspectTime, "Supervisor inspection passed");
                task.updateStatus("Ready for Check-In", "SUP-01", readyTime, "Released for guest check-in");
            } else if ("Occupied".equalsIgnoreCase(r.getCurrentStatus())) {
                // Room is occupied by guest: push Occupied state log to stack
                task.setCurrentStatus("Occupied");
                task.setLastUpdated(readyTime);
                entity.TaskStatusHistory occLog = new entity.TaskStatusHistory(
                        "LOG-" + System.currentTimeMillis() % 10000,
                        "Ready for Check-In",
                        "Occupied",
                        r.getAssignedStaffId(),
                        readyTime,
                        "Guest checked in",
                        false
                );
                task.getHistoryStack().push(occLog);
            }
            cleanTasks.add(task);
        }

        // 4. Object Serialization to New Persistent Storage Files
        saveRoomsToFile(cleanRooms);
        saveTasksToFile(cleanTasks);

        return "SUCCESS: System storage files ('" + roomsFileName + "', '" + tasksFileName 
                + "') wiped and re-seeded with " + cleanRooms.getNumberOfEntries() 
                + " compliant Rooms and " + cleanTasks.getNumberOfEntries() + " Tasks.";
    }

    public static void main(String[] args) {
        HousekeepingDAO dao = new HousekeepingDAO();
        String result = dao.resetAndSeedData();
        System.out.println(result);
    }
}
