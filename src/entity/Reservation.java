package entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a Reservation in the Front-Desk subsystem.
 * Stores guest, room, billing, and status information.
 *
 * @author Mun Jun How
 */
public class Reservation implements Serializable, Comparable<Reservation> {

    private String confirmationNumber; // 8-digit unique ID (Primary Key for HashMap)
    private Guest guest;
    private Room room;
    private String checkInDate;
    private String checkOutDate;
    private double roomRate;           // Rate per night
    private double incidentalCharges;  // Extra charges: room service, spa, etc.
    private boolean isPaid;
    private String status; // "Confirmed", "Checked-In", "Checked-Out"

    public Reservation(String confirmationNumber, Guest guest, Room room,
                       String checkInDate, String checkOutDate,
                       double roomRate, String status) {
        this.confirmationNumber = confirmationNumber;
        this.guest = guest;
        this.room = room;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.roomRate = roomRate;
        this.incidentalCharges = 0.0;
        this.isPaid = false;
        this.status = status;
    }

    // --- Getters & Setters ---
    public String getConfirmationNumber() { return confirmationNumber; }
    public void setConfirmationNumber(String confirmationNumber) { this.confirmationNumber = confirmationNumber; }

    public Guest getGuest() { return guest; }
    public void setGuest(Guest guest) { this.guest = guest; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public String getCheckInDate() { return checkInDate; }
    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }

    public String getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(String checkOutDate) { this.checkOutDate = checkOutDate; }

    public double getRoomRate() { return roomRate; }
    public void setRoomRate(double roomRate) { this.roomRate = roomRate; }

    public double getIncidentalCharges() { return incidentalCharges; }
    public void addIncidentalCharge(double amount) {
        if (amount > 0) this.incidentalCharges += amount;
    }

    /**
     * Calculates number of nights between check-in and check-out dates.
     * Falls back to 1 night if dates are unparseable.
     */
    public long getNumberOfNights() {
        try {
            java.time.LocalDate in  = java.time.LocalDate.parse(checkInDate);
            java.time.LocalDate out = java.time.LocalDate.parse(checkOutDate);
            long nights = java.time.temporal.ChronoUnit.DAYS.between(in, out);
            return nights > 0 ? nights : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    /** Total = (roomRate × nights) + incidentalCharges */
    public double getTotalAmount() {
        return (roomRate * getNumberOfNights()) + incidentalCharges;
    }

    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { this.isPaid = paid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /** Returns a detailed folio-style summary for billing inquiries. */
    public String toFolioString() {
        long nights = getNumberOfNights();
        return String.format(
            "  Confirmation No : %s\n" +
            "  Guest Name      : %s\n" +
            "  IC Number       : %s\n" +
            "  Contact         : %s\n" +
            "  Room            : %s (%s)\n" +
            "  Check-In Date   : %s\n" +
            "  Check-Out Date  : %s\n" +
            "  Nights          : %d night(s)\n" +
            "  Room Rate       : RM%.2f / night\n" +
            "  Room Subtotal   : RM%.2f\n" +
            "  Incidentals     : RM%.2f\n" +
            "  ---------------------------------\n" +
            "  TOTAL AMOUNT    : RM%.2f\n" +
            "  Payment Status  : %s\n" +
            "  Reservation Status: %s",
            confirmationNumber,
            guest.getName(), guest.getIcNumber(), guest.getContactNumber(),
            room.getRoomId(), room.getRoomType(),
            checkInDate, checkOutDate,
            nights,
            roomRate, roomRate * nights,
            incidentalCharges,
            getTotalAmount(),
            isPaid ? "PAID" : "OUTSTANDING",
            status
        );
    }


    @Override
    public String toString() {
        return String.format("%-10s | %-20s | Room: %-5s | %-12s | Check-In: %-12s | Total: RM%-8.2f | %-10s | %s",
                confirmationNumber, guest.getName(), room.getRoomId(),
                room.getRoomType(), checkInDate, getTotalAmount(),
                isPaid ? "PAID" : "OUTSTANDING", status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return Objects.equals(confirmationNumber, that.confirmationNumber);
    }

    @Override
    public int compareTo(Reservation o) {
        return this.confirmationNumber.compareToIgnoreCase(o.confirmationNumber);
    }
}
