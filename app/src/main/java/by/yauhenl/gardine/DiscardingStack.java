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
    private LinkedHashSet<T> apps;

    public DiscardingStack(int limit) {
        this.limit = limit;
        this.apps = new LinkedHashSet<>();
    }

    public void add(T app) {
        if(apps.contains(app)) {
            apps.remove(app);
        } else {
            truncateSize(this.limit - 1);
        }
        apps.add(app);
    }

    public ArrayDeque<T> getAll() {
        ArrayDeque<T> reversed =  new ArrayDeque<>();
        for (T a : this.apps) {
            reversed.push(a);
        }
        return reversed;
    }

    private void truncateSize(int limit) {
        if(apps.size() < limit) {
            return;
        }
        Iterator<T> iter = apps.iterator();
        for (int i = apps.size(); iter.hasNext(); i--) {
            iter.next();
            if(i > limit) {
                iter.remove();
            }
        }
    }
}
