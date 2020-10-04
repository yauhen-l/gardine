package by.yauhenl.gardine;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * Stack (FILO) of unique elements.
 * All elements over limit are dropped.
 * Adding the same element several times moves it on top of the stack.
 * @param <T>
 */
public class DiscardingStack<T> {

    private final int limit;
    private LinkedHashSet<T> elements;

    public DiscardingStack(int limit) {
        this.limit = limit;
        this.elements = new LinkedHashSet<>();
    }

    public boolean remove(T e) {
        return this.elements.remove(e);
    }

    public void add(T e) {
        if(elements.contains(e)) {
            elements.remove(e);
        } else {
            truncateSize(this.limit - 1);
        }
        elements.add(e);
    }

    public ArrayDeque<T> getAll() {
        ArrayDeque<T> reversed =  new ArrayDeque<>();
        for (T a : this.elements) {
            reversed.push(a);
        }
        return reversed;
    }

    private void truncateSize(int limit) {
        if(elements.size() < limit) {
            return;
        }
        Iterator<T> iter = elements.iterator();
        for (int i = elements.size(); iter.hasNext(); i--) {
            iter.next();
            if(i > limit) {
                iter.remove();
            }
        }
    }
}
