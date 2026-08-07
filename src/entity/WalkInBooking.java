package entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entity representing a Walk-In Registration or Standard Booking entry.
 * Managed chronologically in a Linear Queue ADT.
 * 
 * @author Walk-In Subsystem Lead
 */
public class WalkInBooking implements Serializable, Comparable<WalkInBooking> {

    private String bookingId;           // Unique ID (e.g., WB1001)
    private Guest guest;                // Associated guest entity
    private String requestedRoomType;   // Standard, Deluxe, Suite
    private int numberOfNights;
    private double estimatedRatePerNight;
    private String registrationTime;    // Formatted timestamp (yyyy-MM-dd HH:mm:ss)
    private String bookingType;         // "Walk-In" or "Standard Advance"
    private Room assignedRoom;          // Null if waiting, populated upon room allocation
    private String status;              // WAITING, ALLOCATED, CANCELLED, EXPIRED

    public WalkInBooking(String bookingId, Guest guest, String requestedRoomType,
                         int numberOfNights, double estimatedRatePerNight,
                         String registrationTime, String bookingType, String status) {
        this.bookingId = bookingId;
        this.guest = guest;
        this.requestedRoomType = requestedRoomType;
        this.numberOfNights = numberOfNights;
        this.estimatedRatePerNight = estimatedRatePerNight;
        this.registrationTime = registrationTime;
        this.bookingType = bookingType;
        this.assignedRoom = null;
        this.status = status;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }

    public String getRequestedRoomType() {
        return requestedRoomType;
    }

    public void setRequestedRoomType(String requestedRoomType) {
        this.requestedRoomType = requestedRoomType;
    }

    public int getNumberOfNights() {
        return numberOfNights;
    }

    public void setNumberOfNights(int numberOfNights) {
        this.numberOfNights = numberOfNights;
    }

    public double getEstimatedRatePerNight() {
        return estimatedRatePerNight;
    }

    public void setEstimatedRatePerNight(double estimatedRatePerNight) {
        this.estimatedRatePerNight = estimatedRatePerNight;
    }

    public String getRegistrationTime() {
        return registrationTime;
    }

    public void setRegistrationTime(String registrationTime) {
        this.registrationTime = registrationTime;
    }

    public String getBookingType() {
        return bookingType;
    }

    public void setBookingType(String bookingType) {
        this.bookingType = bookingType;
    }

    public Room getAssignedRoom() {
        return assignedRoom;
    }

    public void setAssignedRoom(Room assignedRoom) {
        this.assignedRoom = assignedRoom;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotalEstimatedCost() {
        return numberOfNights * estimatedRatePerNight;
    }

    public String toDetailString() {
        return String.format(
            "  Booking ID       : %s\n" +
            "  Guest Name       : %s\n" +
            "  IC Number        : %s\n" +
            "  Contact          : %s\n" +
            "  Channel Type     : %s\n" +
            "  Requested Type   : %s\n" +
            "  Nights           : %d night(s) @ RM%.2f/night\n" +
            "  Est. Total Cost  : RM%.2f\n" +
            "  Registration Time: %s\n" +
            "  Status           : %s\n" +
            "  Assigned Room    : %s",
            bookingId, guest.getName(), guest.getIcNumber(), guest.getContactNumber(),
            bookingType, requestedRoomType, numberOfNights, estimatedRatePerNight,
            getTotalEstimatedCost(), registrationTime, status,
            (assignedRoom != null ? assignedRoom.getRoomId() + " (" + assignedRoom.getRoomType() + ")" : "Unassigned")
        );
    }

    @Override
    public String toString() {
        return String.format("%-10s | %-18s | %-12s | %-12s | %-2d nights | RM%-8.2f | %-10s | Room: %-6s",
                bookingId, guest.getName(), bookingType, requestedRoomType,
                numberOfNights, getTotalEstimatedCost(), status,
                (assignedRoom != null ? assignedRoom.getRoomId() : "N/A"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WalkInBooking that = (WalkInBooking) o;
        return Objects.equals(bookingId, that.bookingId);
    }

    @Override
    public int compareTo(WalkInBooking o) {
        return this.bookingId.compareToIgnoreCase(o.bookingId);
    }
}
