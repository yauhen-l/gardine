package by.yauhenl.gardine;

import android.content.Intent;

/**
 * Created by yauhen on 8/30/19.
 */
public class App {

    final String title;
    final String packageName;
    final Intent startIntent;

    App(String title, String packageName, Intent startIntent) {
        this.title = title;
        this.packageName = packageName;
        this.startIntent = startIntent;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        String pn = "";
        if(obj instanceof String) {
            pn = (String) obj;
        } else if (obj instanceof App) {
            pn = ((App) obj).packageName;
        }
        return this.packageName.equals(pn);
    }

    @Override
    public int hashCode() {
        return this.packageName.hashCode();
    }

    // WARN: it is used in list item view
    @Override
    public String toString() {
        return this.title;
    }

    public String toLogString() {
        return "App{" +
                "title='" + title + '\'' +
                ", packageName='" + packageName + '\'' +
                ", startIntent=" + startIntent +
                '}';
    }
}
