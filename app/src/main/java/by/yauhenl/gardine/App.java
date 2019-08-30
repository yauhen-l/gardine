package by.yauhenl.gardine;

import android.content.Intent;
import android.support.annotation.NonNull;

/**
 * Created by yauhen on 8/30/19.
 */
public class App implements Comparable<App> {

    public final String title;
    public final String packageName;
    public final long lastTimeUsed;
    public final Intent startIntent;

    public App(String title, String packageName, long lastTimeUsed, Intent startIntent) {
        this.title = title;
        this.packageName = packageName;
        this.lastTimeUsed = lastTimeUsed;
        this.startIntent = startIntent;
    }

    public String toString() {
        return this.title;
    }

    @Override
    public int compareTo(@NonNull App o) {
        return (int) (o.lastTimeUsed - this.lastTimeUsed);
    }
}
