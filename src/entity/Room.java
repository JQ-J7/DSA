package entity;

import java.io.Serializable;

/**
 * Entity class representing a Hotel Room.
 * 
 * @author Tan Jun Qi
 */
public class Room implements Serializable {

    private String roomId;
    private String roomType;
    private int floorNumber;
    private String currentStatus;
    private String assignedStaffId;

    public Room(String roomId, String roomType, int floorNumber, String currentStatus, String assignedStaffId) {
        this.roomId = roomId;
        this.roomType = roomType;
        this.floorNumber = floorNumber;
        this.currentStatus = currentStatus;
        this.assignedStaffId = assignedStaffId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getAssignedStaffId() {
        return assignedStaffId;
    }

    public void setAssignedStaffId(String assignedStaffId) {
        this.assignedStaffId = assignedStaffId;
    }

    @Override
    public String toString() {
        return String.format("Room ID: %-8s | Type: %-15s | Floor: %-2d | Status: %-22s | Staff: %-8s",
                roomId, roomType, floorNumber, currentStatus, (assignedStaffId.isEmpty() ? "Unassigned" : assignedStaffId));
    }
}
