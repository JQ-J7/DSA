package adt;

/**
 * Interface for a Stack Abstract Data Type (ADT).
 * Used for sequential task log management and instant status rollback operations.
 * 
 * @author Tan Jun Qi
 * @param <T> Data type of elements stored in the stack
 */
public interface StackInterface<T> {

    /**
     * Adds a new entry to the top of this stack.
     * @param newEntry An object to be added to the stack.
     */
    void push(T newEntry);

    /**
     * Removes and returns this stack's top entry.
     * @return The object at the top of the stack or null if stack is empty.
     */
    T pop();

    /**
     * Retrieves this stack's top entry without removing it.
     * @return The object at the top of the stack or null if stack is empty.
     */
    T peek();

    /**
     * Detects whether this stack is empty.
     * @return True if the stack is empty, false otherwise.
     */
    boolean isEmpty();

    /**
     * Removes all entries from this stack.
     */
    void clear();

    /**
     * Gets the number of entries currently in this stack.
     * @return The integer number of entries.
     */
    int getNumberOfEntries();
}
