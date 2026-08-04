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
}
