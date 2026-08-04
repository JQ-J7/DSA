package adt;

import java.io.Serializable;

/**
 * Custom Linear ADT - Linked-based Stack implementation.
 * Provides LIFO operations required for instant task log rollbacks.
 * 
 * @author Tan Jun Qi
 * @param <T> Data type of elements stored in the stack
 */
public class LinkedStack<T> implements StackInterface<T>, Serializable {

    private Node topNode;
    private int numberOfEntries;

    public LinkedStack() {
        topNode = null;
        numberOfEntries = 0;
    }

    @Override
    public void push(T newEntry) {
        Node newNode = new Node(newEntry, topNode);
        topNode = newNode;
        numberOfEntries++;
    }

    @Override
    public T pop() {
        T topData = peek();
        if (topNode != null) {
            topNode = topNode.next;
            numberOfEntries--;
        }
        return topData;
    }

    @Override
    public T peek() {
        if (isEmpty()) {
            return null;
        } else {
            return topNode.data;
        }
    }

    @Override
    public boolean isEmpty() {
        return topNode == null;
    }

    @Override
    public void clear() {
        topNode = null;
        numberOfEntries = 0;
    }

    @Override
    public int getNumberOfEntries() {
        return numberOfEntries;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Node current = topNode;
        while (current != null) {
            sb.append(current.data.toString()).append("\n");
            current = current.next;
        }
        return sb.toString();
    }

    private class Node implements Serializable {
        private T data;
        private Node next;

        private Node(T data) {
            this(data, null);
        }

        private Node(T data, Node next) {
            this.data = data;
            this.next = next;
        }
    }
}
