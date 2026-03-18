package com.larleeloo.jormungandr.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A doubly-linked list that manages inventory slot ordering.
 * Each node wraps an InventorySlot and maintains prev/next references
 * so that items can be reordered via drag-and-drop by relinking nodes
 * rather than shifting array elements.
 */
public class InventoryLinkedList {

    public static class Node {
        private InventorySlot slot;
        private Node prev;
        private Node next;

        public Node(InventorySlot slot) {
            this.slot = slot;
        }

        public InventorySlot getSlot() { return slot; }
        public void setSlot(InventorySlot slot) { this.slot = slot; }
        public Node getPrev() { return prev; }
        public Node getNext() { return next; }
    }

    private Node head;
    private Node tail;
    private int size;

    public InventoryLinkedList() {
        head = null;
        tail = null;
        size = 0;
    }

    /**
     * Build the linked list from an existing ArrayList of InventorySlots.
     */
    public static InventoryLinkedList fromList(List<InventorySlot> slots) {
        InventoryLinkedList list = new InventoryLinkedList();
        if (slots == null) return list;
        for (InventorySlot slot : slots) {
            list.append(slot);
        }
        return list;
    }

    /**
     * Append a slot to the end of the linked list.
     */
    public void append(InventorySlot slot) {
        Node node = new Node(slot);
        if (head == null) {
            head = node;
            tail = node;
        } else {
            tail.next = node;
            node.prev = tail;
            tail = node;
        }
        size++;
    }

    /**
     * Get the node at a given index by traversal.
     */
    public Node getNodeAt(int index) {
        if (index < 0 || index >= size) return null;
        Node current;
        if (index <= size / 2) {
            current = head;
            for (int i = 0; i < index; i++) {
                current = current.next;
            }
        } else {
            current = tail;
            for (int i = size - 1; i > index; i--) {
                current = current.prev;
            }
        }
        return current;
    }

    /**
     * Get the InventorySlot at a given index.
     */
    public InventorySlot getSlotAt(int index) {
        Node node = getNodeAt(index);
        return node != null ? node.slot : null;
    }

    /**
     * Swap the slot data between two nodes at the given indices.
     * This swaps the InventorySlot contents (itemId, quantity) rather than
     * relinking nodes, which keeps the positional slot indices stable for
     * the backing ArrayList used by the RecyclerView adapter.
     */
    public void swapSlots(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) return;
        Node fromNode = getNodeAt(fromIndex);
        Node toNode = getNodeAt(toIndex);
        if (fromNode == null || toNode == null) return;

        // Swap the item data between slots
        String tempItemId = fromNode.slot.getItemId();
        int tempQuantity = fromNode.slot.getQuantity();

        fromNode.slot.setItemId(toNode.slot.getItemId());
        fromNode.slot.setQuantity(toNode.slot.getQuantity());

        toNode.slot.setItemId(tempItemId);
        toNode.slot.setQuantity(tempQuantity);
    }

    /**
     * Move an item from one slot to another. If the target is empty,
     * the item is placed there and the source is cleared.
     * If the target has the same item, stacks are merged up to maxStack.
     * If the target has a different item, the two are swapped.
     *
     * @return true if any change was made
     */
    public boolean moveItem(int fromIndex, int toIndex, int maxStackSize) {
        if (fromIndex == toIndex) return false;
        Node fromNode = getNodeAt(fromIndex);
        Node toNode = getNodeAt(toIndex);
        if (fromNode == null || toNode == null) return false;

        InventorySlot fromSlot = fromNode.slot;
        InventorySlot toSlot = toNode.slot;

        if (fromSlot.isEmpty()) return false;

        // Target is empty: move item there, clear source
        if (toSlot.isEmpty()) {
            toSlot.setItemId(fromSlot.getItemId());
            toSlot.setQuantity(fromSlot.getQuantity());
            fromSlot.setItemId(null);
            fromSlot.setQuantity(0);
            return true;
        }

        // Same item type: try to merge stacks
        if (fromSlot.getItemId() != null && fromSlot.getItemId().equals(toSlot.getItemId())) {
            int spaceInTarget = maxStackSize - toSlot.getQuantity();
            if (spaceInTarget > 0) {
                int toMove = Math.min(fromSlot.getQuantity(), spaceInTarget);
                toSlot.setQuantity(toSlot.getQuantity() + toMove);
                fromSlot.setQuantity(fromSlot.getQuantity() - toMove);
                if (fromSlot.getQuantity() <= 0) {
                    fromSlot.setItemId(null);
                    fromSlot.setQuantity(0);
                }
                return true;
            }
            // Both stacks are full, fall through to swap
        }

        // Different items (or same item with full stacks): swap
        swapSlots(fromIndex, toIndex);
        return true;
    }

    /**
     * Convert the linked list back to an ArrayList, preserving traversal order.
     * Each slot's positional index is updated to match its list position.
     */
    public List<InventorySlot> toList() {
        List<InventorySlot> result = new ArrayList<>(size);
        Node current = head;
        int index = 0;
        while (current != null) {
            current.slot.setSlot(index);
            result.add(current.slot);
            current = current.next;
            index++;
        }
        return result;
    }

    /**
     * Ensure the list has exactly the given capacity.
     * Adds empty slots if needed, or trims excess empty slots from the end.
     */
    public void ensureCapacity(int capacity) {
        while (size < capacity) {
            append(new InventorySlot(null, 0, size));
        }
    }

    public int getSize() { return size; }
    public Node getHead() { return head; }
    public Node getTail() { return tail; }
}
