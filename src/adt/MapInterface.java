package adt;

/**
 * MapInterface for a Non-Linear ADT.
 *
 * @author Mun Jun How
 * @param <K> Key type
 * @param <V> Value type
 */
public interface MapInterface<K, V> {
    /**
     * Associates the specified value with the specified key in this map.
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with key, or null if there was no mapping for key.
     */
    V put(K key, V value);

    /**
     * Returns the value to which the specified key is mapped,
     * or null if this map contains no mapping for the key.
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or null
     */
    V get(K key);

    /**
     * Removes the mapping for a key from this map if it is present.
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with key, or null
     */
    V remove(K key);

    /**
     * Returns true if this map contains a mapping for the specified key.
     * @param key key whose presence in this map is to be tested
     * @return true if this map contains a mapping for the specified key
     */
    boolean containsKey(K key);

    /**
     * Returns true if this map contains no key-value mappings.
     * @return true if this map contains no key-value mappings
     */
    boolean isEmpty();

    /**
     * Returns the number of key-value mappings in this map.
     * @return the number of key-value mappings in this map
     */
    int size();

    /**
     * Removes all of the mappings from this map.
     */
    void clear();

    /**
     * Returns an array containing all of the values in this map.
     * @return an array of values
     */
    Object[] values();

    /**
     * Returns an array containing all of the keys in this map.
     * @return an array of keys
     */
    Object[] keys();
}
