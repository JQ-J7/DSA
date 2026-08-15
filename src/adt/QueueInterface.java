package adt;

/**
 * Interface for a Queue Abstract Data Type (ADT).
 * Provides operations to enqueue, dequeue, and inspect elements in FIFO order.
 * 
 * @author Chan Shao Lun
 * @param <T> Data type of elements stored in the queue
 */
public interface QueueInterface<T> {

    /**
     * Adds a new entry to the back of this queue.
     * 
     * @param newEntry An object to be added.
     * @return True if addition is successful, false otherwise.
     */
    boolean enqueue(T newEntry);

    /**
     * Removes and returns the entry at the front of this queue.
     * 
     * @return The object at the front of the queue, or null if empty.
     */
    T dequeue();

    /**
     * Retrieves the entry at the front of this queue without removing it.
     * 
     * @return The object at the front of the queue, or null if empty.
     */
    T getFront();

    /**
     * Checks whether this queue is empty.
     * 
     * @return True if the queue is empty, false otherwise.
     */
    boolean isEmpty();

    /**
     * Removes all entries from this queue.
     */
    void clear();

    /**
     * Gets the number of entries currently in this queue.
     * 
     * @return The integer count of entries in the queue.
     */
    int size();
}
