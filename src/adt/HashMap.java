package adt;

import java.io.Serializable;

/**
 * HashMap implementation of MapInterface.
 * Uses chaining for collision resolution.
 *
 * @author Mun Jun How
 * @param <K> Key type
 * @param <V> Value type
 */
public class HashMap<K, V> implements MapInterface<K, V>, Serializable {
    
    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;
    
    private Node<K, V>[] table;
    private int size;
    
    @SuppressWarnings("unchecked")
    public HashMap() {
        table = (Node<K, V>[]) new Node[DEFAULT_CAPACITY];
        size = 0;
    }
    
    private static class Node<K, V> implements Serializable {
        K key;
        V value;
        Node<K, V> next;
        
        Node(K key, V value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }
    
    private int getIndex(K key) {
        if (key == null) return 0;
        return Math.abs(key.hashCode() % table.length);
    }
    
    @Override
    public V put(K key, V value) {
        int index = getIndex(key);
        Node<K, V> current = table[index];
        
        while (current != null) {
            if (current.key.equals(key)) {
                V oldValue = current.value;
                current.value = value;
                return oldValue;
            }
            current = current.next;
        }
        
        // Add new node at the beginning of the list
        Node<K, V> newNode = new Node<>(key, value, table[index]);
        table[index] = newNode;
        size++;
        
        if (size >= table.length * LOAD_FACTOR) {
            resize();
        }
        
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private void resize() {
        Node<K, V>[] oldTable = table;
        table = (Node<K, V>[]) new Node[oldTable.length * 2];
        size = 0; // reset size, will be incremented in put
        
        for (Node<K, V> node : oldTable) {
            while (node != null) {
                put(node.key, node.value);
                node = node.next;
            }
        }
    }
    
    @Override
    public V get(K key) {
        int index = getIndex(key);
        Node<K, V> current = table[index];
        
        while (current != null) {
            if (current.key.equals(key)) {
                return current.value;
            }
            current = current.next;
        }
        
        return null;
    }
    
    @Override
    public V remove(K key) {
        int index = getIndex(key);
        Node<K, V> current = table[index];
        Node<K, V> previous = null;
        
        while (current != null) {
            if (current.key.equals(key)) {
                if (previous == null) {
                    table[index] = current.next;
                } else {
                    previous.next = current.next;
                }
                size--;
                return current.value;
            }
            previous = current;
            current = current.next;
        }
        
        return null;
    }
    
    @Override
    public boolean containsKey(K key) {
        return get(key) != null;
    }
    
    @Override
    public boolean isEmpty() {
        return size == 0;
    }
    
    @Override
    public int size() {
        return size;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void clear() {
        table = (Node<K, V>[]) new Node[DEFAULT_CAPACITY];
        size = 0;
    }
    
    @Override
    public Object[] values() {
        if (size == 0) return new Object[0];
        
        Object[] vals = new Object[size];
        int index = 0;
        for (Node<K, V> node : table) {
            while (node != null) {
                vals[index++] = node.value;
                node = node.next;
            }
        }
        return vals;
    }
    
    @Override
    public Object[] keys() {
        if (size == 0) return new Object[0];
        
        Object[] ks = new Object[size];
        int index = 0;
        for (Node<K, V> node : table) {
            while (node != null) {
                ks[index++] = node.key;
                node = node.next;
            }
        }
        return ks;
    }
}
