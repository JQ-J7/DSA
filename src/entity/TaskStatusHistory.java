package entity;

import java.io.Serializable;

/**
 * Entity class representing an individual status change record or rollback event.
 * 
 * @author Tan Jun Qi
 */
public class TaskStatusHistory implements Serializable {

    private String logId;
    private String previousStatus;
    private String newStatus;
    private String updatedBy;
    private String timestamp;
    private String changeReason;
    private boolean isRollback;

    public TaskStatusHistory(String logId, String previousStatus, String newStatus, String updatedBy, String timestamp, String changeReason, boolean isRollback) {
        this.logId = logId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.updatedBy = updatedBy;
        this.timestamp = timestamp;
        this.changeReason = changeReason;
        this.isRollback = isRollback;
    }

    public String getLogId() {
        return logId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public boolean isRollback() {
        return isRollback;
    }

    @Override
    public String toString() {
        return String.format("[%s] Log %s by %s: '%s' -> '%s' (Reason: %s) [Rollback: %s]",
                timestamp, logId, updatedBy, previousStatus, newStatus, changeReason, (isRollback ? "YES" : "NO"));
    }
}
