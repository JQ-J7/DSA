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
}
