package com.example;

// interface to Collections
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.Deque;

// class of implementations data structure
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StructureTesting {
    private final static Logger logger = LoggerFactory.getLogger("StructureTesting");

    private static void testArrayList() {
        // init ArrayList
        ArrayList<String> abc = new ArrayList<>();
        String[] adder = { "1", "2", "3", "4", "5", "6", "7", "8", "9" };
        abc.addAll(Arrays.asList(adder));
        logger.info("ArrayList: {}", abc);
    }

    private static void testList() {
        List<String> mylist = new ArrayList<>();
        String[] adder = { "1", "2", "3", "4", "5", "6", "7", "8", "9" };
        Collections.addAll(mylist, adder);
        logger.info("List: {}", mylist);
    }

    private static void testListIterator() {
        ArrayList<String> mylist = new ArrayList<>();
        String[] adder = { "1", "2", "3", "4", "5", "6", "7", "8", "9" };
        Collections.addAll(mylist, adder);
        ListIterator<String> imylist = mylist.listIterator(0);
        while (imylist.hasNext()) {
            logger.info("ListIterator 'imylist' have: {}", imylist.next());
        }
    }

    private static void testQueue() {
        Queue<String> queue = new LinkedList<>();
        ArrayList<String> mylist = new ArrayList<>();
        String[] adder = { "1", "2", "3", "4", "5", "6", "7", "8", "9" };
        Collections.addAll(mylist, adder);
        queue.addAll(mylist);
        while (queue.peek() != null) {
            logger.info("Queue have: {}", queue.poll());
        }

    }

    private static void testDeque() {
        Deque<String> deque = new LinkedList<>();
        ArrayList<String> mylist = new ArrayList<>();
        String[] adder = { "1", "2", "3", "4", "5", "6", "7", "8", "9" };
        Collections.addAll(mylist, adder);
        deque.addAll(mylist);
        while (deque.peekLast() != null) {
            logger.info("Queue have: {}", deque.pollLast());
        }
    }

    public static void main(String[] args) {
        // Create ActorSystem and top level supervisor
        logger.info("ArrayList functional test");
        testArrayList();
        logger.info("List functional test");
        testList();
        logger.info("ListIterator functional test");
        testListIterator();
        logger.info("Queue functional test");
        testQueue();
        logger.info("Deque functional test");
        testDeque();
        logger.info("HashMap functional test");
        logger.info("HashSet functional test");
        logger.info("Map functional test");
    }
}
