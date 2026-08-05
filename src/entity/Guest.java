package entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a Guest.
 *
 * @author Mun Jun How
 */
public class Guest implements Serializable, Comparable<Guest> {
    private String icNumber;
    private String name;
    private String contactNumber;

    public Guest(String icNumber, String name, String contactNumber) {
        this.icNumber = icNumber;
        this.name = name;
        this.contactNumber = contactNumber;
    }

    public String getIcNumber() { return icNumber; }
    public void setIcNumber(String icNumber) { this.icNumber = icNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    @Override
    public String toString() {
        return String.format("Guest[IC=%s, Name=%s, Contact=%s]", icNumber, name, contactNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Guest guest = (Guest) o;
        return Objects.equals(icNumber, guest.icNumber);
    }

    @Override
    public int compareTo(Guest o) {
        return this.name.compareToIgnoreCase(o.name);
    }
}
