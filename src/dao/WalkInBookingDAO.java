package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.WalkInBooking;
import java.io.*;

/**
 * Data Access Object for Walk-In Registrations and Standard Bookings.
 * Handles reading from and writing to binary files using Object Streams.
 * 
 * @author Walk-In Subsystem Lead
 */
public class WalkInBookingDAO {

    private String fileName = "walkin_bookings.dat";

    public void saveBookingsToFile(ListInterface<WalkInBooking> bookingList) {
        File file = new File(fileName);
        try (ObjectOutputStream ooStream = new ObjectOutputStream(new FileOutputStream(file))) {
            ooStream.writeObject(bookingList);
        } catch (FileNotFoundException ex) {
            System.out.println("Bookings data file not found.");
        } catch (IOException ex) {
            System.out.println("Cannot save bookings to file: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public ListInterface<WalkInBooking> retrieveBookingsFromFile() {
        File file = new File(fileName);
        ListInterface<WalkInBooking> bookingList = new ArrayList<>();
        if (!file.exists()) {
            return bookingList;
        }
        try (ObjectInputStream oiStream = new ObjectInputStream(new FileInputStream(file))) {
            bookingList = (ArrayList<WalkInBooking>) (oiStream.readObject());
        } catch (FileNotFoundException ex) {
            System.out.println("No booking data file found.");
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Cannot read bookings from file.");
        }
        return bookingList;
    }
}
