package by.yauhenl.gardine;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class RecentActivities {

    private final int limit;
    private LinkedHashSet<App> apps;

    public RecentActivities(int limit) {
        this.limit = limit;
        this.apps = new LinkedHashSet<>();
    }

    public void add(App app) {
        if(apps.contains(app)) {
            apps.remove(app);
        } else {
            truncateSize(this.limit - 1);
        }
        apps.add(app);
    }

    public Collection<App> getAll() {
        ArrayDeque<App> reversed =  new ArrayDeque<>();
        for (App a : this.apps) {
            reversed.push(a);
        }
        return reversed;
    }

    private void truncateSize(int limit) {
        if(apps.size() < limit) {
            return;
        }
        Iterator<App> iter = apps.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            iter.next();
            if(i >= limit) {
                iter.remove();
            }
        }
    }
}
