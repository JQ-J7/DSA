package adt;

import java.io.Serializable;

/**
 * Node-based implementation of the QueueInterface (Linear ADT).
 * Maintains First-In, First-Out (FIFO) ordering for walk-in registrations.
 * 
 * @author Chan Shao Lun
 * @param <T> Data type of elements stored in the queue
 */
public class LinkedQueue<T> implements QueueInterface<T>, Serializable {

    private Node firstNode;
    private Node lastNode;
    private int count;

    public LinkedQueue() {
        firstNode = null;
        lastNode = null;
        count = 0;
    }

    @Override
    public boolean enqueue(T newEntry) {
        Node newNode = new Node(newEntry, null);
        if (isEmpty()) {
            firstNode = newNode;
        } else {
            lastNode.next = newNode;
        }
        lastNode = newNode;
        count++;
        return true;
    }

    @Override
    public T dequeue() {
        T front = getFront();
        if (front != null) {
            firstNode = firstNode.next;
            if (firstNode == null) {
                lastNode = null;
            }
            count--;
        }
        return front;
    }

    @Override
    public T getFront() {
        if (isEmpty()) {
            return null;
        } else {
            return firstNode.data;
        }
    }

    @Override
    public boolean isEmpty() {
        return (firstNode == null) && (lastNode == null);
    }

    @Override
    public void clear() {
        firstNode = null;
        lastNode = null;
        count = 0;
    }

    @Override
    public int size() {
        return count;
    }

    /**
     * Converts queue contents to a ListInterface for iteration/reporting without
     * mutating the queue.
     * 
     * @return ListInterface containing all elements in FIFO order.
     */
    public ListInterface<T> toList() {
        ListInterface<T> list = new ArrayList<>(count > 0 ? count : 10);
        Node currentNode = firstNode;
        while (currentNode != null) {
            list.add(currentNode.data);
            currentNode = currentNode.next;
        }
        return list;
    }

    private class Node implements Serializable {
        private T data;
        private Node next;

        private Node(T dataPortion) {
            this(dataPortion, null);
        }

        private Node(T dataPortion, Node linkPortion) {
            data = dataPortion;
            next = linkPortion;
        }
    }
}
