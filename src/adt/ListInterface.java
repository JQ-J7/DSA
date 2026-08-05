package adt;

/**
 * Interface for a List Abstract Data Type (ADT).
 * Provides operations to store, manipulate, and access an ordered collection of elements.
 * 
 * @author Tan Jun Qi
 * @param <T> Data type of elements stored in the list
 */
public interface ListInterface<T> {

    /**
     * Adds a new entry to the end of this list.
     * 
     * @param newEntry The object to be added as a new entry.
     * @return True if addition is successful, false otherwise.
     */
    boolean add(T newEntry);

    /**
     * Adds a new entry at a specified position within this list.
     * Entries at or above the specified position are shifted to the next higher position.
     * 
     * @param newPosition An integer specifying the desired position (1-indexed).
     * @param newEntry The object to be added as a new entry.
     * @return True if addition is successful, false if position is invalid or list is full.
     */
    boolean add(int newPosition, T newEntry);

    /**
     * Removes the entry at a given position from this list.
     * Entries at higher positions are shifted down to lower positions.
     * 
     * @param givenPosition An integer indicating the position of the entry to remove (1-indexed).
     * @return The removed entry, or null if the position is invalid or list is empty.
     */
    T remove(int givenPosition);

    /**
     * Removes all entries from this list.
     */
    void clear();

    /**
     * Replaces the entry at a given position in this list with a new entry.
     * 
     * @param givenPosition An integer indicating the position of the entry to replace (1-indexed).
     * @param newEntry The object to replace the existing entry.
     * @return True if replacement occurs, false if position is invalid.
     */
    boolean replace(int givenPosition, T newEntry);

    /**
     * Retrieves the entry at a given position in this list.
     * 
     * @param givenPosition An integer indicating the position of the desired entry (1-indexed).
     * @return The entry at the specified position, or null if position is invalid.
     */
    T getEntry(int givenPosition);

    /**
     * Checks whether this list contains a given entry.
     * 
     * @param anEntry The object to search for in the list.
     * @return True if the list contains the entry, false otherwise.
     */
    boolean contains(T anEntry);

    /**
     * Gets the number of entries currently in this list.
     * 
     * @return The integer count of entries in the list.
     */
    int getNumberOfEntries();

    /**
     * Checks whether this list is empty.
     * 
     * @return True if the list contains no entries, false otherwise.
     */
    boolean isEmpty();

    /**
     * Checks whether this list is full.
     * 
     * @return True if the list cannot accept more entries, false otherwise.
     */
    boolean isFull();
}