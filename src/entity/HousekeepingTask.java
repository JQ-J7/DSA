package entity;

import adt.LinkedStack;
import adt.StackInterface;
import java.io.Serializable;

/**
 * Entity class representing a Housekeeping Task for a hotel room.
 * Utilizes a custom Stack ADT to maintain LIFO status history for instant rollbacks.
 * 
 * @author Tan Jun Qi
 */
public class HousekeepingTask implements Serializable {

    private String taskId;
    private String roomId;
    private String staffId;
    private String currentStatus;
    private String lastUpdated;
    private StackInterface<TaskStatusHistory> historyStack;
    private int rollbackCount;

    public HousekeepingTask(String taskId, String roomId, String staffId, String initialStatus, String timestamp) {
        this.taskId = taskId;
        this.roomId = roomId;
        this.staffId = staffId;
        this.currentStatus = initialStatus;
        this.lastUpdated = timestamp;
        this.historyStack = new LinkedStack<>();
        this.rollbackCount = 0;

        // Push initial log
        TaskStatusHistory initialLog = new TaskStatusHistory(
                "LOG-" + System.currentTimeMillis() % 10000,
                "N/A",
                initialStatus,
                staffId,
                timestamp,
                "Initial task created",
                false
        );
        historyStack.push(initialLog);
    }

    public String getTaskId() {
        return taskId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public StackInterface<TaskStatusHistory> getHistoryStack() {
        return historyStack;
    }

    public int getRollbackCount() {
        return rollbackCount;
    }

    /**
     * Updates the task status sequentially and logs the status change onto the stack.
     */
    public boolean updateStatus(String newStatus, String updatedBy, String timestamp, String reason) {
        TaskStatusHistory newLog = new TaskStatusHistory(
                "LOG-" + System.currentTimeMillis() % 10000,
                this.currentStatus,
                newStatus,
                updatedBy,
                timestamp,
                reason,
                false
        );
        historyStack.push(newLog);
        this.currentStatus = newStatus;
        this.lastUpdated = timestamp;
        return true;
    }

    /**
     * Instantly rolls back the current task status to the previous state using Stack pop.
     */
    public TaskStatusHistory rollbackStatus(String updatedBy, String timestamp, String reason) {
        if (historyStack.isEmpty()) {
            return null;
        }

        // Pop current status log
        TaskStatusHistory poppedLog = historyStack.pop();

        if (historyStack.isEmpty()) {
            // Push it back if it was the only record
            historyStack.push(poppedLog);
            return null;
        }

        // Previous log is now at top of stack
        TaskStatusHistory previousLog = historyStack.peek();
        this.currentStatus = previousLog.getNewStatus();
        this.lastUpdated = timestamp;
        this.rollbackCount++;

        // Return the popped state for reporting/auditing
        return poppedLog;
    }

    @Override
    public String toString() {
        return String.format("Task ID: %-8s | Room: %-6s | Staff: %-8s | Status: %-22s | Last Updated: %s",
                taskId, roomId, staffId, currentStatus, lastUpdated);
    }
}
