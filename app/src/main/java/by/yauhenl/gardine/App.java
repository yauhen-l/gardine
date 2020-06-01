package by.yauhenl.gardine;

import android.content.Intent;

/**
 * Created by yauhen on 8/30/19.
 */
public class App {

    public final String title;
    public final String packageName;
    public final Intent startIntent;

    public App(String title, String packageName, Intent startIntent) {
        this.title = title;
        this.packageName = packageName;
        this.startIntent = startIntent;
    }

    public String toString() {
        return this.title;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(!(obj instanceof App)) {
            return false;
        }
        return this.packageName.equals(((App) obj).packageName);
    }

    @Override
    public int hashCode() {
        return this.packageName.hashCode();
    }
}
