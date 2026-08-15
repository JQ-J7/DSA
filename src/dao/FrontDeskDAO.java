package dao;

import adt.HashMap;
import adt.MapInterface;
import entity.Reservation;
import java.io.*;

/**
 * Data Access Object for Front-Desk Reservations.
 * Handles reading from and writing to binary files using Object Streams.
 *
 * @author Mun Jun How
 */
public class FrontDeskDAO {

    private String fileName = "reservations.dat";
    private String historyFileName = "checkout_history.dat";

    public void saveReservationsToFile(MapInterface<String, Reservation> reservationMap) {
        File file = new File(fileName);
        try (ObjectOutputStream ooStream = new ObjectOutputStream(new FileOutputStream(file))) {
            ooStream.writeObject(reservationMap);
        } catch (FileNotFoundException ex) {
            System.out.println("Reservations data file not found.");
        } catch (IOException ex) {
            System.out.println("Cannot save reservations to file: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public MapInterface<String, Reservation> retrieveReservationsFromFile() {
        File file = new File(fileName);
        MapInterface<String, Reservation> reservationMap = new HashMap<>();
        if (!file.exists()) {
            return reservationMap;
        }
        try (ObjectInputStream oiStream = new ObjectInputStream(new FileInputStream(file))) {
            reservationMap = (HashMap<String, Reservation>) (oiStream.readObject());
        } catch (FileNotFoundException ex) {
            System.out.println("No reservation data file found.");
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Cannot read reservations from file.");
        }
        return reservationMap;
    }

    public void saveHistoryToFile(adt.ListInterface<Reservation> historyList) {
        File file = new File(historyFileName);
        try (ObjectOutputStream ooStream = new ObjectOutputStream(new FileOutputStream(file))) {
            ooStream.writeObject(historyList);
        } catch (FileNotFoundException ex) {
            System.out.println("Check-out history data file not found.");
        } catch (IOException ex) {
            System.out.println("Cannot save check-out history to file: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public adt.ListInterface<Reservation> retrieveHistoryFromFile() {
        File file = new File(historyFileName);
        adt.ListInterface<Reservation> historyList = new adt.ArrayList<>();
        if (!file.exists()) {
            return historyList;
        }
        try (ObjectInputStream oiStream = new ObjectInputStream(new FileInputStream(file))) {
            historyList = (adt.ArrayList<Reservation>) (oiStream.readObject());
        } catch (FileNotFoundException ex) {
            System.out.println("No check-out history data file found.");
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Cannot read check-out history from file.");
        }
        return historyList;
    }
}
